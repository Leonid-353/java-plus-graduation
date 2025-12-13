package ru.yandex.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.event.EventFullDto;
import ru.yandex.practicum.dto.event.EventShortDto;
import ru.yandex.practicum.dto.event.EventWithCommentsDto;
import ru.yandex.practicum.event.service.EventService;
import ru.yandex.practicum.event.service.EventServiceImpl;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {
    private final EventService eventService;
    private final EventServiceImpl eventServiceImpl;

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
            @RequestParam(defaultValue = "10") @Positive Integer size,
            HttpServletRequest request) {

        log.info("Получен запрос GET /events с параметрами: text={}, categories={}, paid={}, rangeStart={}, " +
                        "rangeEnd={}, onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        // Сохраняем информацию о просмотре для статистики
        eventServiceImpl.addHit(request);
        return eventService.getEvents(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);
    }

    @GetMapping("/{id}")
    public EventFullDto getEvent(@PathVariable Long id, HttpServletRequest request) {
        log.info("Получен запрос GET /events/{}", id);
        // Сохраняем информацию о просмотре для статистики
        eventServiceImpl.addHit(request);
        return eventService.getEvent(id);
    }

    @GetMapping("/comments/{eventId}")
    public EventWithCommentsDto getEventWithComments(@PathVariable Long eventId) {
        return eventService.getEventWithComments(eventId);
    }
}