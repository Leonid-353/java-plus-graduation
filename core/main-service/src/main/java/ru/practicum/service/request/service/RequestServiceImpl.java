package ru.practicum.service.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.service.event.Event;
import ru.practicum.service.event.State;
import ru.practicum.service.event.repository.EventRepository;
import ru.practicum.service.exception.ConflictException;
import ru.practicum.service.exception.NotFoundException;
import ru.practicum.service.request.Request;
import ru.practicum.service.request.RequestStatus;
import ru.practicum.service.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.service.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.service.request.dto.ParticipationRequestDto;
import ru.practicum.service.request.dto.Status;
import ru.practicum.service.request.mapper.RequestMapper;
import ru.practicum.service.request.repository.RequestRepository;
import ru.practicum.service.user.model.User;
import ru.practicum.service.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestMapper requestMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        List<Request> requests = requestRepository.findAllByRequester(user);
        return requestMapper.toParticipationRequestDtoList(requests);
    }

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        // Проверка: нельзя добавить повторный запрос
        if (requestRepository.existsByEventAndRequesterAndStatusNot(event, user, RequestStatus.CANCELED)) {
            throw new ConflictException("Запрос на участие в событии с id=" + eventId + " уже существует");
        }

        // Проверка: инициатор события не может добавить запрос на участие в своём событии
        if (Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии");
        }

        // Проверка: нельзя участвовать в неопубликованном событии
        if (event.getState() != State.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        // Проверка: если у события достигнут лимит запросов на участие
        if (event.getParticipantLimit() > 0 && event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит запросов на участие в событии с id=" + eventId);
        }

        Request request = Request.builder()
                .event(event)
                .requester(user)
                .created(LocalDateTime.now())
                .build();

        // Если для события отключена пре-модерация или лимит участников равен 0,
        // то запрос автоматически подтверждается
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);

            // Увеличиваем счетчик подтвержденных заявок в событии
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        request = requestRepository.save(request);
        return requestMapper.toParticipationRequestDto(request);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не найден"));

        // Проверка: запрос должен принадлежать пользователю
        if (!Objects.equals(request.getRequester().getId(), userId)) {
            throw new ConflictException("Запрос с id=" + requestId + " не принадлежит пользователю с id=" + userId);
        }

        // Если запрос был подтвержден, уменьшаем счетчик подтвержденных заявок в событии
        if (request.getStatus() == RequestStatus.CONFIRMED) {
            Event event = request.getEvent();
            event.setConfirmedRequests(event.getConfirmedRequests() - 1);
            eventRepository.save(event);
        }

        request.setStatus(RequestStatus.CANCELED);
        request = requestRepository.save(request);

        return requestMapper.toParticipationRequestDto(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        // Проверка: пользователь существует
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        // Проверка: событие существует и принадлежит пользователю
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (!Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ConflictException("Событие с id=" + eventId + " не принадлежит пользователю с id=" + userId);
        }

        List<Request> requests = requestRepository.findAllByEvent(event);
        return requestMapper.toParticipationRequestDtoList(requests);
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest) {
        // Проверка: пользователь существует
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        // Проверка: событие существует и принадлежит пользователю
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (!Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ConflictException("Событие с id=" + eventId + " не принадлежит пользователю с id=" + userId);
        }

        // Если для события лимит заявок равен 0 или отключена пре-модерация заявок, то подтверждение не требуется
        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            throw new ConflictException("Для события подтверждение заявок не требуется");
        }

        // Проверка: нельзя подтвердить заявку, если уже достигнут лимит по заявкам на данное событие
        if (updateRequest.getStatus() == Status.CONFIRMED &&
                event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит по заявкам на данное событие");
        }

        List<Request> requests = requestRepository.findAllByEventAndIdIn(event, updateRequest.getRequestIds());

        // Проверка: все запросы должны существовать
        if (requests.size() != updateRequest.getRequestIds().size()) {
            throw new ConflictException("Некоторые запросы не найдены");
        }

        // Проверка: статус можно изменить только у заявок, находящихся в состоянии ожидания
        for (Request request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Статус можно изменить только у заявок, находящихся в состоянии ожидания");
            }
        }

        List<Request> confirmedRequests = new ArrayList<>();
        List<Request> rejectedRequests = new ArrayList<>();

        // Обработка запросов
        for (Request request : requests) {
            if (updateRequest.getStatus() == Status.CONFIRMED) {
                // Проверка: достигнут ли лимит заявок
                if (event.getConfirmedRequests() >= event.getParticipantLimit()) {
                    request.setStatus(RequestStatus.REJECTED);
                    rejectedRequests.add(request);
                } else {
                    request.setStatus(RequestStatus.CONFIRMED);
                    event.setConfirmedRequests(event.getConfirmedRequests() + 1);
                    confirmedRequests.add(request);
                }
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(request);
            }
        }

        // Сохранение изменений
        requestRepository.saveAll(requests);
        eventRepository.save(event);

        // Если при подтверждении заявок лимит исчерпан, отклоняем все остальные заявки в ожидании
        if (event.getConfirmedRequests() >= event.getParticipantLimit()) {
            List<Request> pendingRequests = requestRepository.findAllByEventAndStatus(event, RequestStatus.PENDING);
            pendingRequests.forEach(r -> r.setStatus(RequestStatus.REJECTED));
            requestRepository.saveAll(pendingRequests);
            rejectedRequests.addAll(pendingRequests);
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(requestMapper.toParticipationRequestDtoList(confirmedRequests))
                .rejectedRequests(requestMapper.toParticipationRequestDtoList(rejectedRequests))
                .build();
    }
}