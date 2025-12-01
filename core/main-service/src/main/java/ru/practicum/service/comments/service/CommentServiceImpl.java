package ru.practicum.service.comments.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.service.comments.Comment;
import ru.practicum.service.comments.dto.CommentCreateDto;
import ru.practicum.service.comments.dto.CommentDto;
import ru.practicum.service.comments.dto.CommentUpdateDto;
import ru.practicum.service.comments.mapper.CommentMapper;
import ru.practicum.service.comments.repository.CommentRepository;
import ru.practicum.service.event.dto.EventWithCommentsDto;
import ru.practicum.service.event.repository.EventRepository;
import ru.practicum.service.event.service.EventService;
import ru.practicum.service.exception.NotFoundException;
import ru.practicum.service.exception.ValidationException;
import ru.practicum.service.user.repository.UserRepository;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventService eventService;


    public List<CommentDto> getCommentsAdmin(Integer size, Integer from) {
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Comment> commentPage = commentRepository.findAll(pageable);
        return commentPage.getContent().stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    public void deleteCommentByAdmin(Long commentId) {
        commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));
        commentRepository.deleteById(commentId);
    }

    public CommentDto createComment(CommentCreateDto commentCreateDto, Long userId, Long eventId) {
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found: " + userId));
        eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
        return CommentMapper.toDto(commentRepository.save(CommentMapper.toEntity(commentCreateDto)));
    }

    public List<CommentDto> getAllCommentsByUserId(Long userId) {
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return commentRepository.findAllByUserId(userId)
                .stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    public CommentDto updateComment(Long commentId, CommentUpdateDto commentUpdateDto, Long userId, Long eventId) {
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found: " + userId));
        eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));
        checkUserIsAuthor(comment, userId);
        comment.setText(commentUpdateDto.getText());
        return CommentMapper.toDto(commentRepository.save(comment));
    }

    public void deleteCommentByUserId(Long userId, Long commentId, Long eventId) {
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found: " + userId));
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));
        eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
        checkUserIsAuthor(comment, userId);
        if (!comment.getEvent().getId().equals(eventId)) {
            throw new ValidationException("Comment has wrong event");
        }
        commentRepository.deleteById(commentId);
    }

    public List<CommentDto> getCommentsByEventId(Long eventId, Integer from, Integer size) {
        eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("created").descending());
        Page<Comment> commentPage = commentRepository.findByEventId(eventId, pageable);
        return commentPage.getContent().stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    public EventWithCommentsDto getEventWithComments(Long eventId) {
        eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
        Pageable pageable = PageRequest.of(0, 3, Sort.by("created").descending());
        List<CommentDto> commentDtos = commentRepository.findByEventId(eventId, pageable).stream()
                .map(CommentMapper::toDto)
                .toList();
        return CommentMapper.toDto(eventService.getEvent(eventId), commentDtos);
    }

    private void checkUserIsAuthor(Comment comment, Long userId) {
        if (!comment.getUser().getId().equals(userId)) {
            throw new ValidationException("User " + userId + " is not author of comment " + comment.getId());
        }
    }
}

