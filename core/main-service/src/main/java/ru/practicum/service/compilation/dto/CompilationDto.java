package ru.practicum.service.compilation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.service.event.dto.EventShortDto;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompilationDto {
    private Long id;

    @NotBlank
    @Size(min = 1, max = 50)
    private String title;

    private Boolean pinned;

    @Builder.Default
    private Set<EventShortDto> events = new HashSet<>();
}