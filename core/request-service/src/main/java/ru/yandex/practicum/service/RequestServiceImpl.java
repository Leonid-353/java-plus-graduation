package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.CollectorClient;
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
import ru.yandex.practicum.grpc.stats.action.ActionTypeProto;
import ru.yandex.practicum.mapper.RequestMapper;
import ru.yandex.practicum.model.Request;
import ru.yandex.practicum.repository.RequestRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    final CollectorClient userActionClient;
    private final RequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final RequestMapper requestMapper;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        UserDto user = userClient.getUserById(userId);

        return getUserRequestsInTransaction(user.getId());
    }

    @Override
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        UserDto user = userClient.getUserById(userId);
        EventFullDto event = eventClient.getEventByIdFeign(eventId);

        validateBeforeTransaction(user, event, userId);

        boolean autoConfirm = !event.getRequestModeration() || event.getParticipantLimit() == 0;

        // Если для события отключена пре-модерация или лимит участников равен 0,
        // то запрос автоматически подтверждается
        if (autoConfirm) {
            eventClient.updateEventForRequests(eventId, event.getConfirmedRequests() + 1);
        }

        ParticipationRequestDto savedRequest = saveRequestInTransaction(user.getId(), eventId, autoConfirm);

        userActionClient.collectUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER, Instant.now());

        return savedRequest;
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        UserDto user = userClient.getUserById(userId);

        Request request = validateAndGetRequest(requestId, userId);

        // Если запрос был подтвержден, уменьшаем счетчик подтвержденных заявок в событии
        if (request.getStatus() == RequestStatus.CONFIRMED) {
            EventFullDto event = eventClient.getEventByIdFeign(request.getEventId());
            eventClient.updateEventForRequests(event.getId(), event.getConfirmedRequests() - 1);
        }

        return cancelRequestInTransaction(request);
    }

    @Override
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        // Проверка: пользователь существует
        UserDto user = userClient.getUserById(userId);

        // Проверка: событие существует и принадлежит пользователю
        EventFullDto event = eventClient.getEventByIdFeign(eventId);

        if (!Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ConflictException("Событие с id=" + eventId + " не принадлежит пользователю с id=" + userId);
        }

        return getEventParticipantsInTransaction(event.getId());
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
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

        EventRequestStatusUpdateResult result = updateRequestStatusInTransaction(event, updateRequest);

        eventClient.updateEventForRequests(event.getId(), event.getConfirmedRequests());

        return result;
    }

    // Transactional methods
    // Get user requests
    @Transactional(readOnly = true)
    private List<ParticipationRequestDto> getUserRequestsInTransaction(Long requesterId) {
        List<Request> requests = requestRepository.findAllByRequesterId(requesterId);

        return requestMapper.toParticipationRequestDtoList(requests);
    }

    // Create request
    @Transactional
    private ParticipationRequestDto saveRequestInTransaction(Long requesterId, Long eventId, boolean autoConfirm) {
        // Проверка: нельзя добавить повторный запрос
        if (requestRepository.existsByEventIdAndRequesterIdAndStatusNot(
                eventId, requesterId, RequestStatus.CANCELED)) {
            throw new ConflictException("Запрос на участие в событии с id=" + eventId + " уже существует");
        }

        Request request = Request.builder()
                .eventId(eventId)
                .requesterId(requesterId)
                .status(autoConfirm ? RequestStatus.CONFIRMED : RequestStatus.PENDING)
                .created(LocalDateTime.now())
                .build();

        Request savedRequest = requestRepository.save(request);
        requestRepository.flush();
        return requestMapper.toParticipationRequestDto(savedRequest);
    }

    // Cancel request
    @Transactional
    private ParticipationRequestDto cancelRequestInTransaction(Request request) {
        request.setStatus(RequestStatus.CANCELED);
        request = requestRepository.save(request);
        requestRepository.flush();
        return requestMapper.toParticipationRequestDto(request);
    }

    // Get event participants
    @Transactional(readOnly = true)
    private List<ParticipationRequestDto> getEventParticipantsInTransaction(Long eventId) {
        List<Request> requests = requestRepository.findAllByEventId(eventId);

        return requestMapper.toParticipationRequestDtoList(requests);
    }

    // Update request status
    @Transactional
    private EventRequestStatusUpdateResult updateRequestStatusInTransaction(
            EventFullDto event, EventRequestStatusUpdateRequest updateRequest) {
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
        Long currentConfirmed = event.getConfirmedRequests();

        // Обработка запросов
        for (Request request : requests) {
            if (updateRequest.getStatus() == Status.CONFIRMED) {
                // Проверка: достигнут ли лимит заявок
                if (currentConfirmed >= event.getParticipantLimit()) {
                    request.setStatus(RequestStatus.REJECTED);
                    rejectedRequests.add(request);
                } else {
                    request.setStatus(RequestStatus.CONFIRMED);
                    currentConfirmed++;
                    confirmedRequests.add(request);
                }
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(request);
            }
        }

        // Сохранение изменений
        requestRepository.saveAll(requests);
        requestRepository.flush();
        // Передаем изменения в основной метод для передачи в event-service
        event.setConfirmedRequests(currentConfirmed);

        // Если при подтверждении заявок лимит исчерпан, отклоняем все остальные заявки в ожидании
        if (currentConfirmed >= event.getParticipantLimit()) {
            List<Request> pendingRequests = requestRepository.findAllByEventIdAndStatus(
                    event.getId(), RequestStatus.PENDING);
            pendingRequests.forEach(r -> r.setStatus(RequestStatus.REJECTED));
            requestRepository.saveAll(pendingRequests);
            requestRepository.flush();
            rejectedRequests.addAll(pendingRequests);
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(requestMapper.toParticipationRequestDtoList(confirmedRequests))
                .rejectedRequests(requestMapper.toParticipationRequestDtoList(rejectedRequests))
                .build();
    }

    // Auxiliary methods
    // Create request
    private void validateBeforeTransaction(UserDto user, EventFullDto event, Long userId) {
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
            throw new ConflictException("Достигнут лимит запросов на участие в событии с id=" + event.getId());
        }
    }

    // Cancel request
    private Request validateAndGetRequest(Long requestId, Long userId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не найден"));

        // Проверка: запрос должен принадлежать пользователю
        if (!Objects.equals(request.getRequesterId(), userId)) {
            throw new ConflictException("Запрос с id=" + requestId + " не принадлежит пользователю с id=" + userId);
        }

        return request;
    }
}