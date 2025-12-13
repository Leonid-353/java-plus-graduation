package ru.yandex.practicum.category.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.category.service.CategoryService;
import ru.yandex.practicum.dto.category.CategoryRequestDto;
import ru.yandex.practicum.dto.category.CategoryResponseDto;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin/categories")
public class CategoryAdminController {
    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponseDto saveCategory(@Valid @RequestBody CategoryRequestDto categoryRequestDto) {
        return categoryService.createCategory(categoryRequestDto);
    }

    @PatchMapping("/{catId}")
    public CategoryResponseDto updateCategory(@Valid @RequestBody CategoryRequestDto categoryRequestDto,
                                              @PathVariable Long catId) {
        return categoryService.updateCategory(catId, categoryRequestDto);
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long catId) {
        categoryService.deleteCategory(catId);
    }
}
