package ru.yandex.practicum.kafka;

import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.config.KafkaConfigProducer;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AggregatorEventSimilarityProducer implements AutoCloseable {
    final KafkaConfigProducer kafkaConfig;
    KafkaProducer<String, SpecificRecordBase> producer;

    @PostConstruct
    public void init() {
        String topic = kafkaConfig.getProducer().getTypedTopics()
                .get(KafkaConfigProducer.TopicType.EVENTS_SIMILARITY);

        if (topic == null) {
            throw new IllegalArgumentException("Топик для EVENT_SIMILARITY не настроен!");
        }

        this.producer = new KafkaProducer<>(kafkaConfig.getProducer().getProperties());
        log.info("Producer создан для топика: {}", topic);
    }

    public void send(KafkaConfigProducer.TopicType topicType, String key, SpecificRecordBase message) {
        String topicName = kafkaConfig.getProducer().getTypedTopics().get(topicType);

        if (topicName == null || topicName.isEmpty()) {
            throw new RuntimeException(String.format("Нет такого топика: %s", topicType));
        }

        producer.send(new ProducerRecord<>(topicName, key, message));
    }

    public void flush() {
        producer.flush();
    }

    @Override
    public void close() {
        producer.flush();
        producer.close(Duration.ofSeconds(5));
        log.info("Producer закрыт");
    }
}
