package ru.practicum.request.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.service.RequestService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class RequestController {

    private final RequestService requestService;

    @GetMapping("/users/{userId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getUserRequests(@PathVariable @Positive Long userId) {
        log.info("Запрос на получение всех заявок участия пользователя с id {}", userId);
        return ResponseEntity.ok(requestService.getUserRequests(userId));
    }

    @PostMapping("/users/{userId}/requests")
    @ResponseStatus(code = HttpStatus.CREATED)
    public ResponseEntity<ParticipationRequestDto> createParticipationRequest(@PathVariable Long userId,
                                                                              @RequestParam Long eventId) {
        log.info("Запрос на создание заявки на участие пользователя с id {} в событии с id {}", userId, eventId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(requestService.createParticipationRequest(userId, eventId));
    }

    @PatchMapping("/users/{userId}/requests/{requestId}/cancel")
    public ResponseEntity<ParticipationRequestDto> cancelParticipationRequest(@PathVariable @Positive Long userId,
                                                                              @PathVariable @Positive Long requestId) {
        log.info("Запрос на отмену заявки на участие с id = {}", requestId);
        return ResponseEntity.ok(requestService.cancelParticipationRequest(userId, requestId));
    }

    @GetMapping("/users/{userId}/events/{eventId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getEventRequests(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId) {
        log.info("Запрос на получение всех заявок на событие с id = {} для пользователя с ID {}", eventId, userId);
        return ResponseEntity.ok(requestService.getEventRequests(userId, eventId));
    }

    @PatchMapping("/users/{userId}/events/{eventId}/requests")
    public ResponseEntity<Map<String, List<ParticipationRequestDto>>> approveRequests(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId,
            @RequestBody @Valid EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest) {
        log.info("Запрос на изменение статуса переданных заявок на событие с id = {} для пользователя с ID {}: {}",
                eventId, userId, eventRequestStatusUpdateRequest);

        return ResponseEntity.ok(requestService.approveRequests(userId, eventId, eventRequestStatusUpdateRequest));
    }
}
