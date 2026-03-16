package com.haeger.kafkachallenge.orders.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.common.events.KafkaTopics;
import com.haeger.kafkachallenge.common.events.OrderCreatedPayload;
import com.haeger.kafkachallenge.common.events.OrderFulfillmentCheckResultPayload;
import com.haeger.kafkachallenge.orders.service.OrderService;
import com.haeger.kafkachallenge.orders.service.ProcessedEventService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderFulfillmentEventListener {
    private static final TypeReference<IntegrationEvent<JsonNode>> EVENT_TYPE = new TypeReference<>() { };
    private static final Logger LOG = LoggerFactory.getLogger(OrderFulfillmentEventListener.class);

    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;
    private final OrderService orderService;

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
                case EventTypes.ORDER_FULFILLMENT_CHECK_SUCCEEDED -> handleSucceeded(event.getPayload());
                case EventTypes.ORDER_FULFILLMENT_CHECK_FAILED -> handleFailed(event.getPayload());
                case EventTypes.SHIPMENT_PREPARATION_STARTED -> handleShipmentPreparationStarted(event.getPayload());
                case EventTypes.ORDER_SHIPPED -> handleOrderShipped(event.getPayload());
                default -> LOG.debug("Ignoring unsupported order event type {}", event.getType());
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to deserialize order event", ex);
        }
    }

    private void handleSucceeded(JsonNode payload) throws JsonProcessingException {
        OrderFulfillmentCheckResultPayload result = objectMapper.treeToValue(payload, OrderFulfillmentCheckResultPayload.class);
        orderService.confirmOrder(result.getOrderId());
        LOG.info("Processed {} for order {}", EventTypes.ORDER_FULFILLMENT_CHECK_SUCCEEDED, result.getOrderId());
    }

    private void handleFailed(JsonNode payload) throws JsonProcessingException {
        OrderFulfillmentCheckResultPayload result = objectMapper.treeToValue(payload, OrderFulfillmentCheckResultPayload.class);
        orderService.declineOrder(result.getOrderId());
        LOG.info("Processed {} for order {}", EventTypes.ORDER_FULFILLMENT_CHECK_FAILED, result.getOrderId());
    }

    private void handleShipmentPreparationStarted(JsonNode payload) throws JsonProcessingException {
        OrderCreatedPayload order = objectMapper.treeToValue(payload, OrderCreatedPayload.class);
        orderService.startShipmentPreparation(order.getOrderId());
        LOG.info("Processed {} for order {}", EventTypes.SHIPMENT_PREPARATION_STARTED, order.getOrderId());
    }

    private void handleOrderShipped(JsonNode payload) throws JsonProcessingException {
        OrderCreatedPayload order = objectMapper.treeToValue(payload, OrderCreatedPayload.class);
        orderService.markShipped(order.getOrderId());
        LOG.info("Processed {} for order {}", EventTypes.ORDER_SHIPPED, order.getOrderId());
    }
}
