package com.haeger.kafkachallenge.shipment.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.common.events.KafkaTopics;
import com.haeger.kafkachallenge.common.events.OrderCreatedPayload;
import com.haeger.kafkachallenge.shipment.service.ProcessedEventService;
import com.haeger.kafkachallenge.shipment.service.ShipmentService;
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
    private final ShipmentService shipmentService;

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
                case EventTypes.ORDER_CONFIRMED -> handleOrderConfirmed(event.getPayload());
                default -> LOG.debug("Ignoring unsupported order event type {}", event.getType());
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to deserialize order event", ex);
        }
    }

    /**
     * Handles the processing of an "ORDER_CONFIRMED" event.
     * This method deserializes the event payload into an {@link OrderCreatedPayload} object,
     * initiates the shipment preparation process for the order, and logs the processing status.
     *
     * @param payload the JSON payload containing the details of the confirmed order
     * @throws JsonProcessingException if the payload cannot be deserialized into an {@link OrderCreatedPayload} object
     */
    private void handleOrderConfirmed(JsonNode payload) throws JsonProcessingException {
        OrderCreatedPayload order = objectMapper.treeToValue(payload, OrderCreatedPayload.class);
        shipmentService.startPreparation(order);
        LOG.info("Processed {} for order {}", EventTypes.ORDER_CONFIRMED, order.getOrderId());
    }
}
