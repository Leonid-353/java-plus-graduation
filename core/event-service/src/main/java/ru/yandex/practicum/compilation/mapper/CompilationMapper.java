package ru.yandex.practicum.compilation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.compilation.model.Compilation;
import ru.yandex.practicum.dto.compilation.CompilationDto;
import ru.yandex.practicum.dto.compilation.NewCompilationDto;

@Mapper(componentModel = "spring")
public interface CompilationMapper {

    CompilationDto toCompilationDto(Compilation compilation);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    Compilation toCompilation(NewCompilationDto newCompilationDto);
}