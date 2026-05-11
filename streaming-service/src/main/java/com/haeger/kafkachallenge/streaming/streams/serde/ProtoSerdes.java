package com.haeger.kafkachallenge.streaming.streams.serde;

import com.google.protobuf.Message;
import com.haeger.kafkachallenge.streaming.proto.NotificationRequested;
import com.haeger.kafkachallenge.streaming.proto.OrderCreated;
import com.haeger.kafkachallenge.streaming.proto.OrderStatusChanged;
import com.haeger.kafkachallenge.streaming.proto.ProductUpserted;
import com.haeger.kafkachallenge.streaming.proto.ShipmentCompleted;
import com.haeger.kafkachallenge.streaming.proto.ShipmentPreparationStarted;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig;
import io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.serialization.Serde;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProtoSerdes {
    private final String schemaRegistryUrl;

    public ProtoSerdes(
        @Value("${spring.kafka.properties.schema.registry.url:mock://streaming-service}") String schemaRegistryUrl
    ) {
        this.schemaRegistryUrl = schemaRegistryUrl;
    }

    public Serde<ProductUpserted> productUpserted() {
        return protobufSerde(ProductUpserted.class);
    }

    public Serde<OrderCreated> orderCreated() {
        return protobufSerde(OrderCreated.class);
    }

    public Serde<OrderStatusChanged> orderStatusChanged() {
        return protobufSerde(OrderStatusChanged.class);
    }

    public Serde<ShipmentPreparationStarted> shipmentPreparationStarted() {
        return protobufSerde(ShipmentPreparationStarted.class);
    }

    public Serde<ShipmentCompleted> shipmentCompleted() {
        return protobufSerde(ShipmentCompleted.class);
    }

    public Serde<NotificationRequested> notificationRequested() {
        return protobufSerde(NotificationRequested.class);
    }

    private <T extends Message> Serde<T> protobufSerde(Class<T> valueType) {
        KafkaProtobufSerde<T> serde = new KafkaProtobufSerde<>();
        Map<String, Object> config = new HashMap<>();
        config.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        config.put(KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE, valueType.getName());
        serde.configure(config, false);
        return serde;
    }
}
