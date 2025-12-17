package ru.yandex.practicum.category.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.category.service.CategoryService;
import ru.yandex.practicum.dto.category.CategoryResponseDto;

import java.util.Collection;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/categories")
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public Collection<CategoryResponseDto> getCategories(@RequestParam(defaultValue = "10", required = false) int size,
                                                         @RequestParam(defaultValue = "0", required = false) int from) {
        return categoryService.getCategories(from, size);
    }

    @GetMapping("/{catId}")
    public CategoryResponseDto getCategory(@PathVariable Long catId) {
        return categoryService.getCategoryById(catId);
    }
}
