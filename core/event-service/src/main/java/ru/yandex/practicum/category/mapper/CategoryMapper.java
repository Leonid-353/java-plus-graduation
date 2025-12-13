package ru.yandex.practicum.category.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.category.model.Category;
import ru.yandex.practicum.dto.category.CategoryRequestDto;
import ru.yandex.practicum.dto.category.CategoryResponseDto;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    Category toCategory(CategoryRequestDto categoryRequestDto);

    CategoryResponseDto toCategoryResponseDto(Category category);
}
