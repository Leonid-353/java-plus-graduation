package ru.yandex.practicum.dto.compilation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.dto.event.EventShortDto;

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