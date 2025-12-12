package ru.yandex.practicum.event.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.dto.event.EventFullDto;
import ru.yandex.practicum.event.service.EventService;
import ru.yandex.practicum.feign.EventClient;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events/feign")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventFeignController implements EventClient {
    final EventService eventService;

    @Override
    public EventFullDto getEventByIdFeign(Long eventId) {
        return eventService.getEventByIdFeign(eventId);
    }

    @Override
    public EventFullDto getEventByUserFeign(Long userId, Long eventId) {
        return eventService.getEventByUserFeign(userId, eventId);
    }

    @Override
    public void updateEventForRequests(Long eventId, EventFullDto eventFullDto) {
        eventService.updateEventForRequests(eventId, eventFullDto);
    }
}
