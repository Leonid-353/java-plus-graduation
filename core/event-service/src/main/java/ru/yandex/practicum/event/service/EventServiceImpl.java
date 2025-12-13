package ru.yandex.practicum.event.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.category.model.Category;
import ru.yandex.practicum.category.repository.CategoryRepository;
import ru.yandex.practicum.dto.comment.CommentDto;
import ru.yandex.practicum.dto.event.*;
import ru.yandex.practicum.dto.event.enums.State;
import ru.yandex.practicum.dto.stats.StatsDtoRequest;
import ru.yandex.practicum.dto.stats.StatsDtoResponse;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.event.mapper.EventMapper;
import ru.yandex.practicum.event.model.Event;
import ru.yandex.practicum.event.model.Location;
import ru.yandex.practicum.event.model.QEvent;
import ru.yandex.practicum.event.repository.EventRepository;
import ru.yandex.practicum.event.repository.LocationRepository;
import ru.yandex.practicum.exception.ForbiddenException;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.exception.ValidationException;
import ru.yandex.practicum.feign.CommentClient;
import ru.yandex.practicum.feign.StatsClient;
import ru.yandex.practicum.feign.UserClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {
    private static final String APP_NAME = "explore-with-me";
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserClient userClient;
    private final LocationRepository locationRepository;
    private final CommentClient commentClient;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;
    private final EntityManager entityManager;

    // Public methods
    @Override
    public Collection<EventShortDto> getEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort, Integer from, Integer size) {
        Page<Event> page = getEventsPageInTransaction(text, categories, paid, rangeStart,
                rangeEnd, onlyAvailable, sort, from, size);

        List<String> uris = page.getContent().stream()
                .map(e -> "/events/" + e.getId())
                .collect(Collectors.toList());

        Set<Long> initiatorIds = page.getContent().stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());

        Map<Long, Long> viewStats = getViewStats(uris);
        Map<Long, UserShortDto> userMap = getUserShortDtos(initiatorIds);

        return page.getContent().stream()
                .map(e -> mapToEventShortDto(e, viewStats, userMap))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getEvent(Long id) {
        Event event = getEventInTransaction(id);

        // Получаем статистику просмотров
        List<String> uris = List.of("/events/" + id);
        Map<Long, Long> viewStats = getViewStats(uris);

        return eventMapper.toEventFullDto(event, viewStats.getOrDefault(id, 0L));
    }

    @Override
    public EventWithCommentsDto getEventWithComments(Long eventId) {
        Event event = findEventById(eventId);
        UserDto user = findUserById(event.getInitiatorId());

        List<String> uris = List.of("/events/" + eventId);
        Map<Long, Long> viewStats = getViewStats(uris);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(event, viewStats.getOrDefault(eventId, 0L));
        UserShortDto userShortDto = UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
        eventFullDto.setInitiator(userShortDto);

        List<CommentDto> comments = commentClient.getEventWithComments(eventId, 0, 3);

        return eventMapper.toEventWithCommentsDto(eventFullDto, comments);
    }

    // Private methods
    @Override
    public Collection<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        UserDto user = findUserById(userId);

        Page<Event> events = getUserEventsInTransaction(user.getId(), from, size);

        // Получаем статистику просмотров
        List<String> uris = events.getContent().stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        Map<Long, Long> viewStats = getViewStats(uris);
        UserShortDto userShortDto = getUserShortDto(userId);

        return events.getContent().stream()
                .map(e -> mapToEventShortDto(e, viewStats, userShortDto, userId))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        UserDto initiator = findUserById(userId);

        LocalDateTime createdOn = LocalDateTime.now();

        // Проверка, что дата события не раньше чем через 2 часа от текущего момента
        if (newEventDto.getEventDate().isBefore(createdOn.plusHours(2))) {
            throw new ValidationException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        return createEventInTransaction(initiator.getId(), newEventDto);
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        UserDto user = findUserById(userId);

        Event event = getUserEventInTransaction(eventId, user.getId());

        // Получаем статистику просмотров
        List<String> uris = List.of("/events/" + eventId);
        Map<Long, Long> viewStats = getViewStats(uris);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(event, viewStats.getOrDefault(eventId, 0L));
        UserShortDto userShortDto = UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
        eventFullDto.setInitiator(userShortDto);

        return eventFullDto;
    }

    @Override
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        UserDto user = findUserById(userId);

        Event event = updateEventInTransaction(user.getId(), eventId, updateRequest);

        // Получаем статистику просмотров
        List<String> uris = List.of("/events/" + eventId);
        Map<Long, Long> viewStats = getViewStats(uris);

        return eventMapper.toEventFullDto(event, viewStats.getOrDefault(eventId, 0L));
    }

    // Admin methods
    @Override
    public Collection<EventFullDto> getEventsAdmin(List<Long> users, List<String> states, List<Long> categories,
                                                   LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                   Integer from, Integer size) {
        Page<Event> events = getEventsInTransactionAdmin(users, states, categories,
                rangeStart, rangeEnd, from, size);

        // Получаем статистику просмотров
        List<String> uris = events.getContent().stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        Map<Long, Long> viewStats = getViewStats(uris);

        Set<Long> initiatorIds = events.getContent().stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());

        Map<Long, UserShortDto> userMap = getUserShortDtos(initiatorIds);

        return events.getContent().stream()
                .map(e -> {
                    EventFullDto dto = eventMapper.toEventFullDto(
                            e, viewStats.getOrDefault(e.getId(), 0L));
                    UserShortDto initiator = userMap.get(e.getInitiatorId());
                    if (initiator != null) {
                        dto.setInitiator(initiator);
                    } else {
                        dto.setInitiator(UserShortDto.builder()
                                .id(e.getInitiatorId())
                                .build());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {
        Event event = updateEventInTransactionAdmin(eventId, updateEventAdminRequest);

        updateEventAdminFields(event, updateEventAdminRequest);

        // Получаем статистику просмотров
        List<String> uris = List.of("/events/" + eventId);
        Map<Long, Long> viewStats = getViewStats(uris);

        return eventMapper.toEventFullDto(event, viewStats.getOrDefault(eventId, 0L));
    }

    // Feign methods
    @Override
    public EventFullDto getEventByIdFeign(Long eventId) {
        Event event = findEventById(eventId);

        List<String> uris = List.of("/events/" + eventId);
        Map<Long, Long> viewStats = getViewStats(uris);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(event, viewStats.getOrDefault(eventId, 0L));
        UserDto userDto = userClient.getUserById(event.getInitiatorId());
        UserShortDto userShortDto = UserShortDto.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .build();

        eventFullDto.setInitiator(userShortDto);
        return eventFullDto;
    }

    @Override
    public EventFullDto getEventByUserFeign(Long userId, Long eventId) {
        return getUserEvent(userId, eventId);
    }

    @Override
    public void updateEventForRequests(Long eventId, Long confirmedRequests) {
        Event event = findEventById(eventId);

        event.setConfirmedRequests(confirmedRequests);
        eventRepository.save(event);
    }

    // Transactional methods
    // Get events page
    @Transactional(readOnly = true)
    private Page<Event> getEventsPageInTransaction(String text, List<Long> categories, Boolean paid,
                                                   LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                   Boolean onlyAvailable, String sort, Integer from, Integer size) {
        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(entityManager);
        QEvent event = QEvent.event;

        Pageable pageable;
        if (sort != null && sort.equals("EVENT_DATE")) {
            pageable = PageRequest.of(from / size, size, Sort.by("eventDate"));
        } else {
            pageable = PageRequest.of(from / size, size);
        }

        BooleanBuilder booleanBuilder = new BooleanBuilder(event.state.eq(State.PUBLISHED));

        if (text != null && !text.trim().isEmpty()) {
            booleanBuilder.or(event.annotation.containsIgnoreCase(text)).or(event.description.containsIgnoreCase(text));
        }

        if (categories != null && !categories.isEmpty()) {
            booleanBuilder.and(event.category.id.in(categories));
        }

        if (paid != null) {
            booleanBuilder.and(event.paid.eq(paid));
        }

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }

        if (rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new ValidationException("Дата окончания не может быть раньше даты начала");
        }
        booleanBuilder.and(event.eventDate.goe(rangeStart));

        if (rangeEnd != null) {
            booleanBuilder.and(event.eventDate.loe(rangeEnd));
        }

        if (Boolean.TRUE.equals(onlyAvailable)) {
            booleanBuilder.and(event.participantLimit.eq(0).or(event.confirmedRequests.lt(event.participantLimit)));
        }

        long totalCount = jpaQueryFactory.selectFrom(event)
                .where(booleanBuilder)
                .fetchCount();

        List<Event> events = jpaQueryFactory.selectFrom(event)
                .where(booleanBuilder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(events, pageable, totalCount);
    }

    private EventShortDto mapToEventShortDto(Event event, Map<Long, Long> viewStats,
                                             Map<Long, UserShortDto> userMap) {
        EventShortDto dto = eventMapper.toEventShortDto(event, viewStats.getOrDefault(event.getId(), 0L));
        UserShortDto userDto = userMap.get(event.getInitiatorId());

        if (userDto != null) {
            dto.setInitiator(userDto);
        } else {
            dto.setInitiator(UserShortDto.builder()
                    .id(event.getInitiatorId())
                    .build());
        }

        return dto;
    }

    // Get event
    @Transactional(readOnly = true)
    private Event getEventInTransaction(Long id) {
        Event event = findEventById(id);

        // Проверяем, что событие опубликовано
        if (event.getState() != State.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + id + " не опубликовано");
        }

        return event;
    }

    // Get user events
    @Transactional(readOnly = true)
    private Page<Event> getUserEventsInTransaction(Long initiatorId, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);

        return eventRepository.findAllByInitiatorId(initiatorId, pageable);
    }

    private EventShortDto mapToEventShortDto(Event event, Map<Long, Long> viewStats,
                                             UserShortDto userShortDto, Long userId) {
        EventShortDto dto = eventMapper.toEventShortDto(event, viewStats.getOrDefault(event.getId(), 0L));

        if (userShortDto != null) {
            dto.setInitiator(userShortDto);
        } else {
            dto.setInitiator(UserShortDto.builder()
                    .id(userId)
                    .build());
        }
        return dto;
    }

    // Create event
    @Transactional
    private EventFullDto createEventInTransaction(Long initiatorId, NewEventDto newEventDto) {
        Category category = findCategoryById(newEventDto.getCategory());

        // Поиск существующей локации или создание новой
        Location location = locationRepository.findByLatAndLon(
                        newEventDto.getLocation().getLat(),
                        newEventDto.getLocation().getLon())
                .orElseGet(() -> locationRepository.save(
                        eventMapper.toLocation(newEventDto.getLocation())
                ));

        Event event = eventMapper.toEvent(newEventDto);
        event.setInitiatorId(initiatorId);
        event.setCategory(category);
        event.setLocation(location);
        event = eventRepository.save(event);

        return eventMapper.toEventFullDto(event, 0L);
    }

    // Get user event
    @Transactional(readOnly = true)
    private Event getUserEventInTransaction(Long eventId, Long initiatorId) {
        return eventRepository.findByIdAndInitiatorId(eventId, initiatorId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }

    // Update event
    @Transactional
    private Event updateEventInTransaction(Long initiatorId, Long eventId, UpdateEventUserRequest updateRequest) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, initiatorId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        // Проверка статуса события
        if (event.getState() == State.PUBLISHED) {
            throw new ForbiddenException("Нельзя изменить опубликованное событие");
        }

        updateEventFields(event, updateRequest);

        // Обновление статуса события
        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case SEND_TO_REVIEW:
                    event.setState(State.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(State.CANCELED);
                    break;
            }
        }

        return eventRepository.save(event);
    }

    // Get events admin
    @Transactional(readOnly = true)
    private Page<Event> getEventsInTransactionAdmin(List<Long> users, List<String> states, List<Long> categories,
                                                    LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                    Integer from, Integer size) {
        Collection<State> statesList = null;
        if (states != null) {
            statesList = states.stream()
                    .map(State::valueOf)
                    .collect(Collectors.toList());
        }

        Pageable pageable = PageRequest.of(from / size, size);

        return eventRepository.findAllByAdmin(users, statesList, categories, rangeStart, rangeEnd, pageable);
    }

    // Update event admin
    @Transactional
    private Event updateEventInTransactionAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {
        Event event = findEventById(eventId);

        updateEventAdminFields(event, updateEventAdminRequest);

        // Обновление статуса события администратором
        if (updateEventAdminRequest.getStateAction() != null) {
            switch (updateEventAdminRequest.getStateAction()) {
                case PUBLISH_EVENT:
                    // Проверяем, что событие находится в состоянии ожидания
                    if (event.getState() != State.PENDING) {
                        throw new ForbiddenException("Событие должно быть в состоянии ожидания публикации");
                    }

                    // Проверяем, что дата начала события не ранее чем через 1 час от текущего момента
                    if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                        throw new ForbiddenException("Дата начала события должна быть не ранее чем через 1 час от момента публикации");
                    }

                    event.setState(State.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;

                case REJECT_EVENT:
                    // Проверяем, что событие не опубликовано
                    if (event.getState() == State.PUBLISHED) {
                        throw new ForbiddenException("Невозможно отклонить опубликованное событие");
                    }

                    event.setState(State.CANCELED);
                    break;
            }
        }

        return eventRepository.save(event);
    }

    // Вспомогательные методы
    @Transactional(readOnly = true)
    private Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Событие с ID: %s не найдено", eventId)));
    }

    private UserDto findUserById(Long userId) {
        return userClient.getUserById(userId);
    }

    // Метод для получения UserShortDto
    private UserShortDto getUserShortDto(Long userId) {
        return userClient.getUserShortById(userId);
    }

    // Метод для получения UserShortDtos
    private Map<Long, UserShortDto> getUserShortDtos(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UserShortDto> users = userClient.getUsersShortByIds(new ArrayList<>(userIds));

        return users.stream()
                .collect(Collectors.toMap(
                        UserShortDto::getId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
    }

    private Category findCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + categoryId + " не найдена"));
    }

    private void updateEventFields(Event event, UpdateEventUserRequest updateRequest) {
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }

        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }

        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }

        if (updateRequest.getCategory() != null) {
            Category category = findCategoryById(updateRequest.getCategory());
            event.setCategory(category);
        }

        if (updateRequest.getEventDate() != null) {
            // Проверка, что дата события не раньше чем через 2 часа от текущего момента
            if (updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
            }
            event.setEventDate(updateRequest.getEventDate());
        }

        if (updateRequest.getLocation() != null) {
            Location location = locationRepository.findByLatAndLon(
                            updateRequest.getLocation().getLat(),
                            updateRequest.getLocation().getLon())
                    .orElseGet(() -> locationRepository.save(
                            eventMapper.toLocation(updateRequest.getLocation())
                    ));
            event.setLocation(location);
        }

        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }

        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }

        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
    }

    private void updateEventAdminFields(Event event, UpdateEventAdminRequest updateRequest) {
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }

        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }

        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }

        if (updateRequest.getCategory() != null) {
            Category category = findCategoryById(updateRequest.getCategory());
            event.setCategory(category);
        }

        if (updateRequest.getEventDate() != null) {
            // Если событие уже опубликовано, проверяем что дата не раньше чем через 1 час
            if (event.getState() == State.PUBLISHED && updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ValidationException("Дата события должна быть не ранее чем через 1 час от текущего момента");
            }
            event.setEventDate(updateRequest.getEventDate());
        }

        if (updateRequest.getLocation() != null) {
            Location location = locationRepository.findByLatAndLon(
                            updateRequest.getLocation().getLat(),
                            updateRequest.getLocation().getLon())
                    .orElseGet(() -> locationRepository.save(
                            eventMapper.toLocation(updateRequest.getLocation())
                    ));
            event.setLocation(location);
        }

        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }

        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }

        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
    }

    // Метод для сохранения информации о просмотре события
    public void addHit(HttpServletRequest request) {
        statsClient.hit(StatsDtoRequest.builder()
                .app(APP_NAME)
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build());
    }

    // Метод для получения статистики просмотров событий
    private Map<Long, Long> getViewStats(List<String> uris) {
        if (uris.isEmpty()) {
            return Collections.emptyMap();
        }

        List<StatsDtoResponse> statsResponses = statsClient.findStats(
                LocalDateTime.of(2000, 1, 1, 0, 0, 0),
                LocalDateTime.now().plusHours(1),
                uris,
                true);

        Map<Long, Long> viewStats = new HashMap<>();
        for (StatsDtoResponse stat : statsResponses) {
            try {
                String uri = stat.getUri();
                Long eventId = Long.parseLong(uri.substring(uri.lastIndexOf('/') + 1));
                viewStats.put(eventId, stat.getHits());
            } catch (Exception e) {
                log.error("Ошибка при обработке статистики для URI: {}", stat.getUri(), e);
            }
        }

        return viewStats;
    }
}