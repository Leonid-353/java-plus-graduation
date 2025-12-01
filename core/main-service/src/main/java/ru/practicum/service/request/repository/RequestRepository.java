package ru.practicum.service.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.service.event.Event;
import ru.practicum.service.request.Request;
import ru.practicum.service.request.RequestStatus;
import ru.practicum.service.user.model.User;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findAllByRequester(User requester);

    List<Request> findAllByEvent(Event event);

    List<Request> findAllByEventAndStatus(Event event, RequestStatus status);

    @Query("SELECT COUNT(r) FROM Request r WHERE r.event = :event AND r.status = 'CONFIRMED'")
    Long countConfirmedRequestsByEvent(@Param("event") Event event);

    Optional<Request> findByEventAndRequester(Event event, User requester);

    List<Request> findAllByIdIn(List<Long> requestIds);

    List<Request> findAllByEventAndIdIn(Event event, List<Long> requestIds);

    Boolean existsByEventAndRequesterAndStatusNot(Event event, User requester, RequestStatus status);
}