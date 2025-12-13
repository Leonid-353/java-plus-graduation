package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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


    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsAdmin(Integer size, Integer from) {
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Comment> commentPage = commentRepository.findAll(pageable);
        return commentPage.getContent().stream()
                .map(commentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));
        commentRepository.deleteById(commentId);
    }

    @Override
    public CommentDto createComment(CommentCreateDto commentCreateDto, Long userId, Long eventId) {
        validateUserAndEventExist(userId, eventId);

        return createCommentInTransaction(commentCreateDto, userId, eventId);
    }

    @Override
    public List<CommentDto> getAllCommentsByUserId(Long userId) {
        userClient.getUserById(userId);

        return getAllCommentsByUserIdInTransaction(userId);
    }

    @Override
    public CommentDto updateComment(Long commentId, CommentUpdateDto commentUpdateDto, Long userId, Long eventId) {
        validateUserAndEventExist(userId, eventId);

        return updateCommentInTransaction(commentId, commentUpdateDto, userId);
    }

    @Override
    public void deleteCommentByUserId(Long userId, Long commentId, Long eventId) {
        validateUserAndEventExist(userId, eventId);

        deleteCommentInTransaction(userId, commentId, eventId);
    }

    @Override
    public List<CommentDto> getCommentsByEventId(Long eventId, Integer from, Integer size) {
        eventClient.getEventByIdFeign(eventId);

        return getCommentsByEventIdInTransaction(eventId, from, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getEventWithComments(Long eventId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("created").descending());

        return commentRepository.findByEventId(eventId, pageable).stream()
                .map(commentMapper::toDto)
                .toList();
    }

    // Transactional methods
    // Create comment
    @Transactional
    private CommentDto createCommentInTransaction(CommentCreateDto commentCreateDto, Long userId, Long eventId) {
        Comment comment = commentMapper.toEntity(commentCreateDto);

        return commentMapper.toDto(commentRepository.save(comment));
    }

    // Get all comments by user id
    @Transactional(readOnly = true)
    private List<CommentDto> getAllCommentsByUserIdInTransaction(Long userId) {
        return commentRepository.findAllByUserId(userId)
                .stream()
                .map(commentMapper::toDto)
                .toList();
    }

    // Update comment
    @Transactional
    private CommentDto updateCommentInTransaction(Long commentId, CommentUpdateDto commentUpdateDto, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));

        checkUserIsAuthor(comment, userId);
        comment.setText(commentUpdateDto.getText());
        return commentMapper.toDto(commentRepository.save(comment));
    }

    // Delete comment
    @Transactional
    private void deleteCommentInTransaction(Long userId, Long commentId, Long eventId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));

        checkUserIsAuthor(comment, userId);

        if (!comment.getEventId().equals(eventId)) {
            throw new ValidationException("Comment has wrong event");
        }

        commentRepository.deleteById(commentId);
    }

    // Get comments by event id
    @Transactional(readOnly = true)
    private List<CommentDto> getCommentsByEventIdInTransaction(Long eventId, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("created").descending());
        Page<Comment> commentPage = commentRepository.findByEventId(eventId, pageable);

        return commentPage.getContent().stream()
                .map(commentMapper::toDto)
                .toList();
    }

    // Auxiliary methods
    private void checkUserIsAuthor(Comment comment, Long userId) {
        if (!comment.getAuthorId().equals(userId)) {
            throw new ValidationException("User " + userId + " is not author of comment " + comment.getId());
        }
    }

    private void validateUserAndEventExist(Long userId, Long eventId) {
        try {
            userClient.getUserById(userId);
            eventClient.getEventByIdFeign(eventId);
        } catch (Exception e) {
            throw new NotFoundException("Пользователь или событие не найдены");
        }
    }
}

