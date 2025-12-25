package ru.yandex.practicum.kafka;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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
public class AggregatorUserActionConsumer {
    final KafkaConfigConsumer kafkaConfig;
    KafkaConsumer<Long, SpecificRecordBase> consumer;

    @PostConstruct
    public void init() {
        String topic = kafkaConfig.getConsumer().getTypedTopics()
                .get(KafkaConfigConsumer.TopicType.USER_ACTIONS);

        if (topic == null) {
            throw new IllegalArgumentException("Топик для USER_ACTIONS не настроен!");
        }

        this.consumer = new KafkaConsumer<>(kafkaConfig.getConsumer().getProperties());
        this.consumer.subscribe(Collections.singletonList(topic));
        log.info("Consumer создан для топика: {}", topic);
    }

    public ConsumerRecords<Long, SpecificRecordBase> poll(Duration timeout) {
        if (consumer == null) {
            throw new IllegalStateException("Consumer не инициализирован!");
        }
        return consumer.poll(timeout);
    }

    public void commitAsync() {
        if (consumer != null) {
            consumer.commitAsync();
        }
    }

    public void commitSync() {
        if (consumer != null) {
            consumer.commitSync();
        }
    }

    @PreDestroy
    public void close() {
        if (consumer != null) {
            consumer.close();
            log.info("Consumer закрыт");
        }
    }

    public void wakeup() {
        log.info("Прерывание AggregatorUserActionConsumer");
        if (consumer != null) {
            consumer.wakeup();
        }
    }
}
