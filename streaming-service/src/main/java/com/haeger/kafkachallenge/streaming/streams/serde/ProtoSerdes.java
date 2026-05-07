package com.haeger.kafkachallenge.streaming.streams.serde;

import com.haeger.kafkachallenge.streaming.proto.NotificationRequested;
import com.haeger.kafkachallenge.streaming.proto.OrderCreated;
import com.haeger.kafkachallenge.streaming.proto.OrderStatusChanged;
import com.haeger.kafkachallenge.streaming.proto.ProductUpserted;
import com.haeger.kafkachallenge.streaming.proto.ShipmentCompleted;
import com.haeger.kafkachallenge.streaming.proto.ShipmentPreparationStarted;
import org.apache.kafka.common.serialization.Serde;

public final class ProtoSerdes {
    private ProtoSerdes() {
    }

    public static Serde<ProductUpserted> productUpserted() {
        return new ProtobufSerde<>(ProductUpserted::parseFrom);
    }

    public static Serde<OrderCreated> orderCreated() {
        return new ProtobufSerde<>(OrderCreated::parseFrom);
    }

    public static Serde<OrderStatusChanged> orderStatusChanged() {
        return new ProtobufSerde<>(OrderStatusChanged::parseFrom);
    }

    public static Serde<ShipmentPreparationStarted> shipmentPreparationStarted() {
        return new ProtobufSerde<>(ShipmentPreparationStarted::parseFrom);
    }

    public static Serde<ShipmentCompleted> shipmentCompleted() {
        return new ProtobufSerde<>(ShipmentCompleted::parseFrom);
    }

    public static Serde<NotificationRequested> notificationRequested() {
        return new ProtobufSerde<>(NotificationRequested::parseFrom);
    }
}
