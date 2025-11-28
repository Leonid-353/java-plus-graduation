package ru.practicum.service.stats.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.service.stats.model.Stats;

public interface StatsRepository extends JpaRepository<Stats, Long> {
}
