package com.haeger.kafkachallenge.streaming.shipment.service;

import com.haeger.kafkachallenge.common.dto.ShipmentDto;
import com.haeger.kafkachallenge.common.dto.ShipmentStatus;
import com.haeger.kafkachallenge.streaming.config.DtoMapper;
import com.haeger.kafkachallenge.streaming.order.entity.Order;
import com.haeger.kafkachallenge.streaming.proto.ShipmentCompleted;
import com.haeger.kafkachallenge.streaming.proto.ShipmentPreparationStarted;
import com.haeger.kafkachallenge.streaming.shipment.entity.Shipment;
import com.haeger.kafkachallenge.streaming.shipment.repository.ShipmentRepository;
import com.haeger.kafkachallenge.streaming.streams.topology.ProtoEventPublisher;
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
    private final ProtoEventPublisher eventPublisher;
    private final DtoMapper dtoMapper;

    @Transactional
    public void startPreparation(Order order) {
        Shipment existingShipment = shipmentRepository.findByOrderId(order.getId()).orElse(null);
        if (existingShipment != null) {
            LOG.info("Shipment for order {} already exists with status {}", order.getId(), existingShipment.getStatus());
            return;
        }

        Instant now = Instant.now();
        Shipment shipment = shipmentRepository.save(Shipment.builder()
            .orderId(order.getId())
            .customerId(order.getCustomerId())
            .customerEmail(order.getCustomerEmail())
            .customerFullName(order.getCustomerFullName())
            .status(ShipmentStatus.PREPARATION_STARTED)
            .createdAt(now)
            .updatedAt(now)
            .preparationStartedAt(now)
            .build());

        eventPublisher.publishShipmentPreparationStarted(ShipmentPreparationStarted.newBuilder()
            .setShipmentId(shipment.getId())
            .setOrderId(shipment.getOrderId())
            .setCustomerId(shipment.getCustomerId())
            .setCustomerEmail(shipment.getCustomerEmail())
            .setCustomerFullName(shipment.getCustomerFullName())
            .build());
    }

    @Transactional
    public ShipmentDto completeShipment(Long orderId) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Shipment for order %d not found".formatted(orderId)
            ));

        if (shipment.getStatus() == ShipmentStatus.SHIPPED) {
            return dtoMapper.toShipmentDto(shipment);
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

        eventPublisher.publishShipmentCompleted(ShipmentCompleted.newBuilder()
            .setShipmentId(savedShipment.getId())
            .setOrderId(savedShipment.getOrderId())
            .build());
        return dtoMapper.toShipmentDto(savedShipment);
    }
}
