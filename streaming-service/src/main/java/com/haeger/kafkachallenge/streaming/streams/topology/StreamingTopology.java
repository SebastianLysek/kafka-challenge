package com.haeger.kafkachallenge.streaming.streams.topology;

import com.haeger.kafkachallenge.common.dto.OrderStatus;
import com.haeger.kafkachallenge.streaming.inventory.service.InventoryReservationService;
import com.haeger.kafkachallenge.streaming.proto.OrderCreated;
import com.haeger.kafkachallenge.streaming.proto.OrderStatusChanged;
import com.haeger.kafkachallenge.streaming.proto.ProductUpserted;
import com.haeger.kafkachallenge.streaming.proto.ShipmentCompleted;
import com.haeger.kafkachallenge.streaming.streams.serde.ProtoSerdes;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class StreamingTopology {
    private final InventoryReservationService inventoryReservationService;

    @Bean
    KStream<String, OrderCreated> streamingServiceTopology(StreamsBuilder builder) {
        return buildTopology(builder, inventoryReservationService::reserve);
    }

    public static KStream<String, OrderCreated> buildTopology(
        StreamsBuilder builder,
        Consumer<OrderCreated> inventoryReservation
    ) {
        Serde<ProductUpserted> productSerde = ProtoSerdes.productUpserted();
        Serde<OrderCreated> orderCreatedSerde = ProtoSerdes.orderCreated();
        Serde<OrderStatusChanged> orderStatusChangedSerde = ProtoSerdes.orderStatusChanged();
        Serde<ShipmentCompleted> shipmentCompletedSerde = ProtoSerdes.shipmentCompleted();

        builder.table(
            StreamingTopics.PRODUCT_UPSERTED,
            Consumed.with(Serdes.String(), productSerde),
            Materialized.<String, ProductUpserted, KeyValueStore<Bytes, byte[]>>as(StreamingTopics.INVENTORY_STORE)
                .withKeySerde(Serdes.String())
                .withValueSerde(productSerde)
        );

        KStream<String, OrderCreated> orders = builder.stream(
            StreamingTopics.ORDER_CREATED,
            Consumed.with(Serdes.String(), orderCreatedSerde)
        );
        orders.transform(
                () -> new OrderDecisionTransformer(inventoryReservation),
                StreamingTopics.INVENTORY_STORE
            )
            .to(StreamingTopics.ORDER_STATUS_CHANGED, Produced.with(Serdes.String(), orderStatusChangedSerde));

        builder.stream(StreamingTopics.SHIPMENT_COMPLETED, Consumed.with(Serdes.String(), shipmentCompletedSerde))
            .mapValues(event -> OrderStatusChanged.newBuilder()
                .setOrderId(event.getOrderId())
                .setStatus(OrderStatus.SHIPPED.name())
                .setReason("Shipment completed")
                .build())
            .selectKey((key, value) -> Long.toString(value.getOrderId()))
            .to(StreamingTopics.ORDER_STATUS_CHANGED, Produced.with(Serdes.String(), orderStatusChangedSerde));

        return orders;
    }
}
