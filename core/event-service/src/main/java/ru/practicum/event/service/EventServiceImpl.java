package ru.practicum.event.service;

import com.google.protobuf.Timestamp;
import feign.FeignException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.practicum.api.RequestApi;
import ru.practicum.api.UserApi;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.client.AnalyzerClient;
import ru.practicum.client.CollectorClient;
import ru.practicum.dto.event.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.Location;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.repository.LocationRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.grpc.stats.action.UserActionProto;
import ru.practicum.grpc.stats.event.InteractionsCountRequestProto;
import ru.practicum.grpc.stats.event.RecommendedEventProto;
import ru.practicum.grpc.stats.event.UserPredictionsRequestProto;
import ru.practicum.validation.EventValidator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserApi userApi;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final RequestApi requestApi;
    private final EventValidator eventValidator;
    private final CollectorClient collectorClient;
    private final AnalyzerClient analyzerClient;


    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, Pageable pageable) {
        eventValidator.validateUserExists(userId);

        return eventRepository.findByInitiatorId(userId, pageable)
                .stream()
                .map(EventMapper::toShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto request) {
        Long initiator = getUserById(userId);
        Category category = getCategoryById(request.getCategory());
        Location location = resolveLocation(request.getLocation());

        Event event = EventMapper.toEvent(request, initiator, category);
        event.setLocation(location);
        event.setState(EventState.PENDING);

        Event savedEvent = eventRepository.save(event);
        log.info("Событие успешно добавлено под id {} со статусом {} и ожидается подтверждение",
                savedEvent.getId(), event.getState());
        return EventMapper.toFullDto(savedEvent);
    }

    @Transactional(readOnly = true)
    @Override
    public EventFullDto getEventByIdAndInitiator(Long userId,
                                                 Long eventId) {
        Event event = getEventById(eventId);
        eventValidator.validateEventOwnership(event, userId);
        return EventMapper.toFullDto(event);
    }

    @Transactional(readOnly = true)
    @Override
    public EventFullDto getEventFullDtoById(Long eventId) {
        Event event = getEventById(eventId);
        return EventMapper.toFullDto(event);
    }

    @Override
    public EventFullDto updateUserEvent(Long userId,
                                        Long eventId,
                                        UpdateEventUserRequest updateDto) {
        Long initiator = getUserById(userId);
        Event event = getEventById(eventId);

        eventValidator.validateUserUpdate(event, initiator, updateDto);
        applyUserUpdates(event, updateDto);

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие успешно обновлено под id {} и дожидается подтверждения", eventId);
        return EventMapper.toFullDto(updatedEvent);
    }

    @Transactional(readOnly = true)
    @Override
    public List<EventFullDto> searchEventsByAdmin(SearchAdminEventsParamDto searchParams) {

        return eventRepository.findAll((root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();

                    // Фильтр по пользователям
                    if (searchParams.getUsers() != null && !searchParams.getUsers().isEmpty()) {
                        predicates.add(root.get("initiatorId").in(searchParams.getUsers()));
                    }

                    // Фильтр по состояниям
                    if (searchParams.getEventStates() != null && !searchParams.getEventStates().isEmpty()) {
                        predicates.add(root.get("state").in(searchParams.getEventStates()));
                    }

                    // Фильтр по категориям
                    if (searchParams.getCategoriesIds() != null && !searchParams.getCategoriesIds().isEmpty()) {
                        predicates.add(root.get("category").get("id").in(searchParams.getCategoriesIds()));
                    }

                    // Фильтр по датам
                    predicates.add(cb.between(root.get("eventDate"), searchParams.getRangeStart(),
                            searchParams.getRangeEnd()));

                    return cb.and(predicates.toArray(new Predicate[0]));
                }, searchParams.getPageRequest()).stream()
                .map(EventMapper::toFullDto)
                .collect(Collectors.toList());
    }


    @Override
    public EventFullDto updateEventByAdmin(Long eventId,
                                           UpdateEventAdminRequest updateEventAdminRequest) {
        Event oldEvent = getEventById(eventId);
        eventValidator.validateAdminPublishedEventDate(updateEventAdminRequest.getEventDate(), oldEvent);
        eventValidator.validateAdminEventDate(oldEvent);
        eventValidator.validateAdminEventUpdateState(oldEvent.getState());
        applyAdminUpdates(oldEvent, updateEventAdminRequest);
        Event event = eventRepository.save(oldEvent);
        log.info("Событие успешно обновлено администратором");
        return EventMapper.toFullDto(event);
    }

    private void handleStateUpdateEventAdminRequest(StateAction action, Event event) {
        if (event.getState() != EventState.PENDING) {
            throw new ConflictException("Изменение статуса возможно только для событий в состоянии PENDING");
        }

        switch (action) {
            case PUBLISH_EVENT -> {
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            }
            case REJECT_EVENT -> {
                event.setState(EventState.CANCELED);
                event.setPublishedOn(null);
            }
            default -> throw new UnsupportedOperationException("Неподдерживаемая операция: " + action);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<EventShortDto> searchPublicEvents(SearchPublicEventsParamDto searchParams) {

        Specification<Event> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Базовые условия
            predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), searchParams.getRangeStart()));
            predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), searchParams.getRangeEnd()));
            predicates.add(cb.equal(root.get("state"), EventState.PUBLISHED));

            // Фильтр по тексту
            if (StringUtils.hasText(searchParams.getText())) {
                String searchTerm = "%" + searchParams.getText().toLowerCase() + "%";
                Predicate annotationLike = cb.like(cb.lower(root.get("annotation")), searchTerm);
                Predicate descriptionLike = cb.like(cb.lower(root.get("description")), searchTerm);
                predicates.add(cb.or(annotationLike, descriptionLike));
            }

            // Фильтр по категориям
            if (searchParams.getCategoriesIds() != null && !searchParams.getCategoriesIds().isEmpty()) {
                predicates.add(root.get("category").get("id").in(searchParams.getCategoriesIds()));
            }

            // Фильтр по paid
            if (searchParams.getPaid() != null) {
                predicates.add(cb.equal(root.get("paid"), searchParams.getPaid()));
            }

            // Фильтр по доступности
            if (searchParams.isOnlyAvailable()) {
                predicates.add(cb.gt(root.get("participantLimit"), root.get("confirmedRequests")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Event> eventsPage = eventRepository.findAll(
                specification,
                PageRequest.of(searchParams.getPageRequest().getPageNumber(),
                        searchParams.getPageRequest().getPageSize(),
                        searchParams.getPageRequest().getSort())
        );

        List<Event> events = eventsPage.getContent();
        if (events.isEmpty()) {
            return new ArrayList<>();
        }
        return paginateAndMap(events, searchParams.getPageRequest());
    }

    @Transactional(readOnly = true)
    @Override
    public EventFullDto getPublicEvent(Long eventId, Long userId) {
        Event event = getEventById(eventId);
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException("У события должен быть статус <PUBLISHED>");
        }
        sendStats(userId, eventId);
        EventFullDto eventFullDto = EventMapper.toFullDto(event);
        eventFullDto.setRating(getEventRating(event.getId()));
        return eventFullDto;
    }

    @Override
    public List<EventShortDto> getRecommendations(Long userId, int maxResults) {
        List<RecommendedEventProto> protoList = analyzerClient.getRecommendationsForUser(UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build());

        if (protoList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> eventIds = protoList.stream()
                .map(RecommendedEventProto::getEventId)
                .collect(Collectors.toList());
        List<Event> events = eventRepository.findAllById(eventIds);
        Map<Long, Event> eventMap = events.stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));

        List<EventShortDto> eventShortDtos = new ArrayList<>();
        for (RecommendedEventProto proto : protoList) {
            Event event = eventMap.get(proto.getEventId());
            if (event != null) {
                eventShortDtos.add(EventMapper.toShortDto(event));
            }
        }

        return eventShortDtos;
    }

    @Override
    public void addLikeToEvent(Long eventId, Long userId) {
        log.debug("Сохраняем лайк от пользователя = {}", userId);
        collectorClient.newUserAction(UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(ActionTypeProto.ACTION_LIKE)
                .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond())
                        .setNanos(Instant.now().getNano()).build())
                .build());
    }

    private void sendStats(long userId, long eventId) {
        log.debug("Сохраняем просмотр от пользователя = {}", userId);
        collectorClient.newUserAction(UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(ActionTypeProto.ACTION_VIEW)
                .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond())
                        .setNanos(Instant.now().getNano()).build())
                .build());
    }

    private double getEventRating(long eventId) {
        List<RecommendedEventProto> proto = analyzerClient.getInteractionsCount(InteractionsCountRequestProto.newBuilder()
                .addEventId(eventId)
                .build());
        if (proto.isEmpty())
            return 0;
        else
            return proto.get(0).getScore();
    }

    private List<EventShortDto> paginateAndMap(List<Event> events, PageRequest pageRequest) {
        List<Event> paginatedEvents = events.stream()
                .skip(pageRequest.getOffset())
                .toList();

        return paginatedEvents.stream()
                .map(EventMapper::toShortDto)
                .toList();
    }

    private Long getUserById(Long userId) {
        try {
            return userApi.getUserById(userId).getId();
        } catch (FeignException e) {
            log.error(e.toString());
            new NotFoundException("Не найден пользователя с ID: " + userId);
            return null;
        }
    }

    private Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Не найдено событие с ID: " + eventId));
    }

    private Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ValidationException("Указана не правильная ID категории: " + categoryId));
    }

    private Location resolveLocation(LocationDto requestLocation) {
        Location mayBeExistingLocation = null;
        mayBeExistingLocation = locationRepository
                .findByLatAndLon(requestLocation.getLat(), requestLocation.getLon())
                .orElseGet(() -> locationRepository.save(mapLocationDtoToLocation(requestLocation)));
        return mayBeExistingLocation;
    }

    private void applyAdminUpdates(Event event, UpdateEventAdminRequest update) {
        Optional.ofNullable(update.getAnnotation()).ifPresent(event::setAnnotation);
        Optional.ofNullable(update.getDescription()).ifPresent(event::setDescription);
        Optional.ofNullable(update.getEventDate()).ifPresent(event::setEventDate);
        Optional.ofNullable(mapLocationDtoToLocation(update.getLocation())).ifPresent(event::setLocation);
        Optional.ofNullable(update.getPaid()).ifPresent(event::setPaid);
        Optional.ofNullable(update.getParticipantLimit()).ifPresent(event::setParticipantLimit);
        Optional.ofNullable(update.getRequestModeration()).ifPresent(event::setRequestModeration);
        Optional.ofNullable(update.getTitle()).ifPresent(event::setTitle);

        Optional.ofNullable(update.getCategory())
                .map(categoryId -> categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ValidationException("Не найдена категория с ID: " + categoryId)))
                .ifPresent(event::setCategory);
        Optional.ofNullable(update.getStateAction())
                .ifPresent(action -> handleStateUpdateEventAdminRequest(action, event));
    }

    private Location mapLocationDtoToLocation(LocationDto locationDto) {
        if (locationDto == null) return null;
        return new Location(null, locationDto.getLat(), locationDto.getLon());
    }

    @Override
    public EventFullDto increaseConfirmed(Long eventId, Integer delta) {
        eventRepository.incrementConfirmedRequests(eventId, delta);
        return EventMapper.toFullDto(getEventById(eventId));
    }

    private void applyUserUpdates(Event event, UpdateEventUserRequest update) {

        Optional.ofNullable(update.getAnnotation()).ifPresent(event::setAnnotation);
        Optional.ofNullable(update.getDescription()).ifPresent(event::setDescription);
        Optional.ofNullable(update.getEventDate()).ifPresent(event::setEventDate);
        Optional.ofNullable(update.getPaid()).ifPresent(event::setPaid);
        Optional.ofNullable(update.getParticipantLimit()).ifPresent(event::setParticipantLimit);
        Optional.ofNullable(update.getRequestModeration()).ifPresent(event::setRequestModeration);
        Optional.ofNullable(update.getTitle()).ifPresent(event::setTitle);

        Optional.ofNullable(update.getCategory())
                .map(this::getCategoryById)
                .ifPresent(event::setCategory);

        Optional.ofNullable(update.getLocation())
                .map(this::resolveLocation)
                .ifPresent(event::setLocation);

        updateState(update.getStateAction(), event);
    }

    private void updateState(StateAction stateAction, Event event) {
        if (stateAction == null) return;
        switch (stateAction) {
            case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
            case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
        }
    }

}
