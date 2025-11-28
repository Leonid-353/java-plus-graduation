package ru.practicum.service.category.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.service.category.dto.CategoryRequestDto;
import ru.practicum.service.category.dto.CategoryResponseDto;
import ru.practicum.service.category.model.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    Category toCategory(CategoryRequestDto categoryRequestDto);

    CategoryResponseDto toCategoryResponseDto(Category category);
}
