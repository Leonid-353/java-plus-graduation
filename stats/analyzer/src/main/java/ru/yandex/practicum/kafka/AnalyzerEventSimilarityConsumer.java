package ru.yandex.practicum.kafka;

import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.config.KafkaConfigConsumer;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnalyzerEventSimilarityConsumer implements AutoCloseable {
    final KafkaConfigConsumer kafkaConfig;
    KafkaConsumer<Long, SpecificRecordBase> consumer;

    @PostConstruct
    public void init() {
        KafkaConfigConsumer.ConsumerSimilaritiesConfig config = kafkaConfig.getConsumerSimilarities();

        String topic = config.getTypedTopics()
                .get(KafkaConfigConsumer.TopicType.EVENTS_SIMILARITY);

        if (topic == null) {
            throw new IllegalArgumentException("Топик для EVENTS_SIMILARITY не настроен!");
        }

        consumer = new KafkaConsumer<>(config.getProperties());
        consumer.subscribe(Collections.singletonList(topic));
        log.info("Consumer создан для топика: {}", topic);
    }

    public ConsumerRecords<Long, SpecificRecordBase> poll(Duration timeout) {
        return consumer.poll(timeout);
    }

    public void commitAsync() {
        consumer.commitAsync();
    }

    @Override
    public void close() {
        consumer.close();
    }

    public void wakeup() {
        log.info("Прерывание AnalyzerEventSimilarityConsumer");
        consumer.wakeup();
    }
}
