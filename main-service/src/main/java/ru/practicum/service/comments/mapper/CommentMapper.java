package ru.practicum.service.comments.mapper;


import lombok.experimental.UtilityClass;
import ru.practicum.service.comments.Comment;
import ru.practicum.service.comments.dto.CommentCreateDto;
import ru.practicum.service.comments.dto.CommentDto;
import ru.practicum.service.event.dto.EventFullDto;
import ru.practicum.service.event.dto.EventWithCommentsDto;

import java.util.List;


@UtilityClass
public class CommentMapper {

    public static Comment toEntity(CommentCreateDto commentCreateDto) {
        return Comment.builder()
                .text(commentCreateDto.getText())
                .created(commentCreateDto.getCreated())
                .build();
    }

    public static CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .eventId(comment.getEvent().getId())
                .authorId(comment.getUser().getId())
                .created(comment.getCreated())
                .build();
    }

    public static EventWithCommentsDto toDto(EventFullDto event, List<CommentDto> comments) {
        return EventWithCommentsDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .description(event.getDescription())
                .category(event.getCategory())
                .eventDate(event.getEventDate())
                .location(event.getLocation())
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .confirmedRequests(event.getConfirmedRequests())
                .createdOn(event.getCreatedOn())
                .initiator(event.getInitiator())
                .state(event.getState())
                .publishedOn(event.getPublishedOn())
                .views(event.getViews())
                .comments(comments)
                .build();
    }
}