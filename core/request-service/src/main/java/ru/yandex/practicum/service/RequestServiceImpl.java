package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.event.EventFullDto;
import ru.yandex.practicum.dto.event.enums.State;
import ru.yandex.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.yandex.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.yandex.practicum.dto.request.ParticipationRequestDto;
import ru.yandex.practicum.dto.request.enums.RequestStatus;
import ru.yandex.practicum.dto.request.enums.Status;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.exception.ConflictException;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.feign.EventClient;
import ru.yandex.practicum.feign.UserClient;
import ru.yandex.practicum.mapper.RequestMapper;
import ru.yandex.practicum.model.Request;
import ru.yandex.practicum.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final RequestMapper requestMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        UserDto user = userClient.getUserById(userId);

        List<Request> requests = requestRepository.findAllByRequesterId(user.getId());
        return requestMapper.toParticipationRequestDtoList(requests);
    }

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        UserDto user = userClient.getUserById(userId);

        EventFullDto event = eventClient.getEventByIdFeign(eventId);

        // Проверка: нельзя добавить повторный запрос
        if (requestRepository.existsByEventIdAndRequesterIdAndStatusNot(event.getId(), user.getId(), RequestStatus.CANCELED)) {
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
                .eventId(event.getId())
                .requesterId(user.getId())
                .created(LocalDateTime.now())
                .build();

        // Если для события отключена пре-модерация или лимит участников равен 0,
        // то запрос автоматически подтверждается
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);

            // Увеличиваем счетчик подтвержденных заявок в событии
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventClient.updateEventForRequests(event.getId(), event);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        request = requestRepository.save(request);
        return requestMapper.toParticipationRequestDto(request);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        UserDto user = userClient.getUserById(userId);

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не найден"));

        // Проверка: запрос должен принадлежать пользователю
        if (!Objects.equals(request.getRequesterId(), userId)) {
            throw new ConflictException("Запрос с id=" + requestId + " не принадлежит пользователю с id=" + userId);
        }

        // Если запрос был подтвержден, уменьшаем счетчик подтвержденных заявок в событии
        if (request.getStatus() == RequestStatus.CONFIRMED) {
            EventFullDto event = eventClient.getEventByIdFeign(request.getEventId());
            event.setConfirmedRequests(event.getConfirmedRequests() - 1);
            eventClient.updateEventForRequests(event.getId(), event);
        }

        request.setStatus(RequestStatus.CANCELED);
        request = requestRepository.save(request);

        return requestMapper.toParticipationRequestDto(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        // Проверка: пользователь существует
        UserDto user = userClient.getUserById(userId);

        // Проверка: событие существует и принадлежит пользователю
        EventFullDto event = eventClient.getEventByIdFeign(eventId);

        if (!Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ConflictException("Событие с id=" + eventId + " не принадлежит пользователю с id=" + userId);
        }

        List<Request> requests = requestRepository.findAllByEventId(event.getId());
        return requestMapper.toParticipationRequestDtoList(requests);
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest) {
        // Проверка: пользователь существует
        UserDto user = userClient.getUserById(userId);

        // Проверка: событие существует и принадлежит пользователю
        EventFullDto event = eventClient.getEventByIdFeign(eventId);

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

        List<Request> requests = requestRepository.findAllByEventIdAndIdIn(event.getId(), updateRequest.getRequestIds());

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
        eventClient.updateEventForRequests(event.getId(), event);

        // Если при подтверждении заявок лимит исчерпан, отклоняем все остальные заявки в ожидании
        if (event.getConfirmedRequests() >= event.getParticipantLimit()) {
            List<Request> pendingRequests = requestRepository.findAllByEventIdAndStatus(event.getId(), RequestStatus.PENDING);
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