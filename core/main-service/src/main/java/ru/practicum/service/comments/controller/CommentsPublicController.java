package ru.practicum.service.comments.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.service.comments.dto.CommentDto;
import ru.practicum.service.comments.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("events/{eventId}/comments")
@RequiredArgsConstructor
public class CommentsPublicController {
    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> getCommentsByEventId(@PathVariable Long eventId,
                                                 @RequestParam(defaultValue = "0") Integer from,
                                                 @RequestParam(defaultValue = "10") Integer size) {
        return commentService.getCommentsByEventId(eventId, from, size);
    }
}
