package ru.yandex.practicum.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.comment.CommentDto;
import ru.yandex.practicum.service.CommentService;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/admin/comments")
public class CommentsAdminController {
    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> getCommentsAdmin(@RequestParam Integer size, @RequestParam Integer from) {
        return commentService.getCommentsAdmin(size, from);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{commentId}")
    public void deleteComment(@PathVariable Long commentId) {
        commentService.deleteCommentByAdmin(commentId);
    }
}
