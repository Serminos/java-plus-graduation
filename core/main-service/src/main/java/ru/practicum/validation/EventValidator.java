package ru.practicum.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.api.UserApi;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.event.UpdateEventUserRequest;
import ru.practicum.event.model.Event;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventValidator {
    private final UserApi userApi;

    public void validateUserExists(Long userId) {
        if (userApi.getUserById(userId) == null) {
            log.error("Пользователя с id {} не найден", userId);
            throw new NotFoundException("Пользователя с id не найден: " + userId);
        }
    }

    public void validateUserUpdate(Event oldEvent, Long initiator, UpdateEventUserRequest updateDto) {
        if (!oldEvent.getInitiatorId().equals(initiator)) {
            throw new ValidationException("Только пользователь создавший событие может его редактировать");
        }
        if (oldEvent.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Нельзя изменить опубликованное событие");
        }
        if (Objects.nonNull(updateDto.getEventDate()) &&
                updateDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Событие не может начинаться ранее чем через 2 часа");
        }
    }

    public void validateAdminEventDate(Event oldEvent) {
        if (oldEvent.getPublishedOn() == null) return;
        LocalDateTime minEventStartTime = oldEvent.getPublishedOn().plusHours(1);
        if (oldEvent.getEventDate().isBefore(minEventStartTime)) {
            throw new ConflictException(
                    "Событие не может начинаться раньше чем через 1 час после публикации. " +
                            "Минимальное время: " + minEventStartTime
            );
        }
    }

    public void validateAdminPublishedEventDate(LocalDateTime newEventDate, Event oldEvent) {
        LocalDateTime minEventStartTime = oldEvent.getPublishedOn() != null
                ? oldEvent.getPublishedOn().plusHours(1)
                : LocalDateTime.now().plusHours(1);

        if (newEventDate != null && newEventDate.isBefore(minEventStartTime)) {
            throw new ConflictException("Новая дата начала должна быть не ранее " + minEventStartTime);
        }
    }

    public void validateAdminEventUpdateState(EventState currentState) {
        if (currentState == EventState.PUBLISHED || currentState == EventState.CANCELED) {
            throw new ConflictException("Запрещено редактирование в статусах: " +
                    String.join(", ", EventState.PUBLISHED.name(), EventState.CANCELED.name()));
        }
    }

    public void validateEventOwnership(Event event, Long userId) {
        if (!event.getInitiatorId().equals(userId)) {
            throw new ValidationException("Только пользователь создавший событие может получить его полное описание");
        }
    }

}