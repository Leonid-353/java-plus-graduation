package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "event_similarities")
public class EventSimilarity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "event_a", nullable = false)
    Long eventA;

    @Column(name = "event_b", nullable = false)
    Long eventB;

    @Column(nullable = false)
    Double score;

    @Column(nullable = false)
    Instant timestamp;

    // Получить другое событие в паре
    public Long getOtherEvent(Long knownEventId) {
        if (eventA.equals(knownEventId)) {
            return eventB;
        } else if (eventB.equals(knownEventId)) {
            return eventA;
        }
        throw new IllegalArgumentException(
                String.format("Событие %d не найдено в паре [%d, %d]",
                        knownEventId, eventA, eventB)
        );
    }
}
