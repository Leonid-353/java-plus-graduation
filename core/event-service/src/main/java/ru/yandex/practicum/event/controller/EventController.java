package ru.yandex.practicum.event.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.constants.HttpHeaders;
import ru.yandex.practicum.dto.event.EventFullDto;
import ru.yandex.practicum.dto.event.EventShortDto;
import ru.yandex.practicum.dto.event.EventWithCommentsDto;
import ru.yandex.practicum.event.service.EventService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {
    private final EventService eventService;

    @GetMapping
    public Collection<EventShortDto> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {

        log.info("Получен запрос GET /events с параметрами: text={}, categories={}, paid={}, rangeStart={}, " +
                        "rangeEnd={}, onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        return eventService.getEvents(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEvent(@RequestHeader(HttpHeaders.USER_ID_HEADER) Long userId,
                                 @PathVariable Long eventId) {
        log.info("Получен запрос GET /events/{}", eventId);

        return eventService.getEvent(userId, eventId);
    }

    @GetMapping("/comments/{eventId}")
    public EventWithCommentsDto getEventWithComments(@RequestHeader(HttpHeaders.USER_ID_HEADER) Long userId,
                                                     @PathVariable Long eventId) {
        return eventService.getEventWithComments(userId, eventId);
    }

    @PutMapping("/{eventId}/like")
    public void likeEvent(@RequestHeader(HttpHeaders.USER_ID_HEADER) Long userId,
                          @PathVariable Long eventId) {
        log.info("Получен запрос оценки 'like' события ID: {} пользователем ID: {}", eventId, userId);
        eventService.likeEvent(userId, eventId);
    }

    @GetMapping("/recommendations")
    public List<EventFullDto> getRecommendations(@RequestHeader(HttpHeaders.USER_ID_HEADER) Long userId) {
        log.info("Получен запрос рекомендаций для пользователя с ID: {}", userId);
        return eventService.getRecommendations(userId);
    }
}