package ru.yandex.practicum.kafka.deserializer;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.kafka.support.serializer.DeserializationException;

public class GeneralAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {
    private final DecoderFactory decoderFactory;
    private final DatumReader<T> datumReader;

    public GeneralAvroDeserializer(Schema schema) {
        this(DecoderFactory.get(), schema);
    }

    public GeneralAvroDeserializer(DecoderFactory decoderFactory, Schema schema) {
        this.decoderFactory = decoderFactory;
        this.datumReader = new SpecificDatumReader<>(schema);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        try {
            if (data != null) {
                BinaryDecoder decoder = decoderFactory.binaryDecoder(data, null);
                return this.datumReader.read(null, decoder);
            }
            return null;
        } catch (Exception ex) {
            throw new DeserializationException(
                    String.format("Ошибка десериализации данных из топика [%s]", topic), data, false, ex
            );
        }
    }
}
