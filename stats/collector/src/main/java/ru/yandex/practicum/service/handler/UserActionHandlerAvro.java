package ru.yandex.practicum.service.handler;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.config.KafkaConfig;
import ru.yandex.practicum.grpc.stats.action.ActionTypeProto;
import ru.yandex.practicum.grpc.stats.action.UserActionProto;
import ru.yandex.practicum.service.CollectorUserActionProducer;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserActionHandlerAvro {
    final CollectorUserActionProducer producer;

    public void handle(UserActionProto action) {

        Instant timestamp = Instant.ofEpochSecond(
                action.getTimestamp().getSeconds(),
                action.getTimestamp().getNanos()
        );
        UserActionAvro actionAvro = UserActionAvro.newBuilder()
                .setUserId(action.getUserId())
                .setEventId(action.getEventId())
                .setActionType(getActionTypeAvro(action.getActionType()))
                .setTimestamp(timestamp)
                .build();

        producer.send(KafkaConfig.TopicType.USER_ACTIONS, actionAvro.getEventId(), actionAvro);

        log.info("Действие пользователя: {} отправлено в топик", actionAvro);
    }

    private ActionTypeAvro getActionTypeAvro(ActionTypeProto actionType) {
        return switch (actionType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException(
                    String.format("Неизвестный тип действия пользователя: %s", actionType)
            );
        };
    }
}
