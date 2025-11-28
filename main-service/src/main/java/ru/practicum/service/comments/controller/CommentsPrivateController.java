package ru.practicum.service.comments.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.service.comments.dto.CommentCreateDto;
import ru.practicum.service.comments.dto.CommentDto;
import ru.practicum.service.comments.dto.CommentUpdateDto;
import ru.practicum.service.comments.service.CommentService;

@RestController
@RequestMapping("/events/{eventId}/comments")
@RequiredArgsConstructor
public class CommentsPrivateController {
    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@PathVariable Long eventId,
                                    @RequestParam Long userId,
                                    @RequestBody CommentCreateDto commentCreateDto) {
        return commentService.createComment(commentCreateDto, userId, eventId);
    }

    @PatchMapping
    public CommentDto updateComment(@PathVariable Long eventId,
                                    @RequestParam Long userId,
                                    @RequestParam Long commentId,
                                    @RequestBody CommentUpdateDto commentUpdateDto) {
        return commentService.updateComment(commentId, commentUpdateDto, userId, eventId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommentByUserId(@PathVariable Long eventId,
                                      @RequestParam Long userId,
                                      @RequestParam Long commentId) {
        commentService.deleteCommentByUserId(userId, eventId, commentId);
    }
}
