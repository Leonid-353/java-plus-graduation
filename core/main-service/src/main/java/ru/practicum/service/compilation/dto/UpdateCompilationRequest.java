package ru.practicum.service.compilation.dto;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateCompilationRequest {
    @Size(min = 1, max = 50)
    private String title;

    private Boolean pinned;

    private Set<Long> events;
}