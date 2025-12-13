package ru.yandex.practicum.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatsDtoResponse {
    String app;
    String uri;
    Long hits;
}
