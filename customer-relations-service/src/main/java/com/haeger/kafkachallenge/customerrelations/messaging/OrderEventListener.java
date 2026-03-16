package com.haeger.kafkachallenge.customerrelations.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.common.events.KafkaTopics;
import com.haeger.kafkachallenge.common.events.OrderCreatedPayload;
import com.haeger.kafkachallenge.customerrelations.service.CustomerNotificationService;
import com.haeger.kafkachallenge.customerrelations.service.ProcessedEventService;
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
    private final CustomerNotificationService customerNotificationService;

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
                case EventTypes.ORDER_CREATED,
                     EventTypes.ORDER_CONFIRMED,
                     EventTypes.ORDER_DECLINED,
                     EventTypes.SHIPMENT_PREPARATION_STARTED,
                     EventTypes.ORDER_SHIPPED -> handleOrderStatusUpdate(event.getType(), event.getPayload());
                default -> LOG.debug("Ignoring unsupported order event type {}", event.getType());
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to deserialize order event", ex);
        }
    }

    /**
     * Handles the processing of order status update events by deserializing the event payload
     * and delegating the update notification to the customer notification service.
     *
     * @param eventType The type of the event that triggered the status update, such as
     *                  "ORDER_CREATED", "ORDER_CONFIRMED", or "ORDER_SHIPPED".
     * @param payload   The JSON payload containing the details of the order to be processed,
     *                  including order ID, customer information, and order items.
     * @throws JsonProcessingException If there is an error while deserializing the JSON payload
     *                                 into an {@code OrderCreatedPayload}.
     */
    private void handleOrderStatusUpdate(String eventType, JsonNode payload) throws JsonProcessingException {
        OrderCreatedPayload order = objectMapper.treeToValue(payload, OrderCreatedPayload.class);
        customerNotificationService.sendOrderStatusUpdate(eventType, order);
        LOG.info("Processed {} for order {}", eventType, order.getOrderId());
    }
}
