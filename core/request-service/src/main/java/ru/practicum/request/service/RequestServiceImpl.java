package ru.practicum.request.service;


import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.api.EventApi;
import ru.practicum.api.UserApi;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatusEntity;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.request.repository.RequestStatusRepository;
import ru.practicum.request.validation.RequestValidator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final RequestStatusRepository requestStatusRepository;
    private final UserApi userApi;
    private final EventApi eventApi;
    private final RequestValidator requestValidator;

    @Transactional(readOnly = true)
    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.debug("Запрос на получение всех заявок участия пользователя с ID: {}", userId);
        return requestRepository.findByRequesterId(userId).stream()
                .map(RequestMapper::toRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    public ParticipationRequestDto createParticipationRequest(Long userId, Long eventId) {
        final Long initiator = getUserById(userId);
        final EventFullDto eventFullDto = getEventById(eventId);
        log.info("Найдено событие: {}",
                eventFullDto);
        requestValidator.validateRequestCreation(initiator, eventFullDto);

        final Request request = buildNewRequest(initiator, eventFullDto);
        determineInitialStatus(eventFullDto, request);

        final Request savedRequest = requestRepository.save(request);
        updateEventStatistics(eventFullDto, request.getStatus().getName());

        log.info("Заявка на участие сохранена со статусом с ID: {} и статусом: {}",
                savedRequest.getId(), savedRequest.getStatus());
        return RequestMapper.toRequestDto(savedRequest);
    }

    @Override
    public ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId) {
        final Long initiator = getUserById(userId);
        final Request request = getRequestById(requestId);

        requestValidator.validateRequestOwnership(initiator, request);
        updateRequestStatus(request, RequestStatus.CANCELED);
        EventFullDto eventFullDto = eventApi.getEventFullDtoById(request.getEventId());
        if (request.getStatus().getName() == RequestStatus.CONFIRMED) {
            adjustEventConfirmedRequests(eventFullDto, -1);
        }

        log.info("Заявка на участие с id = {} отменена пользователем ID: {}", requestId, userId);
        return RequestMapper.toRequestDto(request);
    }

    private Long getUserById(Long userId) {
        try {
            return userApi.getUserById(userId).getId();
        } catch (FeignException e) {
            new NotFoundException("Не найден пользователя с ID: " + userId);
            return null;
        }
    }

    private EventFullDto getEventById(Long eventId) {
        try {
            return eventApi.getEventFullDtoById(eventId);
        } catch (FeignException e) {
            new NotFoundException("Не найдено событие с ID: " + eventId);
            return null;
        }
    }

    private Request getRequestById(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Не найдена заявка с ID: " + requestId));
    }

    private RequestStatusEntity getRequestStatusEntityByRequestStatus(RequestStatus newStatus) {
        return requestStatusRepository.findByName(newStatus)
                .orElseThrow(() -> new NotFoundException("Не найден статус: " + newStatus.name()));
    }

    private Request buildNewRequest(Long initiator, EventFullDto event) {
        RequestStatusEntity requestStatusEntity = getRequestStatusEntityByRequestStatus(RequestStatus.PENDING);
        return Request.builder()
                .requesterId(initiator)
                .eventId(event.getId())
                .created(LocalDateTime.now())
                .status(requestStatusEntity)
                .build();
    }

    private void determineInitialStatus(EventFullDto eventFullDto, Request request) {
        if (shouldAutoConfirm(eventFullDto)) {
            request.setStatus(getRequestStatusEntityByRequestStatus(RequestStatus.CONFIRMED));
        } else if (isEventFull(eventFullDto)) {
            request.setStatus(getRequestStatusEntityByRequestStatus(RequestStatus.REJECTED));
        }
    }


    private boolean shouldAutoConfirm(EventFullDto event) {
        return event.getParticipantLimit() == 0 ||
                (!event.getRequestModeration() && hasAvailableSlots(event));
    }

    private boolean isEventFull(EventFullDto event) {
        return event.getParticipantLimit() > 0 &&
                event.getConfirmedRequests() >= event.getParticipantLimit();
    }

    private boolean hasAvailableSlots(EventFullDto event) {
        return event.getConfirmedRequests() < event.getParticipantLimit();
    }

    private void updateEventStatistics(EventFullDto event, RequestStatus status) {
        if (status == RequestStatus.CONFIRMED) {
            adjustEventConfirmedRequests(event, 1);
        }
    }

    private void adjustEventConfirmedRequests(EventFullDto eventFullDto, int delta) {
        eventApi.increaseConfirmed(eventFullDto.getId(), delta);
    }

    private void updateRequestStatus(Request request, RequestStatus newStatus) {
        String currentStatusName = request.getStatus().getName().name();
        if (currentStatusName.equals(newStatus.name())) {
            throw new ValidationException("Статус уже установлен: " + newStatus);
        }
        request.setStatus(getRequestStatusEntityByRequestStatus(newStatus));
    }

    @Transactional(readOnly = true)
    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        EventFullDto eventFullDto = getEventById(eventId);
        requestValidator.validateEventOwnership(eventFullDto, userId);

        return requestRepository.findByEventId(eventId)
                .stream()
                .map(RequestMapper::toRequestDto)
                .collect(Collectors.toList());
    }

    public Map<String, List<ParticipationRequestDto>> approveRequests(Long userId,
                                                                      Long eventId,
                                                                      EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest) {
        Long initiator = getUserById(userId);
        EventFullDto eventFullDto = getEventById(eventId);
        requestValidator.validateInitiator(eventFullDto, initiator);

        List<Request> requests = getAndValidateRequests(eventId, eventRequestStatusUpdateRequest.getRequestIds());
        RequestStatus status = eventRequestStatusUpdateRequest.getStatus();

        if (status == RequestStatus.CONFIRMED) {
            requestValidator.validateParticipantLimit(eventFullDto);
        }

        return processStatusSpecificLogic(eventFullDto, requests, status);
    }

    private List<Request> getAndValidateRequests(Long eventId, List<Long> requestIds) {
        List<Request> requests = requestRepository.findRequestByIdIn(requestIds);
        requestValidator.validateRequestsBelongToEvent(requests, eventId);
        return requests;
    }

    private Map<String, List<ParticipationRequestDto>> processStatusSpecificLogic(EventFullDto event,
                                                                                  List<Request> requests,
                                                                                  RequestStatus status) {
        if (status == RequestStatus.REJECTED) {
            return processRejection(requests);
        } else {
            return processConfirmation(event, requests);
        }
    }

    private Map<String, List<ParticipationRequestDto>> processRejection(List<Request> requests) {
        requestValidator.validateNoConfirmedRequests(requests);
        updateRequestStatuses(requests, RequestStatus.REJECTED);
        List<ParticipationRequestDto> rejectedRequests = requestRepository.saveAll(requests)
                .stream()
                .map(RequestMapper::toRequestDto)
                .toList();

        return Map.of("rejectedRequests", rejectedRequests);
    }

    private void updateRequestStatuses(List<Request> requests, RequestStatus status) {
        RequestStatusEntity requestStatusEntity =
                requestStatusRepository.findByName(status)
                        .orElseThrow(() -> new IllegalArgumentException("Не верный статус"));
        requests.forEach(request -> request.setStatus(requestStatusEntity));
    }

    private Map<String, List<ParticipationRequestDto>> processConfirmation(EventFullDto event, List<Request> requests) {
        requestValidator.validateAllRequestsPending(requests);

        int availableSlots = event.getParticipantLimit() - event.getConfirmedRequests();
        List<Request> confirmed = requests.stream().limit(availableSlots).toList();
        List<Request> rejected = requests.stream().skip(availableSlots).toList();

        updateRequestStatuses(confirmed, RequestStatus.CONFIRMED);
        updateRequestStatuses(rejected, RequestStatus.REJECTED);

        requestRepository.saveAll(requests);
        updateEventConfirmedRequests(event, confirmed.size());

        return Map.of(
                "confirmedRequests", mapToParticipationRequestDtoList(confirmed),
                "rejectedRequests", mapToParticipationRequestDtoList(rejected)
        );
    }

    private void updateEventConfirmedRequests(EventFullDto eventFullDto, int newConfirmations) {
        eventApi.increaseConfirmed(eventFullDto.getId(), newConfirmations);

    }

    private List<ParticipationRequestDto> mapToParticipationRequestDtoList(List<Request> requests) {
        return requests.stream()
                .map(RequestMapper::toRequestDto)
                .toList();
    }
}

