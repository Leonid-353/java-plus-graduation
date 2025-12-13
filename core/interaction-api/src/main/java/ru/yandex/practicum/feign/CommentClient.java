package ru.yandex.practicum.feign;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.dto.comment.CommentDto;

import java.util.List;

@FeignClient(name = "comment-service", path = "/comments/feign")
public interface CommentClient {

    @GetMapping("/{eventId}")
    List<CommentDto> getEventWithComments(@PathVariable Long eventId,
                                          @RequestParam(defaultValue = "0") Integer page,
                                          @RequestParam(defaultValue = "3") Integer size) throws FeignException;
}
