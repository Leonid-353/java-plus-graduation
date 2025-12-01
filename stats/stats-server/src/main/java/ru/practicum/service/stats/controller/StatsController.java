package ru.practicum.service.stats.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.service.stats.GetStatsRequest;
import ru.practicum.service.stats.StatsDtoRequest;
import ru.practicum.service.stats.StatsDtoResponse;
import ru.practicum.service.stats.client.StatsClient;
import ru.practicum.service.stats.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class StatsController implements StatsClient {
    final StatsService statsService;

    @Override
    public void hit(StatsDtoRequest request) {
        log.info("Запрос на добавление данных в статистику");
        statsService.saveHit(request);
    }

    @Override
    public List<StatsDtoResponse> findStats(LocalDateTime start, LocalDateTime end,
                                            List<String> uris, Boolean unique) {
        log.info("Запрос на получение статистики");
        log.info("Параметры: \nstart = {} \nend = {} \nuris = {} \nunique = {}", start, end, uris, unique);
        return statsService.findStats(GetStatsRequest.of(start, end, uris, unique));
    }
}
