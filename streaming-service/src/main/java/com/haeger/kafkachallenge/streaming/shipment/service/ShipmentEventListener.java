package com.haeger.kafkachallenge.streaming.shipment.service;

import com.haeger.kafkachallenge.streaming.order.service.OrderService;
import com.haeger.kafkachallenge.streaming.proto.ShipmentPreparationStarted;
import com.haeger.kafkachallenge.streaming.streams.topology.StreamingTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShipmentEventListener {
    private final OrderService orderService;

    @KafkaListener(
        topics = StreamingTopics.SHIPMENT_PREPARATION_STARTED,
        groupId = "streaming-service-shipment-projection",
        containerFactory = "shipmentPreparationStartedKafkaListenerContainerFactory"
    )
    public void onShipmentPreparationStarted(ShipmentPreparationStarted event) {
        orderService.markPreparationStarted(event.getOrderId());
    }
}
