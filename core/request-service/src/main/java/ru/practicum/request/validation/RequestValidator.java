package ru.practicum.request.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.model.Request;
import ru.practicum.request.repository.RequestRepository;

import java.util.List;

@Slf4j
@Component
public class RequestValidator {

    private final RequestRepository requestRepository;

    @Autowired
    public RequestValidator(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    public void validateRequestCreation(Long initiator, EventFullDto event) {
        checkEventState(event);
        checkEventOwnership(initiator, event);
        checkDuplicateRequest(initiator, event.getId());
        checkEventCapacity(event);
    }

    private void checkEventState(EventFullDto eventFullDto) {
        if (!eventFullDto.getState().name().equals(EventState.PUBLISHED.name())) {
            throw new ConflictException("Нельзя подавать заявку на неопубликованное мероприятие");
        }
    }

    private void checkEventOwnership(Long initiator, EventFullDto event) {
        if (event.getInitiator().equals(initiator)) {
            throw new ConflictException("Пользователь не может подать заяку на участие в своем же мероприятии");
        }
    }

    private void checkDuplicateRequest(Long userId, Long eventId) {
        requestRepository.findByRequesterIdAndEventId(userId, eventId)
                .ifPresent(req -> {
                    throw new ConflictException("Пользователь: " +
                            userId + " уже подал заявку на участи в событии: " + eventId);
                });
    }

    public void validateRequestOwnership(Long initiator, Request request) {
        if (!request.getRequesterId().equals(initiator)) {
            throw new ValidationException("Только пользователь подавший заявку может отменить ее. " +
                    "Пользователь ID: " + initiator +
                    "Заявка с ID: " + request.getId());
        }
    }

    private void checkEventCapacity(EventFullDto event) {
        if (event.getParticipantLimit() > 0 &&
                event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("Событие с ID: " + event.getId() + " нет свободных слотов");
        }
    }

    public void validateInitiator(EventFullDto event, Long initiator) {
        if (!event.getInitiator().equals(initiator)) {
            throw new ValidationException("У этого события другой инициатор");
        }
    }

    public void validateRequestsBelongToEvent(List<Request> requests, Long eventId) {
        boolean allMatch = requests.stream()
                .allMatch(request -> request.getEventId().equals(eventId));
        if (!allMatch) {
            throw new ValidationException("Неверно передан список запросов");
        }
    }

    public void validateParticipantLimit(EventFullDto event) {
        if (event.getParticipantLimit() != 0 &&
                event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("Лимит заявок на участие в событии исчерпан");
        }
    }

    public void validateNoConfirmedRequests(List<Request> requests) {
        if (requests.stream().anyMatch(r -> r.getStatus().getName() == RequestStatus.CONFIRMED)) {
            throw new ConflictException("Нельзя отменить уже подтвержденные заявки");
        }
    }

    public void validateAllRequestsPending(List<Request> requests) {
        if (requests.stream().anyMatch(r -> r.getStatus().getName() != RequestStatus.PENDING)) {
            throw new ConflictException("Все заявки должны быть в статусе ожидания");
        }
    }

    public void validateEventOwnership(EventFullDto event, Long userId) {
        if (!event.getInitiator().equals(userId)) {
            throw new ValidationException("Только пользователь создавший событие может получить его полное описание");
        }
    }
}
