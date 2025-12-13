package ru.yandex.practicum.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.category.model.Category;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);

    Boolean existsByName(String name);
}
