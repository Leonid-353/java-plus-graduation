package ru.yandex.practicum.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.service.constants.ActionWeights;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserActionServiceImpl implements UserActionService {
    final Map<Long, Map<Long, Double>> eventUserWeights = new HashMap<>();
    final Map<Long, Double> eventTotalWeights = new HashMap<>();
    final Map<Long, Map<Long, Double>> eventPairMinSums = new HashMap<>();

    @Override
    public List<EventSimilarityAvro> updateSimilarity(UserActionAvro actionAvro) {
        long userId = actionAvro.getUserId();
        long eventId = actionAvro.getEventId();
        ActionTypeAvro actionType = actionAvro.getActionType();
        Instant timestamp = actionAvro.getTimestamp();

        // Преобразуем тип действия в вес
        double newWeight = getActionWeight(actionType);

        // Получаем текущий вес пользователя для этого события
        Map<Long, Double> usersForEvent = eventUserWeights
                .computeIfAbsent(eventId, k -> new HashMap<>());
        double oldWeight = usersForEvent.getOrDefault(userId, 0.0);

        // Если вес не увеличился - возвращаем пустой список
        if (newWeight <= oldWeight) {
            log.debug("Вес не изменился для userId={}, eventId={}: {} -> {}",
                    userId, eventId, oldWeight, newWeight);
            return Collections.emptyList();
        }

        // Обновляем вес пользователя и общую сумму
        updateUserWeight(userId, eventId, oldWeight, newWeight, usersForEvent);

        List<EventSimilarityAvro> updatedSimilarities = new ArrayList<>();

        // Для каждого другого события, где этот пользователь тоже взаимодействовал
        for (Map.Entry<Long, Map<Long, Double>> entry : eventUserWeights.entrySet()) {
            long otherEventId = entry.getKey();

            // Пропускаем то же самое событие
            if (otherEventId == eventId) {
                continue;
            }

            Map<Long, Double> usersForOtherEvent = entry.getValue();
            Double otherWeight = usersForOtherEvent.get(userId);

            // Пропускаем, если пользователь не взаимодействовал с событием
            if (otherWeight == null || otherWeight <= 0) {
                continue;
            }

            // Обновляем минимальную сумму для пары событий
            double newMinSum = updateMinSumForPair(userId, eventId, otherEventId, oldWeight, newWeight, otherWeight);

            // Рассчитываем новую схожесть
            double similarity = calculateSimilarity(eventId, otherEventId, newMinSum);

            EventSimilarityAvro similarityAvro = createSimilarityAvro(eventId, otherEventId, similarity, timestamp);
            updatedSimilarities.add(similarityAvro);

            log.debug("Обновлена схожесть: events [{}, {}] = {}",
                    eventId, otherEventId, similarity);
        }

        log.info("Обработано действие: userId={}, eventId={}, type={}. Обновлено {} схожестей.",
                userId, eventId, actionType, updatedSimilarities.size());

        return updatedSimilarities;
    }

    private double getActionWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> ActionWeights.VIEW;
            case REGISTER -> ActionWeights.REGISTER;
            case LIKE -> ActionWeights.LIKE;
            default -> {
                log.warn("Неизвестный тип действия: {}", actionType);
                yield 0.0;
            }
        };
    }

    private void updateUserWeight(long userId, long eventId,
                                  double oldWeight, double newWeight,
                                  Map<Long, Double> usersForEvent) {
        usersForEvent.put(userId, newWeight);

        double currentTotal = eventTotalWeights.getOrDefault(eventId, 0.0);
        double newTotal = currentTotal - oldWeight + newWeight;
        eventTotalWeights.put(eventId, newTotal);

        log.debug("Обновлён вес: eventId={}, userId={}: {} -> {}, total={}",
                eventId, userId, oldWeight, newWeight, newTotal);
    }

    private double updateMinSumForPair(long userId, long eventId, long otherEventId,
                                       double oldWeight, double newWeight, double otherWeight) {
        long eventA = Math.min(eventId, otherEventId);
        long eventB = Math.max(eventId, otherEventId);

        Map<Long, Double> pairsForA = eventPairMinSums
                .computeIfAbsent(eventA, k -> new HashMap<>());
        double currentMinSum = pairsForA.getOrDefault(eventB, 0.0);

        double oldMin = Math.min(oldWeight, otherWeight);
        double newMin = Math.min(newWeight, otherWeight);
        double updatedMinSum = currentMinSum - oldMin + newMin;

        pairsForA.put(eventB, updatedMinSum);

        log.debug("Обновлён minSum для пары [{}, {}]: {} -> {}",
                eventA, eventB, currentMinSum, updatedMinSum);

        return updatedMinSum;
    }

    private double calculateSimilarity(long eventId, long otherEventId, double minSum) {
        double totalWeight1 = eventTotalWeights.getOrDefault(eventId, 0.0);
        double totalWeight2 = eventTotalWeights.getOrDefault(otherEventId, 0.0);

        if (totalWeight1 <= 0 || totalWeight2 <= 0) {
            return 0.0;
        }

        double product = totalWeight1 * totalWeight2;
        if (product <= 0) {
            return 0.0;
        }

        double similarity = minSum / Math.sqrt(product);

        if (similarity < 0) {
            return 0.0;
        }
        if (similarity > 1.0) {
            log.warn("Схожесть > 1.0: {} для событий [{}, {}]. minSum={}, weights=[{}, {}]",
                    similarity, eventId, otherEventId, minSum, totalWeight1, totalWeight2);
            return 1.0;
        }

        return similarity;
    }

    private EventSimilarityAvro createSimilarityAvro(long eventId, long otherEventId,
                                                     double similarity, Instant timestamp) {
        long eventA = Math.min(eventId, otherEventId);
        long eventB = Math.max(eventId, otherEventId);

        return EventSimilarityAvro.newBuilder()
                .setEventA(eventA)
                .setEventB(eventB)
                .setScore(similarity)
                .setTimestamp(timestamp)
                .build();
    }
}
