package ru.practicum.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.exception.ServiceUnavailableException;


@Component
@Slf4j
public class EventApiFallback {
    public EventFullDto getEventById(@PathVariable Long id) {
        log.warn("Активирован резервный вариант для getEventById для события с id {} ", id);
        throw new ServiceUnavailableException("EventService недоступен");
    }

    public EventFullDto getEventByIdAndInitiator(@PathVariable Long eventId,
                                                 @PathVariable Long userId) {
        log.warn("Активирован резервный вариант для getEventByIdAndInitiator для события с id {} ", eventId);
        throw new ServiceUnavailableException("EventService недоступен");
    }

    public EventFullDto increaseConfirmed(@PathVariable Long eventId, @RequestParam Integer quantity) {
        log.warn("Активирован резервный вариант для increaseConfirmed для события с id {} ", eventId);
        throw new ServiceUnavailableException("EventService недоступен");
    }
}
