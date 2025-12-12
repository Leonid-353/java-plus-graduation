package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.dto.comment.CommentCreateDto;
import ru.yandex.practicum.dto.comment.CommentDto;
import ru.yandex.practicum.dto.comment.CommentUpdateDto;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.exception.ValidationException;
import ru.yandex.practicum.feign.EventClient;
import ru.yandex.practicum.feign.UserClient;
import ru.yandex.practicum.mapper.CommentMapper;
import ru.yandex.practicum.model.Comment;
import ru.yandex.practicum.repository.CommentRepository;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final CommentMapper commentMapper;


    public List<CommentDto> getCommentsAdmin(Integer size, Integer from) {
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Comment> commentPage = commentRepository.findAll(pageable);
        return commentPage.getContent().stream()
                .map(commentMapper::toDto)
                .toList();
    }

    public void deleteCommentByAdmin(Long commentId) {
        commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));
        commentRepository.deleteById(commentId);
    }

    public CommentDto createComment(CommentCreateDto commentCreateDto, Long userId, Long eventId) {
        userClient.getUserById(userId);
        eventClient.getEventByIdFeign(eventId);
        return commentMapper.toDto(commentRepository.save(commentMapper.toEntity(commentCreateDto)));
    }

    public List<CommentDto> getAllCommentsByUserId(Long userId) {
        userClient.getUserById(userId);
        return commentRepository.findAllByUserId(userId)
                .stream()
                .map(commentMapper::toDto)
                .toList();
    }

    public CommentDto updateComment(Long commentId, CommentUpdateDto commentUpdateDto, Long userId, Long eventId) {
        userClient.getUserById(userId);
        eventClient.getEventByIdFeign(eventId);
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));
        checkUserIsAuthor(comment, userId);
        comment.setText(commentUpdateDto.getText());
        return commentMapper.toDto(commentRepository.save(comment));
    }

    public void deleteCommentByUserId(Long userId, Long commentId, Long eventId) {
        userClient.getUserById(userId);
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));
        eventClient.getEventByIdFeign(eventId);
        checkUserIsAuthor(comment, userId);
        if (!comment.getEventId().equals(eventId)) {
            throw new ValidationException("Comment has wrong event");
        }
        commentRepository.deleteById(commentId);
    }

    public List<CommentDto> getCommentsByEventId(Long eventId, Integer from, Integer size) {
        eventClient.getEventByIdFeign(eventId);
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("created").descending());
        Page<Comment> commentPage = commentRepository.findByEventId(eventId, pageable);
        return commentPage.getContent().stream()
                .map(commentMapper::toDto)
                .toList();
    }

    public List<CommentDto> getEventWithComments(Long eventId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("created").descending());

        return commentRepository.findByEventId(eventId, pageable).stream()
                .map(commentMapper::toDto)
                .toList();
    }

    private void checkUserIsAuthor(Comment comment, Long userId) {
        if (!comment.getAuthorId().equals(userId)) {
            throw new ValidationException("User " + userId + " is not author of comment " + comment.getId());
        }
    }
}

