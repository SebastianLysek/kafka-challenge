package com.haeger.kafkachallenge.streaming.config;

import com.haeger.kafkachallenge.streaming.proto.NotificationRequested;
import com.haeger.kafkachallenge.streaming.proto.OrderCreated;
import com.haeger.kafkachallenge.streaming.proto.OrderStatusChanged;
import com.haeger.kafkachallenge.streaming.proto.ProductUpserted;
import com.haeger.kafkachallenge.streaming.proto.ShipmentCompleted;
import com.haeger.kafkachallenge.streaming.proto.ShipmentPreparationStarted;
import com.haeger.kafkachallenge.streaming.streams.serde.ProtoSerdes;
import org.apache.kafka.common.serialization.Serde;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaSerdeConfiguration {
    @Bean
    Serde<ProductUpserted> productUpsertedSerde() {
        return ProtoSerdes.productUpserted();
    }

    @Bean
    Serde<OrderCreated> orderCreatedSerde() {
        return ProtoSerdes.orderCreated();
    }

    @Bean
    Serde<OrderStatusChanged> orderStatusChangedSerde() {
        return ProtoSerdes.orderStatusChanged();
    }

    @Bean
    Serde<ShipmentPreparationStarted> shipmentPreparationStartedSerde() {
        return ProtoSerdes.shipmentPreparationStarted();
    }

    @Bean
    Serde<ShipmentCompleted> shipmentCompletedSerde() {
        return ProtoSerdes.shipmentCompleted();
    }

    @Bean
    Serde<NotificationRequested> notificationRequestedSerde() {
        return ProtoSerdes.notificationRequested();
    }
}
