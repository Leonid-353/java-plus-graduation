package ru.yandex.practicum.service.handler;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.stats.recommendations.InteractionsCountRequestProto;
import ru.yandex.practicum.grpc.stats.recommendations.RecommendedEventProto;
import ru.yandex.practicum.grpc.stats.recommendations.SimilarEventsRequestProto;
import ru.yandex.practicum.grpc.stats.recommendations.UserPredictionsRequestProto;
import ru.yandex.practicum.model.EventSimilarity;
import ru.yandex.practicum.model.UserAction;
import ru.yandex.practicum.repository.EventSimilarityRepository;
import ru.yandex.practicum.repository.UserActionRepository;
import ru.yandex.practicum.service.constants.ActionWeights;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecommendationHandler {
    final UserActionRepository userActionRepository;
    final EventSimilarityRepository eventSimilarityRepository;

    public List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        Long userId = request.getUserId();
        int limit = request.getMaxResults();

        Set<Long> allUserEventIds = userActionRepository.findDistinctEventIdsByUserId(userId);

        if (allUserEventIds.isEmpty()) {
            log.debug("Пользователь {} не взаимодействовал ни с одним событием", userId);
            return Collections.emptyList();
        }

        Set<Long> recentEvents = getRecentUserEvents(userId, limit);
        Set<Long> candidateIds = getCandidateEvents(recentEvents, allUserEventIds, limit);

        if (candidateIds.isEmpty()) {
            log.debug("Для пользователя {} не найдено новых кандидатов", userId);
            return Collections.emptyList();
        }

        return candidateIds.stream()
                .map(eventId -> buildRecommendation(eventId, calculateRecommendationScore(eventId, allUserEventIds)))
                .sorted(Comparator.comparing(RecommendedEventProto::getScore).reversed())
                .limit(limit)
                .toList();
    }

    public List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        Long eventId = request.getEventId();
        Long userId = request.getUserId();
        int limit = request.getMaxResults();

        Set<Long> userViewedEvents = userActionRepository.findDistinctEventIdsByUserId(userId);

        return eventSimilarityRepository.findTopByEventAOrEventB(
                        eventId,
                        PageRequest.of(0, limit * 2, Sort.by(Sort.Direction.DESC, "score")))
                .stream()
                .map(es -> es.getOtherEvent(eventId))
                .filter(candidateId -> !userViewedEvents.contains(candidateId))
                .distinct()
                .limit(limit)
                .map(id -> buildRecommendation(id, getSimilarityScore(eventId, id)))
                .toList();
    }

    public List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        Set<Long> eventIds = new HashSet<>(request.getEventIdList());

        return eventIds.stream()
                .map(eventId -> {
                    double score = calculateEventTotalScore(eventId);
                    return buildRecommendation(eventId, score);
                })
                .sorted(Comparator.comparing(RecommendedEventProto::getScore).reversed())
                .toList();
    }

    // Вспомогательные методы
    private Set<Long> getRecentUserEvents(Long userId, int limit) {
        PageRequest pageRequest =
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));

        return userActionRepository.findRecentByUserId(userId, pageRequest).stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> getCandidateEvents(Set<Long> recentEvents, Set<Long> allUserEvents, int limit) {
        if (recentEvents.isEmpty()) {
            return Collections.emptySet();
        }

        List<EventSimilarity> similarities = eventSimilarityRepository.findTopSimilarityEvents(
                recentEvents,
                PageRequest.of(0, limit * 2, Sort.by(Sort.Direction.DESC, "score"))
        );

        return similarities.stream()
                .map(es -> {
                    Long eventA = es.getEventA();
                    return recentEvents.contains(eventA) ? es.getEventB() : eventA;
                })
                .filter(candidateId -> !allUserEvents.contains(candidateId))
                .limit(limit * 2L)
                .collect(Collectors.toSet());
    }

    private Double calculateRecommendationScore(Long candidateEventId, Set<Long> allUserEventIds) {
        List<EventSimilarity> similarities = eventSimilarityRepository.findTopByEventAOrEventB(
                candidateEventId,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "score"))
        );

        List<EventSimilarity> relevantSimilarities = similarities.stream()
                .filter(es -> {
                    Long otherEvent = es.getOtherEvent(candidateEventId);
                    return allUserEventIds.contains(otherEvent);
                })
                .toList();

        if (relevantSimilarities.isEmpty()) {
            return 0.0;
        }

        return relevantSimilarities.stream()
                .mapToDouble(EventSimilarity::getScore)
                .average()
                .orElse(0.0);
    }

    private Double getSimilarityScore(Long eventId1, Long eventId2) {
        return eventSimilarityRepository.findByEventPair(eventId1, eventId2)
                .map(EventSimilarity::getScore)
                .orElse(0.0);
    }

    private Double calculateEventTotalScore(Long eventId) {
        return userActionRepository.findAllByEventId(eventId).stream()
                .mapToDouble(ua -> ActionWeights.getWeight(ua.getActionType()))
                .sum();
    }

    private RecommendedEventProto buildRecommendation(Long eventId, Double score) {
        return RecommendedEventProto.newBuilder()
                .setEventId(eventId)
                .setScore(score)
                .build();
    }
}
