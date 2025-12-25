package ru.yandex.practicum.kafka.serializer;

import com.google.protobuf.Message;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

public class GeneralProtoSerializer implements Serializer<Message> {
    @Override
    public byte[] serialize(String topic, Message data) {
        try {
            return data != null ? data.toByteArray() : null;
        } catch (Exception ex) {
            throw new SerializationException(
                    String.format("Ошибка сериализации protobuf-сообщения для топика [%s]", topic), ex
            );
        }
    }
}
