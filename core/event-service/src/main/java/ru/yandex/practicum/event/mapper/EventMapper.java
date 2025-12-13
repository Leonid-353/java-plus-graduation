package ru.yandex.practicum.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.dto.comment.CommentDto;
import ru.yandex.practicum.dto.event.*;
import ru.yandex.practicum.event.model.Event;
import ru.yandex.practicum.event.model.Location;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category.id", source = "category")
    @Mapping(target = "createdOn", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "state", constant = "PENDING")
    @Mapping(target = "confirmedRequests", constant = "0L")
    @Mapping(target = "publishedOn", ignore = true)
    Event toEvent(NewEventDto newEventDto);

    @Mapping(target = "views", source = "views")
    EventFullDto toEventFullDto(Event event, Long views);

    @Mapping(target = "views", source = "views")
    @Mapping(target = "initiator", ignore = true)
    EventShortDto toEventShortDto(Event event, Long views);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "title", source = "event.title")
    @Mapping(target = "annotation", source = "event.annotation")
    @Mapping(target = "description", source = "event.description")
    @Mapping(target = "category", source = "event.category")
    @Mapping(target = "eventDate", source = "event.eventDate")
    @Mapping(target = "location", source = "event.location")
    @Mapping(target = "paid", source = "event.paid")
    @Mapping(target = "participantLimit", source = "event.participantLimit")
    @Mapping(target = "requestModeration", source = "event.requestModeration")
    @Mapping(target = "confirmedRequests", source = "event.confirmedRequests")
    @Mapping(target = "createdOn", source = "event.createdOn")
    @Mapping(target = "initiator", source = "event.initiator")
    @Mapping(target = "state", source = "event.state")
    @Mapping(target = "publishedOn", source = "event.publishedOn")
    @Mapping(target = "views", source = "event.views")
    @Mapping(target = "comments", source = "comments")
    EventWithCommentsDto toEventWithCommentsDto(EventFullDto event, List<CommentDto> comments);

    @Mapping(target = "id", ignore = true)
    Location toLocation(LocationDto locationDto);
}