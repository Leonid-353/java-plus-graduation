package ru.practicum.service.category.service;

import ru.practicum.service.category.dto.CategoryRequestDto;
import ru.practicum.service.category.dto.CategoryResponseDto;

import java.util.Collection;

public interface CategoryService {
    CategoryResponseDto createCategory(CategoryRequestDto category);

    CategoryResponseDto updateCategory(Long id, CategoryRequestDto category);

    void deleteCategory(Long id);

    Collection<CategoryResponseDto> getCategories(Integer from, Integer size);

    CategoryResponseDto getCategoryById(Long id);
}
