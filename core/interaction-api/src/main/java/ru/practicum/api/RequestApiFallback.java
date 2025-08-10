package ru.practicum.api;

import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.exception.ServiceUnavailableException;

@Component
@Slf4j
public class RequestApiFallback {

    public ParticipationRequestDto getUserRequests(@PathVariable @Positive Long userId) {
        log.warn("Активирован резервный вариант для getUserRequests для запроса  c userId {}", userId);
        throw new ServiceUnavailableException("RequestService недоступен");
    }

    public ParticipationRequestDto createParticipationRequest(@PathVariable Long userId,
                                                              @RequestParam Long eventId) {
        log.warn("Активирован резервный вариант для createParticipationRequest для запроса  c userId {} и eventId {}",
                userId, eventId);
        throw new ServiceUnavailableException("RequestService недоступен");
    }

    public ParticipationRequestDto cancelParticipationRequest(@PathVariable @Positive Long userId,
                                                              @PathVariable @Positive Long requestId) {
        log.warn("Активирован резервный вариант для cancelParticipationRequest для запроса  c userId {} и requestId {}",
                userId, requestId);
        throw new ServiceUnavailableException("RequestService недоступен");
    }
}

