package ru.practicum.service.event.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.service.category.model.Category;
import ru.practicum.service.category.repository.CategoryRepository;
import ru.practicum.service.event.Event;
import ru.practicum.service.event.Location;
import ru.practicum.service.event.QEvent;
import ru.practicum.service.event.State;
import ru.practicum.service.event.dto.*;
import ru.practicum.service.event.mapper.EventMapper;
import ru.practicum.service.event.repository.EventRepository;
import ru.practicum.service.event.repository.LocationRepository;
import ru.practicum.service.exception.ForbiddenException;
import ru.practicum.service.exception.NotFoundException;
import ru.practicum.service.exception.ValidationException;
import ru.practicum.service.stats.GetStatsRequest;
import ru.practicum.service.stats.StatsDtoRequest;
import ru.practicum.service.stats.StatsDtoResponse;
import ru.practicum.service.stats.client.StatsClient;
import ru.practicum.service.user.model.User;
import ru.practicum.service.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {
    private static final String APP_NAME = "explore-with-me";
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;
    private final EntityManager entityManager;


    @Override
    @Transactional(readOnly = true)
    public Collection<EventShortDto> getEvents(String text, List<Long> categories, Boolean paid,
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

        PageImpl<Event> page = new PageImpl<>(events, pageable, totalCount);

        List<String> uris = page.stream()
                .map(e -> "/events/" + e.getId())
                .collect(Collectors.toList());

        Map<Long, Long> viewStats = getViewStats(uris);

        return page.stream()
                .map(e -> eventMapper.toEventShortDto(e, viewStats.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto getEvent(Long id) {
        Event event = findEventById(id);

        // Проверяем, что событие опубликовано
        if (event.getState() != State.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + id + " не опубликовано");
        }

        // Получаем статистику просмотров
        List<String> uris = List.of("/events/" + id);
        Map<Long, Long> viewStats = getViewStats(uris);

        return eventMapper.toEventFullDto(event, viewStats.getOrDefault(id, 0L));
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        User user = findUserById(userId);
        Pageable pageable = PageRequest.of(from / size, size);
        var events = eventRepository.findAllByInitiator(user, pageable);

        // Получаем статистику просмотров
        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        Map<Long, Long> viewStats = getViewStats(uris);

        return events.stream()
                .map(event -> eventMapper.toEventShortDto(event, viewStats.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        User initiator = findUserById(userId);
        Category category = findCategoryById(newEventDto.getCategory());
        LocalDateTime createdOn = LocalDateTime.now();

        // Проверка, что дата события не раньше чем через 2 часа от текущего момента
        if (newEventDto.getEventDate().isBefore(createdOn.plusHours(2))) {
            throw new ValidationException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        // Поиск существующей локации или создание новой
        Location location = locationRepository.findByLatAndLon(
                        newEventDto.getLocation().getLat(),
                        newEventDto.getLocation().getLon())
                .orElseGet(() -> locationRepository.save(
                        eventMapper.toLocation(newEventDto.getLocation())
                ));

        Event event = eventMapper.toEvent(newEventDto, category, location, initiator, createdOn);
        event = eventRepository.save(event);

        return eventMapper.toEventFullDto(event, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        User user = findUserById(userId);
        Event event = eventRepository.findByIdAndInitiator(eventId, user)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        // Получаем статистику просмотров
        List<String> uris = List.of("/events/" + eventId);
        Map<Long, Long> viewStats = getViewStats(uris);

        return eventMapper.toEventFullDto(event, viewStats.getOrDefault(eventId, 0L));
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        User user = findUserById(userId);
        Event event = eventRepository.findByIdAndInitiator(eventId, user)
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

        event = eventRepository.save(event);

        // Получаем статистику просмотров
        List<String> uris = List.of("/events/" + eventId);
        Map<Long, Long> viewStats = getViewStats(uris);

        return eventMapper.toEventFullDto(event, viewStats.getOrDefault(eventId, 0L));
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<EventFullDto> getEventsAdmin(List<Long> users, List<String> states, List<Long> categories,
                                                   LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                   Integer from, Integer size) {
        Collection<State> statesList = null;
        if (states != null) {
            statesList = states.stream()
                    .map(State::valueOf)
                    .collect(Collectors.toList());
        }

        Pageable pageable = PageRequest.of(from / size, size);
        var events = eventRepository.findAllByAdmin(users, statesList, categories, rangeStart, rangeEnd, pageable);

        // Получаем статистику просмотров
        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        Map<Long, Long> viewStats = getViewStats(uris);

        return events.stream()
                .map(event -> eventMapper.toEventFullDto(event, viewStats.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {
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

        event = eventRepository.save(event);

        // Получаем статистику просмотров
        List<String> uris = List.of("/events/" + eventId);
        Map<Long, Long> viewStats = getViewStats(uris);

        return eventMapper.toEventFullDto(event, viewStats.getOrDefault(eventId, 0L));
    }

    // Вспомогательные методы
    private Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
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
        statsClient.saveHit(StatsDtoRequest.builder()
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

        GetStatsRequest statsRequest = GetStatsRequest.of(
                LocalDateTime.of(2000, 1, 1, 0, 0, 0),  // Начальная дата достаточно давняя
                LocalDateTime.now().plusHours(1),       // Текущее время + 1 час для надежности
                uris,
                true
        );

        List<StatsDtoResponse> statsResponses = statsClient.getStats(statsRequest);

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