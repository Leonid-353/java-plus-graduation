package ru.practicum.service.category.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.service.category.dto.CategoryRequestDto;
import ru.practicum.service.category.dto.CategoryResponseDto;
import ru.practicum.service.category.service.CategoryService;

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

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{catId}")
    public void deleteCategory(@PathVariable Long catId) {
        categoryService.deleteCategory(catId);
    }
}
