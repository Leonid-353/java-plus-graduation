package ru.yandex.practicum.service;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.stats.GetStatsRequest;
import ru.yandex.practicum.dto.stats.StatsDtoRequest;
import ru.yandex.practicum.dto.stats.StatsDtoResponse;
import ru.yandex.practicum.exception.BadRequestException;
import ru.yandex.practicum.mapper.StatsMapper;
import ru.yandex.practicum.model.QStats;
import ru.yandex.practicum.model.Stats;
import ru.yandex.practicum.repository.StatsRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    final StatsRepository statsRepository;
    final EntityManager entityManager;
    final StatsMapper statsMapper;

    @Override
    @Transactional
    public void saveHit(StatsDtoRequest request) {
        Stats stats = statsMapper.mapToStat(request);
        statsRepository.save(stats);
    }

    @Override
    public List<StatsDtoResponse> findStats(GetStatsRequest request) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QStats stats = QStats.stats;

        if (request.getEnd().isBefore(request.getStart())) {
            throw new BadRequestException("Дата старта не может быть больше даты финиша.");
        }

        JPAQuery<StatsDtoResponse> query = queryFactory
                .select(
                        Projections.constructor(StatsDtoResponse.class,
                                stats.app,
                                stats.uri,
                                request.getUnique() ? stats.ip.countDistinct() : stats.ip.count())
                )
                .from(stats)
                .where(stats.timestamp.between(request.getStart(), request.getEnd()))
                .groupBy(stats.app, stats.uri)
                .orderBy(stats.id.count().desc());

        if (request.hasUris()) {
            query.where(stats.uri.in(request.getUris()));
        }

        return query.fetch();
    }
}
