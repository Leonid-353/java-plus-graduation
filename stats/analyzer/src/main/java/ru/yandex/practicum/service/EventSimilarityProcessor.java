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
    static final Duration PROCESSING_DELAY = Duration.ofMillis(50);

    final AnalyzerEventSimilarityConsumer consumer;
    final EventSimilarityHandler handler;

    public void start() {
        log.info("=== Запуск обработчика схожести событий ===");

        try {
            registerShutdownHook();
            log.info("Начало обработки сообщений из Kafka...");

            while (true) {
                try {
                    ConsumerRecords<Long, SpecificRecordBase> records = consumer.poll(POLL_TIMEOUT);

                    int messageCount = records.count();
                    if (messageCount > 0) {
                        log.debug("Получено {} сообщений", messageCount);

                        for (ConsumerRecord<Long, SpecificRecordBase> record : records) {
                            processEventSimilarity(record);
                        }

                        consumer.commitAsync();
                        log.debug("Смещения зафиксированы");
                    }

                    Thread.sleep(PROCESSING_DELAY);
                } catch (WakeupException ex) {
                    log.info("Получен WakeupException - завершение обработки");
                    break;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.info("Поток обработки прерван");
                    break;
                } catch (Exception ex) {
                    log.error("Ошибка в цикле обработки", ex);
                }
            }
        } finally {
            shutdown();
        }
    }

    private void processEventSimilarity(ConsumerRecord<Long, SpecificRecordBase> record) {
        try {
            EventSimilarityAvro avro = (EventSimilarityAvro) record.value();

            log.debug("Обработка: eventA={}, eventB={}, offset={}",
                    avro.getEventA(), avro.getEventB(), record.offset());

            handler.handle(avro);
        } catch (ClassCastException ex) {
            log.error("Ошибка преобразования типа сообщения: {}", record.value().getClass(), ex);
        } catch (Exception ex) {
            log.error("Ошибка обработки сообщения offset={}", record.offset(), ex);
        }
    }

    private void shutdown() {
        log.info("Завершение работы обработчика...");

        try {
            log.info("Фиксация оставшихся смещений...");
            consumer.commitSync();
        } catch (Exception ex) {
            log.error("Ошибка при фиксации смещений", ex);
        } finally {
            try {
                log.info("Закрытие AnalyzerEventSimilarityConsumer...");
                consumer.close();
            } catch (Exception ex) {
                log.error("Ошибка при закрытии AnalyzerEventSimilarityConsumer", ex);
            }
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
