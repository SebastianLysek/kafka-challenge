package com.haeger.kafkachallenge.streaming.streams.serde;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class ProtobufSerde<T extends MessageLite> implements Serde<T> {
    private final Serializer<T> serializer = new ProtobufSerializer<>();
    private final Deserializer<T> deserializer;

    public ProtobufSerde(Parser<T> parser) {
        this.deserializer = new ProtobufDeserializer<>(parser);
    }

    @Override
    public Serializer<T> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<T> deserializer() {
        return deserializer;
    }

    @FunctionalInterface
    public interface Parser<T> {
        T parse(byte[] bytes) throws InvalidProtocolBufferException;
    }

    private static class ProtobufSerializer<T extends MessageLite> implements Serializer<T> {
        @Override
        public byte[] serialize(String topic, T data) {
            return data == null ? null : data.toByteArray();
        }
    }

    private static class ProtobufDeserializer<T> implements Deserializer<T> {
        private final Parser<T> parser;

        private ProtobufDeserializer(Parser<T> parser) {
            this.parser = parser;
        }

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
        }

        @Override
        public T deserialize(String topic, byte[] data) {
            if (data == null) {
                return null;
            }
            try {
                return parser.parse(data);
            } catch (InvalidProtocolBufferException ex) {
                throw new SerializationException("Failed to parse protobuf message from " + topic, ex);
            }
        }
    }
}
