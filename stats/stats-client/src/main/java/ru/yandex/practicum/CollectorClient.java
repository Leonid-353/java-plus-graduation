package ru.yandex.practicum;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.stats.action.ActionTypeProto;
import ru.yandex.practicum.grpc.stats.action.UserActionProto;
import ru.yandex.practicum.grpc.stats.collector.UserActionControllerGrpc;

import java.time.Instant;

@Slf4j
@Service
public class CollectorClient {
    @GrpcClient("collector")
    UserActionControllerGrpc.UserActionControllerBlockingStub client;

    public void collectUserAction(long userId, long eventId, ActionTypeProto actionType, Instant instant) {
        try {
            Timestamp timestamp = Timestamp.newBuilder()
                    .setSeconds(instant.getEpochSecond())
                    .setNanos(instant.getNano())
                    .build();
            UserActionProto request = UserActionProto.newBuilder()
                    .setUserId(userId)
                    .setEventId(eventId)
                    .setActionType(actionType)
                    .setTimestamp(timestamp)
                    .build();
            client.collectUserAction(request);
        } catch (Exception ex) {
            log.error("Ошибка при отправке запроса", ex);
        }
    }
}
