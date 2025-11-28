package ru.practicum.service.comments.service;


import ru.practicum.service.comments.dto.CommentCreateDto;
import ru.practicum.service.comments.dto.CommentDto;
import ru.practicum.service.comments.dto.CommentUpdateDto;
import ru.practicum.service.event.dto.EventWithCommentsDto;

import java.util.List;

public interface CommentService {
    List<CommentDto> getCommentsAdmin(Integer size, Integer from);

    void deleteCommentByAdmin(Long commentId);

    CommentDto createComment(CommentCreateDto commentCreateDto, Long userId, Long eventId);

    List<CommentDto> getAllCommentsByUserId(Long userId);

    CommentDto updateComment(Long commentId, CommentUpdateDto commentUpdateDto, Long userId, Long eventId);

    void deleteCommentByUserId(Long userId, Long eventId, Long commentId);

    List<CommentDto> getCommentsByEventId(Long eventId, Integer from, Integer size);

    EventWithCommentsDto getEventWithComments(Long eventId);
}
