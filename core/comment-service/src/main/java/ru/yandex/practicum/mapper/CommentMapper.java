package ru.yandex.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.dto.comment.CommentCreateDto;
import ru.yandex.practicum.dto.comment.CommentDto;
import ru.yandex.practicum.model.Comment;


@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "eventId", ignore = true)
    Comment toEntity(CommentCreateDto commentCreateDto);

    CommentDto toDto(Comment comment);
}