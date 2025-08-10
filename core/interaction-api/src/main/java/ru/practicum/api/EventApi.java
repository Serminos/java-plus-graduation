package ru.practicum.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.event.EventFullDto;

@FeignClient(name = "event-service", path = "/admin/events", fallbackFactory = EventApiFallback.class)
public interface EventApi {
    @GetMapping("/{id}")
    EventFullDto getEventById(@PathVariable Long id);

    @GetMapping("/{eventId}/initiator/{userId}")
    EventFullDto getEventByIdAndInitiator(@PathVariable Long eventId,
                                          @PathVariable Long userId);

    @PostMapping("/{eventId}")
    EventFullDto increaseConfirmed(@PathVariable Long eventId, @RequestParam Integer quantity);
}
