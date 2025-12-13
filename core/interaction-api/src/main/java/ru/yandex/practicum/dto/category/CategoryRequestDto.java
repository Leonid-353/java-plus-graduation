package ru.yandex.practicum.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryRequestDto {
    Long id;

    @NotBlank
    @Size(min = 1, max = 50, message = "Name length must be between 1 and 50 characters")
    String name;
}
