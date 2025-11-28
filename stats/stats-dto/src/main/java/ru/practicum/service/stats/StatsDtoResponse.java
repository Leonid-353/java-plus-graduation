package ru.practicum.service.stats;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StatsDtoResponse {
    String app;
    String uri;
    Long hits;
}
