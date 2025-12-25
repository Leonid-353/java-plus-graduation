package ru.yandex.practicum.controller;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.stats.recommendations.*;
import ru.yandex.practicum.service.handler.RecommendationHandler;

@Slf4j
@GrpcService
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnalyzerController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    final RecommendationHandler handler;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Получен запрос рекомендуемых мероприятий для пользователя: {}", request);
        try {
            handler.getRecommendationsForUser(request).forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(ex)));
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Получен запрос рекомендаций по схожести мероприятий: {}", request);
        try {
            handler.getSimilarEvents(request).forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(ex)));
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Получен запрос рекомендаций по количеству взаимодействий: {}", request);
        try {
            handler.getInteractionsCount(request).forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(ex)));
        }
    }
}
