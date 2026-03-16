package com.haeger.kafkachallenge.shipment.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.common.events.OrderCreatedItemPayload;
import com.haeger.kafkachallenge.common.events.OrderCreatedPayload;
import com.haeger.kafkachallenge.shipment.service.ProcessedEventService;
import com.haeger.kafkachallenge.shipment.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    private final JsonMapper objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build();

    @Mock
    private ProcessedEventService processedEventService;

    @Mock
    private ShipmentService shipmentService;

    private OrderEventListener orderEventListener;

    @BeforeEach
    void setUp() {
        orderEventListener = new OrderEventListener(
            objectMapper,
            processedEventService,
            shipmentService
        );
    }

    @Test
    void orderConfirmedEventStartsShipmentPreparation() throws Exception {
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
        String message = objectMapper.writeValueAsString(IntegrationEvent.of(
            EventTypes.ORDER_CONFIRMED,
            "1001",
            order
        ));
        when(processedEventService.claimEvent(any())).thenReturn(true);

        orderEventListener.onOrderEvent(message);

        verify(shipmentService).startPreparation(order);
        verify(processedEventService).claimEvent(any());
    }

    @Test
    void duplicateEventIsIgnored() throws Exception {
        String message = objectMapper.writeValueAsString(IntegrationEvent.of(
            EventTypes.ORDER_CONFIRMED,
            "1001",
            OrderCreatedPayload.builder()
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
                .build()
        ));
        when(processedEventService.claimEvent(any())).thenReturn(false);

        orderEventListener.onOrderEvent(message);

        verify(shipmentService, never()).startPreparation(any());
        verify(processedEventService).claimEvent(any());
    }
}
