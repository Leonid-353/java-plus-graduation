package ru.yandex.practicum.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.event.model.Location;

import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByLatAndLon(Double lat, Double lon);
}