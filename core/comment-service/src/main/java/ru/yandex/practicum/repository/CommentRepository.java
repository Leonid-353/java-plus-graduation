package ru.yandex.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.model.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByEventId(Long eventId, Pageable pageRequest);

    @Query("SELECT c FROM Comment c WHERE c.authorId = :userId")
    List<Comment> findAllByUserId(@Param("userId") Long userId);
}
