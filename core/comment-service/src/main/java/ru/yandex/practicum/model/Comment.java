package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 2000)
    String text;

    @JoinColumn(name = "event_id", nullable = false)
    Long eventId;

    @JoinColumn(name = "author_id", nullable = false)
    Long authorId;

    @Column(nullable = false)
    LocalDateTime created;
}
