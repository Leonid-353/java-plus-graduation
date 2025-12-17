package ru.yandex.practicum.service;

import ru.yandex.practicum.dto.comment.CommentCreateDto;
import ru.yandex.practicum.dto.comment.CommentDto;
import ru.yandex.practicum.dto.comment.CommentUpdateDto;

import java.util.List;

public interface CommentService {
    List<CommentDto> getCommentsAdmin(Integer size, Integer from);

    void deleteCommentByAdmin(Long commentId);

    CommentDto createComment(CommentCreateDto commentCreateDto, Long userId, Long eventId);

    List<CommentDto> getAllCommentsByUserId(Long userId);

    CommentDto updateComment(Long commentId, CommentUpdateDto commentUpdateDto, Long userId, Long eventId);

    void deleteCommentByUserId(Long userId, Long eventId, Long commentId);

    List<CommentDto> getCommentsByEventId(Long eventId, Integer from, Integer size);

    List<CommentDto> getEventWithComments(Long eventId, Integer page, Integer size);
}
