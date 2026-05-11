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
    private final ProtoSerdes protoSerdes;

    public KafkaSerdeConfiguration(ProtoSerdes protoSerdes) {
        this.protoSerdes = protoSerdes;
    }

    @Bean
    Serde<ProductUpserted> productUpsertedSerde() {
        return protoSerdes.productUpserted();
    }

    @Bean
    Serde<OrderCreated> orderCreatedSerde() {
        return protoSerdes.orderCreated();
    }

    @Bean
    Serde<OrderStatusChanged> orderStatusChangedSerde() {
        return protoSerdes.orderStatusChanged();
    }

    @Bean
    Serde<ShipmentPreparationStarted> shipmentPreparationStartedSerde() {
        return protoSerdes.shipmentPreparationStarted();
    }

    @Bean
    Serde<ShipmentCompleted> shipmentCompletedSerde() {
        return protoSerdes.shipmentCompleted();
    }

    @Bean
    Serde<NotificationRequested> notificationRequestedSerde() {
        return protoSerdes.notificationRequested();
    }
}
