package ru.yandex.practicum.event.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.category.model.Category;
import ru.yandex.practicum.dto.event.enums.State;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "events", schema = "public")
@EqualsAndHashCode(of = "id")
@Getter
@Setter
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 120)
    String title;

    @Column(nullable = false, length = 2000)
    String annotation;

    @Column(nullable = false, length = 7000)
    String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    Category category;

    @Column(name = "event_date", nullable = false)
    LocalDateTime eventDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    Location location;

    @Column(nullable = false)
    Boolean paid;

    @Column(name = "participant_limit", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    Integer participantLimit;

    @Column(name = "request_moderation", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    Boolean requestModeration;

    @Column(name = "confirmed_requests", columnDefinition = "BIGINT DEFAULT 0")
    Long confirmedRequests;

    @Column(name = "created_on", nullable = false)
    LocalDateTime createdOn;

    @JoinColumn(name = "initiator_id", nullable = false)
    Long initiatorId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    State state;

    @Column(name = "published_on")
    LocalDateTime publishedOn;

}
