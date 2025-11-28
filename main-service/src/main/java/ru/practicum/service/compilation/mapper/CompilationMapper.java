package ru.practicum.service.compilation.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.practicum.service.category.dto.CategoryResponseDto;
import ru.practicum.service.compilation.Compilation;
import ru.practicum.service.compilation.dto.CompilationDto;
import ru.practicum.service.compilation.dto.NewCompilationDto;
import ru.practicum.service.event.Event;
import ru.practicum.service.event.dto.EventShortDto;
import ru.practicum.service.event.dto.UserShortDto;
import ru.practicum.service.user.model.User;

import java.util.HashSet;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface CompilationMapper {

    CompilationDto toCompilationDto(Compilation compilation);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    Compilation toCompilation(NewCompilationDto newCompilationDto);

    @AfterMapping
    default void mapEvents(Compilation compilation, @MappingTarget CompilationDto dto) {
        if (compilation.getEvents() != null) {
            Set<EventShortDto> eventDtos = new HashSet<>();

            for (Event event : compilation.getEvents()) {
                EventShortDto eventDto = new EventShortDto();
                eventDto.setId(event.getId());
                eventDto.setTitle(event.getTitle());
                eventDto.setAnnotation(event.getAnnotation());
                eventDto.setEventDate(event.getEventDate());
                eventDto.setPaid(event.getPaid());
                eventDto.setConfirmedRequests(event.getConfirmedRequests());
                eventDto.setViews(0L); // Для компиляций обычно устанавливаем 0 просмотров

                // Маппинг категории
                if (event.getCategory() != null) {
                    CategoryResponseDto categoryDto = new CategoryResponseDto();
                    categoryDto.setId(event.getCategory().getId());
                    categoryDto.setName(event.getCategory().getName());
                    eventDto.setCategory(categoryDto);
                }

                // Маппинг инициатора
                if (event.getInitiator() != null) {
                    User initiator = event.getInitiator();
                    UserShortDto initiatorDto = new UserShortDto();
                    initiatorDto.setId(initiator.getId());
                    initiatorDto.setName(initiator.getName());
                    eventDto.setInitiator(initiatorDto);
                }

                eventDtos.add(eventDto);
            }

            dto.setEvents(eventDtos);
        }
    }
}