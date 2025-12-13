package ru.yandex.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import ru.yandex.practicum.dto.stats.StatsDtoRequest;
import ru.yandex.practicum.model.Stats;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StatsMapper {

    @Mapping(target = "timestamp", source = "timestamp", qualifiedByName = "toOffsetDateTime")
    @Mapping(target = "id", ignore = true)
    Stats mapToStat(StatsDtoRequest statsDtoRequest);

    @Named("toOffsetDateTime")
    default OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        return OffsetDateTime.of(localDateTime, ZoneOffset.UTC);
    }
}