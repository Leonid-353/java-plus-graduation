package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.dto.request.enums.RequestStatus;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "requests", schema = "public")
@EqualsAndHashCode(of = "id")
@Getter
@Setter
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @JoinColumn(name = "event_id", nullable = false)
    Long eventId;

    @JoinColumn(name = "requester_id", nullable = false)
    Long requesterId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    RequestStatus status;

    @Column(nullable = false)
    LocalDateTime created;
}
