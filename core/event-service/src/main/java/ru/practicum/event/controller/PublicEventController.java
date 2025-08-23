package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.SearchPublicEventsParamDto;
import ru.practicum.event.model.EventSort;
import ru.practicum.event.service.EventService;
import ru.practicum.exception.ValidationException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicEventController {
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DEFAULT_TEXT = "";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DEFAULT_PAGE_START = 0;
    private static final int START_SEARCH_DATE_PERIOD = 100;
    private static final int END_SEARCH_DATE_PERIOD = 300;
    private final EventService eventService;
    private final String authHeaderKey = "X-EWM-USER-ID";
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    @GetMapping
    public ResponseEntity<List<EventShortDto>> searchPublicEvents(
            @RequestParam(defaultValue = DEFAULT_TEXT) String text,
            @RequestParam(required = false) List<Long> categoriesIds,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = DATE_TIME_PATTERN) LocalDateTime rangeStart,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = DATE_TIME_PATTERN) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") boolean onlyAvailable,
            @RequestParam(required = false) EventSort eventSort,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_START) @PositiveOrZero int from,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Positive int size,
            HttpServletRequest request) {
        log.info("Запрос на получение опубликованных событий: text='{}', " +
                        "categoriesIds={}, paid={}, start={}, end={}, onlyAvailable={}, eventSort={}",
                text, categoriesIds, paid, rangeStart, rangeEnd, onlyAvailable, eventSort);
        validateTimeRange(rangeStart, rangeEnd);
        if (rangeStart == null) rangeStart = LocalDateTime.now();
        if (rangeEnd == null) rangeEnd = LocalDateTime.now().plusYears(100);

        PageRequest pageRequest = createPageRequest(from, size, eventSort);
        SearchPublicEventsParamDto searchPublicEventsParamDto =
                SearchPublicEventsParamDto.builder().text(text)
                        .categoriesIds(categoriesIds)
                        .paid(paid)
                        .rangeStart(rangeStart)
                        .rangeEnd(rangeEnd)
                        .onlyAvailable(onlyAvailable)
                        .pageRequest(pageRequest)
                        .build();

        List<EventShortDto> eventShortDtos = eventService.searchPublicEvents(searchPublicEventsParamDto);

        return ResponseEntity.ok(eventShortDtos);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventFullDto> getEvent(
            @PathVariable @Positive Long eventId,
            @RequestHeader("X-EWM-USER-ID") Long userId) {
        log.info("Запрос на получение опубликованого события с id {} пользователем", eventId,userId);
        EventFullDto eventFullDto = eventService.getPublicEvent(eventId, userId);

        return ResponseEntity.ok(eventFullDto);
    }

    private PageRequest createPageRequest(int from, int size, EventSort sort) {
        int page = from / size;
        Sort sorting = (sort == EventSort.VIEWS)
                ? Sort.by(Sort.Direction.DESC, "views")
                : Sort.by(Sort.Direction.ASC, "eventDate");

        return PageRequest.of(page, size, sorting);
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new ValidationException("Время начала должно быть до окончания");
        }
    }


    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(@RequestHeader(authHeaderKey) Long userId,
                                                        @RequestParam(defaultValue = "10") int maxResults) {
        log.info("Запрос на получение рекомендаций от пользователя {} с параметром maxResults={}",
                userId, maxResults);
        List<EventShortDto> recommendations = eventService.getRecommendations(userId, maxResults);
        log.info("Ответ на запрос на получение рекомендаций пользователю {} с телом: {}",
                userId, recommendations);
        return recommendations;
    }

    @PutMapping("/{eventId}/like")
    public void addLikeToEvent(@PathVariable Long eventId, @RequestHeader(authHeaderKey) Long userId) {
        log.info("Добавить лайк событию {} от пользователя {}", eventId, userId);
        eventService.addLikeToEvent(eventId, userId);
        log.info("Обработан добавление лайка событию {} от пользователя {}", eventId, userId);
    }
}