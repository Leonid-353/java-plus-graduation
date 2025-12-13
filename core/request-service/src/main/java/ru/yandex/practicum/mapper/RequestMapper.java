package ru.yandex.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.dto.request.ParticipationRequestDto;
import ru.yandex.practicum.model.Request;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    @Mapping(target = "event", source = "eventId")
    @Mapping(target = "requester", source = "requesterId")
    ParticipationRequestDto toParticipationRequestDto(Request request);

    List<ParticipationRequestDto> toParticipationRequestDtoList(List<Request> requests);
}