package ru.practicum.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.event.EventFullDto;

@FeignClient(name = "event-service", fallbackFactory = EventApiFallback.class)
public interface EventApi {
    @GetMapping("/admin/events/{eventId}")
    EventFullDto getEventFullDtoById(@PathVariable Long eventId);

    @GetMapping("/users/{userId}/events/{eventId}")
    EventFullDto getEventByIdAndInitiator(@PathVariable Long userId, @PathVariable Long eventId);

    @PostMapping("/admin/events/{eventId}")
    void increaseConfirmed(@PathVariable Long eventId, @RequestParam Integer quantity);
}
