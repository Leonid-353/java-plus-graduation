package ru.yandex.practicum.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.stats.action.UserActionProto;
import ru.yandex.practicum.grpc.stats.collector.UserActionControllerGrpc;
import ru.yandex.practicum.service.handler.UserActionHandlerAvro;

@Slf4j
@GrpcService
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserActionController extends UserActionControllerGrpc.UserActionControllerImplBase {
    private final UserActionHandlerAvro handler;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Обработка действия пользователя: {}", request);

            handler.handle(request);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(ex.getLocalizedMessage())
                            .withCause(ex)
            ));
        }
    }
}
