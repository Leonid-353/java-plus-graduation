package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.model.Stats;

public interface StatsRepository extends JpaRepository<Stats, Long> {
}
