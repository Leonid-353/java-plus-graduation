package ru.yandex.practicum.service.handler;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.model.EventSimilarity;
import ru.yandex.practicum.repository.EventSimilarityRepository;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventSimilarityHandler {
    final EventSimilarityRepository repository;

    public void handle(EventSimilarityAvro eventSimilarityAvro) {
        EventSimilarity eventSimilarity = EventSimilarity.builder()
                .eventA(eventSimilarityAvro.getEventA())
                .eventB(eventSimilarityAvro.getEventB())
                .score(eventSimilarityAvro.getScore())
                .timestamp(eventSimilarityAvro.getTimestamp())
                .build();
        repository.save(eventSimilarity);
    }
}
