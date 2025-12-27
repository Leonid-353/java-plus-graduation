package ru.yandex.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.model.EventSimilarity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    @Query("""
            SELECT es FROM EventSimilarity es
            WHERE es.eventA IN :eventIds
               OR es.eventB IN :eventIds
            ORDER BY es.score DESC
            """)
    List<EventSimilarity> findTopSimilarityEvents(@Param("eventIds") Set<Long> eventIds, Pageable pageable);

    @Query("""
            SELECT es FROM EventSimilarity es
            WHERE es.eventA = :eventId
               OR es.eventB = :eventId
            ORDER BY es.score DESC
            """)
    List<EventSimilarity> findTopByEventAOrEventB(@Param("eventId") Long eventId, Pageable pageable);

    @Query("""
            SELECT es FROM EventSimilarity es
            WHERE (es.eventA = :eventId1 AND es.eventB = :eventId2)
               OR (es.eventA = :eventId2 AND es.eventB = :eventId1)
            """)
    Optional<EventSimilarity> findByEventPair(@Param("eventId1") Long eventId1, @Param("eventId2") Long eventId2);
}
