package ru.practicum.service.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.service.category.dto.CategoryRequestDto;
import ru.practicum.service.category.dto.CategoryResponseDto;
import ru.practicum.service.category.mapper.CategoryMapper;
import ru.practicum.service.category.model.Category;
import ru.practicum.service.category.repository.CategoryRepository;
import ru.practicum.service.event.repository.EventRepository;
import ru.practicum.service.exception.ConflictException;
import ru.practicum.service.exception.ForbiddenException;
import ru.practicum.service.exception.NotFoundException;

import java.util.Collection;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    final CategoryRepository categoryRepository;
    final CategoryMapper categoryMapper;
    final EventRepository eventRepository;

    @Override
    public CategoryResponseDto createCategory(CategoryRequestDto categoryRequestDto) {
        validateNameCategory(categoryRequestDto);
        return categoryMapper.toCategoryResponseDto(categoryRepository.save(categoryMapper.toCategory(categoryRequestDto)));
    }

    @Override
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto categoryRequestDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found: " + id));

        String newName = categoryRequestDto.getName();

        if (!newName.equals(category.getName())) {
            categoryRepository.findByName(newName)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ForbiddenException(
                                String.format("Категория с именем: '{}' уже существует", categoryRequestDto.getName()));
                    });

            category.setName(newName);
            category = categoryRepository.save(category);
        }

        return categoryMapper.toCategoryResponseDto(category);
    }

    @Override
    public void deleteCategory(Long id) {
        if (eventRepository.existsByCategoryId(id)) {
            throw new ConflictException("category in events" + id);
        }
        categoryRepository.deleteById(id);
    }

    @Override
    public Collection<CategoryResponseDto> getCategories(Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        return categoryRepository.findAll(pageable).stream().map(categoryMapper::toCategoryResponseDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Category not found: " + id));
        return categoryMapper.toCategoryResponseDto(category);
    }

    // Validation methods
    private void validateNameCategory(CategoryRequestDto dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new ForbiddenException(
                    String.format("Категория с именем: '{}' уже существует", dto.getName()));
        }
    }
}
