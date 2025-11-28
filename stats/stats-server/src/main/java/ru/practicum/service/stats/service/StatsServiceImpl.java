package ru.practicum.service.stats.service;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.service.stats.GetStatsRequest;
import ru.practicum.service.stats.StatsDtoRequest;
import ru.practicum.service.stats.StatsDtoResponse;
import ru.practicum.service.stats.exception.BadRequestException;
import ru.practicum.service.stats.mapper.StatsMapper;
import ru.practicum.service.stats.model.QStats;
import ru.practicum.service.stats.model.Stats;
import ru.practicum.service.stats.repository.StatsRepository;

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
