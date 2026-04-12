package ru.yandex.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.model.UserAction;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    @Query("""
            SELECT DISTINCT ua.eventId FROM UserAction ua
            WHERE ua.userId = :userId
            """)
    Set<Long> findDistinctEventIdsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT ua FROM UserAction ua
            WHERE ua.userId = :userId
            ORDER BY ua.timestamp DESC
            """)
    List<UserAction> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    List<UserAction> findAllByEventId(Long eventId);

    Optional<UserAction> findByUserIdAndEventId(Long userId, Long eventId);
}
