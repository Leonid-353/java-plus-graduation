package ru.practicum.service.stats.service;

import ru.practicum.service.stats.GetStatsRequest;
import ru.practicum.service.stats.StatsDtoRequest;
import ru.practicum.service.stats.StatsDtoResponse;

import java.util.List;

public interface StatsService {

    void saveHit(StatsDtoRequest request);

    List<StatsDtoResponse> findStats(GetStatsRequest request);
}
