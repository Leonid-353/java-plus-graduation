package ru.yandex.practicum.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.config.KafkaConfigProducer;
import ru.yandex.practicum.kafka.AggregatorEventSimilarityProducer;
import ru.yandex.practicum.kafka.AggregatorUserActionConsumer;
import ru.yandex.practicum.service.constants.ActionWeights;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AggregationStarter {
    final AggregatorUserActionConsumer consumer;
    final AggregatorEventSimilarityProducer producer;
    final UserActionService service;

    /**
     * Метод для начала процесса агрегации данных.
     * Получает взаимодействия пользователей с событиями,
     * проводит расчеты и записывает в кафку.
     */
    public void start() {
        log.info("=== Запуск агрегатора схожести событий ===");
        log.info("Используемые веса: VIEW={}, REGISTER={}, LIKE={}",
                ActionWeights.VIEW, ActionWeights.REGISTER, ActionWeights.LIKE);

        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Получен сигнал завершения работы...");
                consumer.wakeup();
            }));
            log.info("Начало обработки сообщений из Kafka...");

            while (true) {
                ConsumerRecords<Long, SpecificRecordBase> records = consumer.poll(Duration.ofMillis(1000));
                log.debug("Получено {} сообщений", records.count());

                if (!records.isEmpty()) {
                    for (ConsumerRecord<Long, SpecificRecordBase> record : records) {
                        // processUserAction(record);
                        UserActionAvro actionAvro = (UserActionAvro) record.value();
                        service.updateSimilarity(actionAvro)
                                .forEach(similarity -> {
                                    producer.send(
                                        KafkaConfigProducer.TopicType.EVENTS_SIMILARITY,
                                        similarity.getEventA(),
                                        similarity);
                                    log.info("Similarity send in Aggregator: {}", similarity);
                                });
                    }
                    consumer.commitAsync();
                    log.debug("Смещения зафиксированы");
                }
            }
        } catch (WakeupException ignored) {
            log.info("Получен WakeupException - завершение работы");
        } catch (Exception ex) {
            log.error("Ошибка в цикле обработки", ex);
        } finally {
            shutdown();
        }
    }

//    private void processUserAction(ConsumerRecord<Long, SpecificRecordBase> record) {
//        UserActionAvro actionAvro = (UserActionAvro) record.value();
//
//        log.debug("Обработка: userId={}, eventId={}, action={}, offset={}",
//                actionAvro.getUserId(), actionAvro.getEventId(),
//                actionAvro.getActionType(), record.offset());
//
//        List<EventSimilarityAvro> updatedSimilarities = service.updateSimilarity(actionAvro);
//
//        if (!updatedSimilarities.isEmpty()) {
//            sendSimilaritiesToKafka(updatedSimilarities);
//            log.debug("Отправлено {} схожестей", updatedSimilarities.size());
//        }
//    }
//
//    private void sendSimilaritiesToKafka(Iterable<EventSimilarityAvro> similarities) {
//        for (EventSimilarityAvro similarity : similarities) {
//            try {
//                long key = similarity.getEventA();
//                producer.send(KafkaConfigProducer.TopicType.EVENTS_SIMILARITY, key, similarity);
//            } catch (Exception ex) {
//                log.error("Ошибка отправки схожести в Kafka: {}", similarity, ex);
//                throw new RuntimeException("Не удалось отправить сообщение в Kafka", ex);
//            }
//        }
//        producer.flush();
//    }

    private void shutdown() {
        log.info("Завершение работы агрегатора...");

        try {
            log.info("Сброс оставшихся сообщений в Kafka...");
            producer.flush();

            log.info("Фиксация смещений...");
            consumer.commitAsync();
        } catch (Exception ex) {
            log.error("Ошибка при завершении работы", ex);
        } finally {
            log.info("Закрытие consumer...");
            consumer.close();

            log.info("Закрытие producer...");
            producer.close();
        }

        log.info("Агрегатор остановлен");
    }
}
