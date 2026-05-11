package com.haeger.kafkachallenge.streaming.order.service;

import com.haeger.kafkachallenge.streaming.proto.OrderStatusChanged;
import com.haeger.kafkachallenge.streaming.streams.topology.StreamingTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventListener {
    private final OrderService orderService;

    @KafkaListener(
        topics = StreamingTopics.ORDER_STATUS_CHANGED,
        groupId = "streaming-service-order-projection",
        containerFactory = "orderStatusChangedKafkaListenerContainerFactory"
    )
    public void onOrderStatusChanged(OrderStatusChanged event) {
        orderService.applyStatusChanged(event);
    }
}
