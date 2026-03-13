package com.haeger.kafkachallenge.shipment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.haeger.kafkachallenge.common.dto.ShipmentDto;
import com.haeger.kafkachallenge.common.dto.ShipmentStatus;
import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.common.events.KafkaTopics;
import com.haeger.kafkachallenge.common.events.OrderCreatedItemPayload;
import com.haeger.kafkachallenge.common.events.OrderCreatedPayload;
import com.haeger.kafkachallenge.shipment.entity.Shipment;
import com.haeger.kafkachallenge.shipment.repository.ShipmentRepository;
import com.haeger.kafkachallenge.shipment.util.mapper.PojoMapper;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private OutboxService outboxService;

    @Spy
    private PojoMapper pojoMapper;

    @Spy
    private JsonMapper objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build();

    @InjectMocks
    private ShipmentService shipmentService;

    @Test
    void startPreparationCreatesShipmentAndEnqueuesEvent() {
        OrderCreatedPayload order = OrderCreatedPayload.builder()
            .orderId(1001L)
            .customerId(42L)
            .customerEmail("max@example.com")
            .customerFullName("Max Mustermann")
            .items(java.util.List.of(
                OrderCreatedItemPayload.builder()
                    .productId(10L)
                    .productSku("CPU-AMD-7800X3D")
                    .productName("AMD Ryzen 7 7800X3D")
                    .quantity(1)
                    .build()
            ))
            .build();
        when(shipmentRepository.findByOrderId(1001L)).thenReturn(Optional.empty());
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> {
            Shipment shipment = invocation.getArgument(0);
            shipment.setId(501L);
            return shipment;
        });

        ShipmentDto result = shipmentService.startPreparation(order);

        assertThat(result.getId()).isEqualTo(501L);
        assertThat(result.getOrderId()).isEqualTo(1001L);
        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.PREPARATION_STARTED);
        assertThat(result.getPreparationStartedAt()).isNotNull();

        ArgumentCaptor<IntegrationEvent> eventCaptor = ArgumentCaptor.forClass(IntegrationEvent.class);
        verify(outboxService).enqueue(eq(KafkaTopics.ORDERS), eventCaptor.capture());
        IntegrationEvent<?> event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo(EventTypes.SHIPMENT_PREPARATION_STARTED);
        assertThat(event.getReferenceId()).isEqualTo("1001");
    }

    @Test
    void startPreparationReturnsExistingShipmentWithoutPublishingDuplicateEvent() {
        Shipment shipment = Shipment.builder()
            .id(501L)
            .orderId(1001L)
            .customerId(42L)
            .customerEmail("max@example.com")
            .customerFullName("Max Mustermann")
            .status(ShipmentStatus.PREPARATION_STARTED)
            .createdAt(Instant.parse("2026-03-12T18:00:00Z"))
            .updatedAt(Instant.parse("2026-03-12T18:00:00Z"))
            .preparationStartedAt(Instant.parse("2026-03-12T18:00:00Z"))
            .orderSnapshot("""
                {"orderId":1001,"customerId":42,"customerEmail":"max@example.com","customerFullName":"Max Mustermann","items":[{"productId":10,"productSku":"CPU-AMD-7800X3D","productName":"AMD Ryzen 7 7800X3D","quantity":1}]}
                """.trim())
            .build();
        when(shipmentRepository.findByOrderId(1001L)).thenReturn(Optional.of(shipment));

        ShipmentDto result = shipmentService.startPreparation(OrderCreatedPayload.builder()
            .orderId(1001L)
            .customerId(42L)
            .customerEmail("max@example.com")
            .customerFullName("Max Mustermann")
            .items(java.util.List.of(
                OrderCreatedItemPayload.builder()
                    .productId(10L)
                    .productSku("CPU-AMD-7800X3D")
                    .productName("AMD Ryzen 7 7800X3D")
                    .quantity(1)
                    .build()
            ))
            .build());

        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.PREPARATION_STARTED);
        verify(shipmentRepository, never()).save(any(Shipment.class));
        verify(outboxService, never()).enqueue(any(), any());
    }

    @Test
    void completeShipmentMarksShipmentAsShippedAndEnqueuesEvent() {
        Shipment shipment = Shipment.builder()
            .id(501L)
            .orderId(1001L)
            .customerId(42L)
            .customerEmail("max@example.com")
            .customerFullName("Max Mustermann")
            .status(ShipmentStatus.PREPARATION_STARTED)
            .createdAt(Instant.parse("2026-03-12T18:00:00Z"))
            .updatedAt(Instant.parse("2026-03-12T18:00:00Z"))
            .preparationStartedAt(Instant.parse("2026-03-12T18:00:00Z"))
            .orderSnapshot("""
                {"orderId":1001,"customerId":42,"customerEmail":"max@example.com","customerFullName":"Max Mustermann","items":[{"productId":10,"productSku":"CPU-AMD-7800X3D","productName":"AMD Ryzen 7 7800X3D","quantity":1}]}
                """.trim())
            .build();
        when(shipmentRepository.findByOrderId(1001L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        ShipmentDto result = shipmentService.completeShipment(1001L);

        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.SHIPPED);
        assertThat(result.getShippedAt()).isNotNull();

        ArgumentCaptor<IntegrationEvent> eventCaptor = ArgumentCaptor.forClass(IntegrationEvent.class);
        verify(outboxService).enqueue(eq(KafkaTopics.ORDERS), eventCaptor.capture());
        IntegrationEvent<?> event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo(EventTypes.ORDER_SHIPPED);
        assertThat(event.getReferenceId()).isEqualTo("1001");
    }
}
