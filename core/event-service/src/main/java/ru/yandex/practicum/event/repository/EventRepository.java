package ru.yandex.practicum.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.dto.event.enums.State;
import ru.yandex.practicum.event.model.Event;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long initiatorId);

    boolean existsByCategoryId(Long categoryId);

    @Query("""
            SELECT e FROM Event e
            WHERE (:users IS NULL OR e.initiatorId IN :users)
            AND (:states IS NULL OR e.state IN :states)
            AND (:categories IS NULL OR e.category.id IN :categories)
            AND (CAST(:rangeStart AS timestamp) IS NULL OR e.eventDate >= :rangeStart)
            AND (CAST(:rangeEnd AS timestamp) IS NULL OR e.eventDate <= :rangeEnd)
            """)
    Page<Event> findAllByAdmin(
            @Param("users") Collection<Long> users,
            @Param("states") Collection<State> states,
            @Param("categories") Collection<Long> categories,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable);

    Set<Event> findAllByIdIn(Collection<Long> eventIds);
}