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
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.kafka.AnalyzerUserActionConsumer;
import ru.yandex.practicum.service.handler.UserActionHandler;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserActionProcessor implements Runnable {
    static final Duration POLL_TIMEOUT = Duration.ofMillis(500);

    final AnalyzerUserActionConsumer consumer;
    final UserActionHandler handler;


    @Override
    public void run() {
        log.info("=== Запуск обработчика действий пользователей ===");

        registerShutdownHook();

        try {
            log.info("Начало обработки сообщений из Kafka...");

            while (true) {
                ConsumerRecords<Long, SpecificRecordBase> records = consumer.poll(POLL_TIMEOUT);

                int messageCount = records.count();
                if (messageCount > 0) {
                    log.debug("Получено {} сообщений", messageCount);

                    for (ConsumerRecord<Long, SpecificRecordBase> record : records) {
                        processUserAction(record);
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

    private void processUserAction(ConsumerRecord<Long, SpecificRecordBase> record) {
        UserActionAvro avro = (UserActionAvro) record.value();

        log.debug("Обработка: userId={}, eventId={}, actionType={}, offset={}",
                avro.getUserId(), avro.getEventId(),
                avro.getActionType(), record.offset());

        handler.handle(avro);
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
                log.info("Закрытие AnalyzerUserActionConsumer...");
                consumer.close();
            } catch (Exception ex) {
                log.error("Ошибка при закрытии AnalyzerUserActionConsumer", ex);
            }
        }

        log.info("Обработчик остановлен");
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Сработал ShutdownHook в UserActionProcessor");
            consumer.wakeup();
        }));
    }
}
