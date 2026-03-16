package com.haeger.kafkachallenge.inventory.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.common.events.KafkaTopics;
import com.haeger.kafkachallenge.common.events.OrderCreatedPayload;
import com.haeger.kafkachallenge.inventory.service.OrderFulfillmentService;
import com.haeger.kafkachallenge.inventory.service.ProcessedEventService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderEventListener {
    private static final TypeReference<IntegrationEvent<JsonNode>> EVENT_TYPE = new TypeReference<>() { };
    private static final Logger LOG = LoggerFactory.getLogger(OrderEventListener.class);

    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;
    private final OrderFulfillmentService orderFulfillmentService;

    @KafkaListener(topics = KafkaTopics.ORDERS)
    @Transactional
    public void onOrderEvent(String message) {
        try {
            IntegrationEvent<JsonNode> event = objectMapper.readValue(message, EVENT_TYPE);

            if (event.getEventId() == null || event.getEventId().isBlank()) {
                throw new IllegalArgumentException("Order event is missing eventId");
            }
            if (!processedEventService.claimEvent(event)) {
                LOG.info("Skipping duplicate {} event {}", event.getType(), event.getEventId());
                return;
            }

            switch (event.getType()) {
                case EventTypes.ORDER_CREATED -> handleOrderCreated(event.getPayload());
                default -> LOG.debug("Ignoring unsupported order event type {}", event.getType());
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to deserialize order event", ex);
        }
    }

    private void handleOrderCreated(JsonNode payload) throws JsonProcessingException {
        OrderCreatedPayload order = objectMapper.treeToValue(payload, OrderCreatedPayload.class);
        orderFulfillmentService.handleOrderCreated(order);
        LOG.info("Processed {} for order {}", EventTypes.ORDER_CREATED, order.getOrderId());
    }
}
