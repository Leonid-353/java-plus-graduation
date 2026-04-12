package ru.yandex.practicum.kafka.deserializer;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public class EventSimilarityDeserializer extends GeneralAvroDeserializer<EventSimilarityAvro> {
    public EventSimilarityDeserializer() {
        super(EventSimilarityAvro.getClassSchema());
    }
}
