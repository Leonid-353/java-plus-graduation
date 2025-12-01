package ru.practicum.service.request;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.service.event.Event;
import ru.practicum.service.user.model.User;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    User requester;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    RequestStatus status;

    @Column(nullable = false)
    LocalDateTime created;
}
