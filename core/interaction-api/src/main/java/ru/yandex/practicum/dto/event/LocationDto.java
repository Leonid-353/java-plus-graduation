package ru.yandex.practicum.dto.event;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LocationDto {
    @NotNull
    private Double lat;

    @NotNull
    private Double lon;
}