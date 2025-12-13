package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.dto.request.enums.RequestStatus;
import ru.yandex.practicum.model.Request;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findAllByRequesterId(Long requesterId);

    List<Request> findAllByEventId(Long eventId);

    List<Request> findAllByEventIdAndStatus(Long eventId, RequestStatus requestStatus);

    List<Request> findAllByEventIdAndIdIn(Long eventId, List<Long> requestIds);

    Boolean existsByEventIdAndRequesterIdAndStatusNot(Long eventId, Long requesterId, RequestStatus status);
}