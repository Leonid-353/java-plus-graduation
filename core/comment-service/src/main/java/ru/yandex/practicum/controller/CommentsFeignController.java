package ru.yandex.practicum.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.dto.comment.CommentDto;
import ru.yandex.practicum.feign.CommentClient;
import ru.yandex.practicum.service.CommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/comments/feign") // "events/comments/{eventId}"
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentsFeignController implements CommentClient {
    final CommentService commentService;

    @Override
    public List<CommentDto> getEventWithComments(Long eventId, Integer page, Integer size) {
        return commentService.getEventWithComments(eventId, page, size);
    }
}
