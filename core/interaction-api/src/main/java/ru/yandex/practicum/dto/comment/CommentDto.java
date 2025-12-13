package ru.yandex.practicum.dto.comment;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentDto {

    Long id;

    String text;

    Long eventId;

    Long authorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime created;
}
