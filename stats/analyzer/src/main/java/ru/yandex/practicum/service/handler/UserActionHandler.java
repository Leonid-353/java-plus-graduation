package ru.yandex.practicum.service.handler;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.model.UserAction;
import ru.yandex.practicum.model.enums.ActionType;
import ru.yandex.practicum.repository.UserActionRepository;
import ru.yandex.practicum.service.constants.ActionWeights;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserActionHandler {
    final UserActionRepository repository;

    public void handle(UserActionAvro actionAvro) {
        ActionType newType = ActionType.valueOf(actionAvro.getActionType().name());

        repository.findByUserIdAndEventId(actionAvro.getUserId(), actionAvro.getEventId())
                .ifPresentOrElse(
                        action -> updateIfNeeded(action, newType, actionAvro.getTimestamp()),
                        () -> createNew(actionAvro, newType)
                );
    }

    private void updateIfNeeded(UserAction action, ActionType newType, Instant timestamp) {
        if (ActionWeights.getWeight(newType) > ActionWeights.getWeight(action.getActionType())) {
            action.setActionType(newType);
            action.setTimestamp(timestamp);
            repository.save(action);
        }
    }

    private void createNew(UserActionAvro actionAvro, ActionType actionType) {
        UserAction action = UserAction.builder()
                .userId(actionAvro.getUserId())
                .eventId(actionAvro.getEventId())
                .actionType(actionType)
                .timestamp(actionAvro.getTimestamp())
                .build();
        repository.save(action);
    }
}
