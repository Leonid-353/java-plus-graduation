package ru.yandex.practicum.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.config.KafkaConfig;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CollectorUserActionProducer {
    final KafkaConfig kafkaConfig;
    KafkaProducer<Long, SpecificRecordBase> producer;

    @PostConstruct
    public void init() {
        String topic = kafkaConfig.getProducer().getTypedTopics()
                .get(KafkaConfig.TopicType.USER_ACTIONS);

        if (topic == null) {
            throw new IllegalArgumentException("Топик для USER_ACTIONS не настроен!");
        }

        this.producer = new KafkaProducer<>(kafkaConfig.getProducer().getProperties());
        log.info("Producer создан для топика: {}", topic);
    }

    public void send(KafkaConfig.TopicType topicType, Long key, SpecificRecordBase message) {
        String topicName = kafkaConfig.getProducer().getTypedTopics().get(topicType);

        if (topicName == null || topicName.isEmpty()) {
            throw new RuntimeException(String.format("Нет такого топика: %s", topicType));
        }

        producer.send(new ProducerRecord<>(topicName, key, message));
        producer.flush();
    }

    public void flush() {
        producer.flush();
    }

    @PreDestroy
    public void close() {
        if (producer != null) {
            producer.flush();
            producer.close(Duration.ofSeconds(15));
            log.info("Producer закрыт");
        }
    }
}
