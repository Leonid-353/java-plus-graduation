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
import ru.yandex.practicum.kafka.AnalyzerEventSimilarityConsumer;
import ru.yandex.practicum.service.handler.EventSimilarityHandler;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventSimilarityProcessor {
    static final Duration POLL_TIMEOUT = Duration.ofMillis(5000);

    final AnalyzerEventSimilarityConsumer consumer;
    final EventSimilarityHandler handler;

    public void start() {
        log.info("=== Запуск обработчика схожести событий ===");

        try {
            registerShutdownHook();
            log.info("Начало обработки сообщений из Kafka...");

            while (true) {
                ConsumerRecords<Long, SpecificRecordBase> records = consumer.poll(POLL_TIMEOUT);
                log.debug("Получено {} сообщений", records.count());

                if (!records.isEmpty()) {
                    for (ConsumerRecord<Long, SpecificRecordBase> record : records) {
                        EventSimilarityAvro avro = (EventSimilarityAvro) record.value();
                        handler.handle(avro);
                    }
                    consumer.commitAsync();
                    log.debug("Смещения зафиксированы");
                }
            }
        } catch (WakeupException ignored) {
            log.info("Получен WakeupException - завершение обработки");
        } catch (Exception ex) {
            log.error("Ошибка в цикле обработки", ex);
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        log.info("Завершение работы обработчика...");

        try {
            log.info("Фиксация оставшихся смещений...");
            consumer.commitAsync();
        } catch (Exception ex) {
            log.error("Ошибка при фиксации смещений", ex);
        } finally {
            log.info("Закрытие AnalyzerEventSimilarityConsumer...");
            consumer.close();
        }

        log.info("Обработчик остановлен");
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Сработал ShutdownHook в EventSimilarityProcessor");
            consumer.wakeup();
        }));
    }
}
