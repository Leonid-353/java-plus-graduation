package ru.practicum.service.category.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.service.category.dto.CategoryResponseDto;
import ru.practicum.service.category.service.CategoryService;

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
