package ru.yandex.practicum.event.service;

import ru.yandex.practicum.dto.event.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface EventService {
    // Public methods
    Collection<EventShortDto> getEvents(String text, List<Long> categories, Boolean paid,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                        Boolean onlyAvailable, String sort, Integer from, Integer size);

    EventFullDto getEvent(Long userId, Long eventId);

    EventWithCommentsDto getEventWithComments(Long userId, Long eventId);

    // Private methods
    Collection<EventShortDto> getUserEvents(Long userId, Integer from, Integer size);

    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    EventFullDto getUserEvent(Long userId, Long eventId);

    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest);

    // Admin methods
    Collection<EventFullDto> getEventsAdmin(List<Long> users, List<String> states, List<Long> categories,
                                            LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size);

    EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest);

    // Feign methods
    EventFullDto getEventByIdFeign(Long eventId);

    EventFullDto getEventByUserFeign(Long userId, Long eventId);

    void updateEventForRequests(Long eventId, Long confirmedRequests);

    // Likes and Recommendations
    void likeEvent(Long userId, Long eventId);

    List<EventFullDto> getRecommendations(Long userId);
}