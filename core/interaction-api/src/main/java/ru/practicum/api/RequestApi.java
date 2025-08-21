package ru.practicum.api;

import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.ParticipationRequestDto;

@FeignClient(name = "request-service", path = "/users/{userId}/requests", fallbackFactory = RequestApiFallback.class)
public interface RequestApi {
    @GetMapping
    ParticipationRequestDto getUserRequests(@PathVariable @Positive Long userId);

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    ParticipationRequestDto createParticipationRequest(@PathVariable Long userId,
                                                       @RequestParam Long eventId);


    @PatchMapping("/{requestId}/cancel")
    ParticipationRequestDto cancelParticipationRequest(@PathVariable @Positive Long userId,
                                                       @PathVariable @Positive Long requestId);
}
