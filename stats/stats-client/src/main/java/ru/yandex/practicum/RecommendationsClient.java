package ru.yandex.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.stats.recommendations.*;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationsClient {

    @GrpcClient("analyzer")
    RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    public Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        try {
            UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();
            Iterator<RecommendedEventProto> iterator = client.getRecommendationsForUser(request);
            return asStream(iterator);
        } catch (Exception ex) {
            log.error("Ошибка при получении рекомендованных мероприятий для пользователя: {}", userId, ex);
            return Stream.empty();
        }
    }

    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        try {
            SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                    .setEventId(eventId)
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();
            Iterator<RecommendedEventProto> iterator = client.getSimilarEvents(request);
            return asStream(iterator);
        } catch (Exception ex) {
            log.error("Ошибка при получении похожих на {} мероприятий, с которыми не взаимодействовал пользователь {}",
                    eventId, userId, ex);
            return Stream.empty();
        }
    }

    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        try {
            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addAllEventId(eventIds)
                    .build();
            Iterator<RecommendedEventProto> iterator = client.getInteractionsCount(request);
            return asStream(iterator);
        } catch (Exception ex) {
            log.error("Ошибка при получении мероприятий: {} с суммой максимальных весов действий каждого пользователя",
                    eventIds, ex);
            return Stream.empty();
        }
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}
