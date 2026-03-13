package com.haeger.kafkachallenge.shipment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haeger.kafkachallenge.common.dto.ShipmentDto;
import com.haeger.kafkachallenge.common.dto.ShipmentStatus;
import com.haeger.kafkachallenge.common.dto.OrderStatus;
import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.common.events.KafkaTopics;
import com.haeger.kafkachallenge.common.events.OrderCreatedPayload;
import com.haeger.kafkachallenge.shipment.entity.Shipment;
import com.haeger.kafkachallenge.shipment.repository.ShipmentRepository;
import com.haeger.kafkachallenge.shipment.util.mapper.PojoMapper;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ShipmentService {
    private static final Logger LOG = LoggerFactory.getLogger(ShipmentService.class);

    private final ShipmentRepository shipmentRepository;
    private final OutboxService outboxService;
    private final PojoMapper pojoMapper;
    private final ObjectMapper objectMapper;

    /**
     * Initiates the preparation process for a shipment associated with the given order.
     * If a shipment already exists for the order and its status is either
     * PREPARATION_STARTED or SHIPPED, the method skips the preparation and returns the existing shipment.
     * Otherwise, it creates a new shipment or updates an existing one, setting its status to PREPARATION_STARTED,
     * and saves the shipment to the repository.
     * Additionally, the method publishes a shipment preparation started event.
     *
     * @param order the payload containing the details of the order for which the shipment preparation is to be started
     * @return a {@link ShipmentDto} representing the shipment whose preparation process was initiated
     * @throws IllegalArgumentException if the order payload lacks necessary fields, such as orderId, customerId,
     *                                  customer email, customer full name, or items
     */
    @Transactional
    public ShipmentDto startPreparation(OrderCreatedPayload order) {
        validateOrderSnapshot(order);

        Shipment existingShipment = shipmentRepository.findByOrderId(order.getOrderId()).orElse(null);
        if (existingShipment != null) {
            if (existingShipment.getStatus() == ShipmentStatus.PREPARATION_STARTED
                || existingShipment.getStatus() == ShipmentStatus.SHIPPED) {
                LOG.info(
                    "Shipment for order {} already has status {}, skipping preparation start",
                    order.getOrderId(),
                    existingShipment.getStatus()
                );
                return pojoMapper.toShipmentDto(existingShipment);
            }
        }

        Instant now = Instant.now();
        Shipment shipment = existingShipment != null ? existingShipment : new Shipment();
        pojoMapper.applyOrderSnapshot(order, shipment);
        shipment.setStatus(ShipmentStatus.PREPARATION_STARTED);
        if (shipment.getCreatedAt() == null) {
            shipment.setCreatedAt(now);
        }
        shipment.setUpdatedAt(now);
        shipment.setPreparationStartedAt(now);
        shipment.setOrderSnapshot(toJson(order));

        Shipment savedShipment = shipmentRepository.save(shipment);
        publishShipmentEvent(EventTypes.SHIPMENT_PREPARATION_STARTED, savedShipment);
        return pojoMapper.toShipmentDto(savedShipment);
    }

    /**
     * Completes the shipment process for the order associated with the given order ID.
     * Updates the shipment status to SHIPPED and records the time of shipment.
     * If the shipment is already completed or in an invalid state for completion,
     * appropriate exceptions are thrown.
     *
     * @param orderId the unique identifier of the order whose shipment is to be completed
     * @return a {@link ShipmentDto} representing the details of the completed shipment
     * @throws ResponseStatusException if the shipment is not found or cannot be completed from its current status
     */
    @Transactional
    public ShipmentDto completeShipment(Long orderId) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Shipment for order %d not found".formatted(orderId)
            ));

        if (shipment.getStatus() == ShipmentStatus.SHIPPED) {
            LOG.info("Shipment for order {} already completed, skipping", orderId);
            return pojoMapper.toShipmentDto(shipment);
        }
        if (shipment.getStatus() != ShipmentStatus.PREPARATION_STARTED) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Shipment for order %d cannot be completed from status %s".formatted(orderId, shipment.getStatus())
            );
        }

        shipment.setStatus(ShipmentStatus.SHIPPED);
        shipment.setUpdatedAt(Instant.now());
        shipment.setShippedAt(shipment.getUpdatedAt());

        Shipment savedShipment = shipmentRepository.save(shipment);
        publishShipmentEvent(EventTypes.ORDER_SHIPPED, savedShipment);
        return pojoMapper.toShipmentDto(savedShipment);
    }

    /**
     * Publishes a shipment event to the outbox service for processing.
     *
     * @param eventType The type of event to be published, such as "CREATED", "UPDATED", or "DELETED".
     * @param shipment The shipment object containing details about the shipment, including its status and associated order information.
     */
    private void publishShipmentEvent(String eventType, Shipment shipment) {
        OrderCreatedPayload payload = readOrderSnapshot(shipment);
        payload.setStatus(resolveOrderStatus(shipment.getStatus()));
        payload.setUpdatedAt(shipment.getUpdatedAt());

        outboxService.enqueue(
            KafkaTopics.ORDERS,
            IntegrationEvent.of(
                eventType,
                shipment.getOrderId().toString(),
                payload
            )
        );
    }

    /**
     * Validates that the provided order snapshot contains all required fields.
     * If any required field is missing or invalid, an IllegalArgumentException is thrown.
     *
     * @param order The order snapshot to validate. Must contain non-null and non-blank values for
     *              orderId, customerId, customerEmail, customerFullName, and a non-empty list of items.
     * @throws IllegalArgumentException if any required field in the order snapshot is missing or invalid.
     */
    private void validateOrderSnapshot(OrderCreatedPayload order) {
        if (order.getOrderId() == null) {
            throw new IllegalArgumentException("Order snapshot is missing orderId");
        }
        if (order.getCustomerId() == null) {
            throw new IllegalArgumentException("Order snapshot is missing customerId");
        }
        if (order.getCustomerEmail() == null || order.getCustomerEmail().isBlank()) {
            throw new IllegalArgumentException("Order snapshot is missing customerEmail");
        }
        if (order.getCustomerFullName() == null || order.getCustomerFullName().isBlank()) {
            throw new IllegalArgumentException("Order snapshot is missing customerFullName");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order snapshot is missing items");
        }
    }

    /**
     * Reads and deserializes the order snapshot from the given shipment object
     * into an instance of OrderCreatedPayload.
     *
     * @param shipment The shipment object containing the serialized order snapshot.
     * @return An instance of OrderCreatedPayload representing the deserialized order snapshot.
     * @throws IllegalStateException If the deserialization of the shipment order snapshot fails.
     */
    private OrderCreatedPayload readOrderSnapshot(Shipment shipment) {
        try {
            return objectMapper.readValue(shipment.getOrderSnapshot(), OrderCreatedPayload.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize shipment order snapshot", ex);
        }
    }

    /**
     * Converts an OrderCreatedPayload object into its JSON representation as a String.
     *
     * @param order the OrderCreatedPayload object to be serialized
     * @return the JSON representation of the given OrderCreatedPayload as a String
     * @throws IllegalStateException if the serialization process fails
     */
    private String toJson(OrderCreatedPayload order) {
        try {
            return objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize shipment order snapshot", ex);
        }
    }

    /**
     * Resolves the order status based on the provided shipment status.
     *
     * @param shipmentStatus The current status of the shipment, represented as a {@code ShipmentStatus} enum.
     * @return The corresponding {@code OrderStatus} enum value based on the shipment status.
     */
    private OrderStatus resolveOrderStatus(ShipmentStatus shipmentStatus) {
        return switch (shipmentStatus) {
            case PREPARATION_STARTED -> OrderStatus.PREP_STARTED;
            case SHIPPED -> OrderStatus.SHIPPED;
        };
    }
}
