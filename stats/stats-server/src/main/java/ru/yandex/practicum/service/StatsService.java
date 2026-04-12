package ru.yandex.practicum.service;

import ru.yandex.practicum.dto.stats.GetStatsRequest;
import ru.yandex.practicum.dto.stats.StatsDtoRequest;
import ru.yandex.practicum.dto.stats.StatsDtoResponse;

import java.util.List;

public interface StatsService {

    void saveHit(StatsDtoRequest request);

    List<StatsDtoResponse> findStats(GetStatsRequest request);
}
