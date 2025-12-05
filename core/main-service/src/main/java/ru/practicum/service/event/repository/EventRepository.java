package ru.practicum.service.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.service.event.Event;
import ru.practicum.service.event.State;
import ru.practicum.service.user.model.User;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findAllByInitiator(User initiator, Pageable pageable);

    Optional<Event> findByIdAndInitiator(Long eventId, User initiator);

    boolean existsByCategoryId(Long categoryId);

    @Query("SELECT e FROM Event e " +
            "WHERE (:users IS NULL OR e.initiator.id IN :users) " +
            "AND (:states IS NULL OR e.state IN :states) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (CAST(:rangeStart AS timestamp) IS NULL OR e.eventDate >= :rangeStart) " +
            "AND (CAST(:rangeEnd AS timestamp) IS NULL OR e.eventDate <= :rangeEnd)")
    Page<Event> findAllByAdmin(
            @Param("users") Collection<Long> users,
            @Param("states") Collection<State> states,
            @Param("categories") Collection<Long> categories,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable);

    Set<Event> findAllByIdIn(Collection<Long> eventIds);

    @Query("SELECT DISTINCT e FROM Event e " +
            "JOIN FETCH e.category " +
            "JOIN FETCH e.initiator " +
            "JOIN FETCH e.location " +
            "WHERE e.id IN :eventIds")
    List<Event> findAllByIdWithDetails(@Param("eventIds") Set<Long> eventIds);
}