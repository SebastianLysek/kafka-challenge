package com.haeger.kafkachallenge.customerrelations.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.haeger.kafkachallenge.common.dto.OrderStatus;
import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.common.events.OrderCreatedItemPayload;
import com.haeger.kafkachallenge.common.events.OrderCreatedPayload;
import com.haeger.kafkachallenge.customerrelations.service.CustomerNotificationService;
import com.haeger.kafkachallenge.customerrelations.service.ProcessedEventService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
    private CustomerNotificationService customerNotificationService;

    private OrderEventListener orderEventListener;

    @BeforeEach
    void setUp() {
        orderEventListener = new OrderEventListener(
            objectMapper,
            processedEventService,
            customerNotificationService
        );
    }

    @ParameterizedTest
    @MethodSource("supportedEventTypes")
    void supportedEventsSendNotification(String eventType) throws Exception {
        OrderCreatedPayload order = sampleOrder();
        String message = objectMapper.writeValueAsString(IntegrationEvent.of(eventType, "1001", order));
        IntegrationEvent<?> event = objectMapper.readValue(message, IntegrationEvent.class);

        when(processedEventService.hasProcessed(event.getEventId())).thenReturn(false);
        doNothing().when(processedEventService).markProcessed(any());

        orderEventListener.onOrderEvent(message);

        verify(customerNotificationService).sendOrderStatusUpdate(eventType, order);
        verify(processedEventService).markProcessed(any());
    }

    @Test
    void duplicateEventIsIgnored() throws Exception {
        String message = objectMapper.writeValueAsString(IntegrationEvent.of(
            EventTypes.ORDER_CONFIRMED,
            "1001",
            sampleOrder()
        ));
        IntegrationEvent<?> event = objectMapper.readValue(message, IntegrationEvent.class);
        when(processedEventService.hasProcessed(event.getEventId())).thenReturn(true);

        orderEventListener.onOrderEvent(message);

        verify(customerNotificationService, never()).sendOrderStatusUpdate(anyString(), any());
        verify(processedEventService, never()).markProcessed(any());
    }

    private static Stream<Arguments> supportedEventTypes() {
        return Stream.of(
            Arguments.of(EventTypes.ORDER_CREATED),
            Arguments.of(EventTypes.ORDER_CONFIRMED),
            Arguments.of(EventTypes.ORDER_DECLINED),
            Arguments.of(EventTypes.SHIPMENT_PREPARATION_STARTED),
            Arguments.of(EventTypes.ORDER_SHIPPED)
        );
    }

    private OrderCreatedPayload sampleOrder() {
        return OrderCreatedPayload.builder()
            .orderId(1001L)
            .customerId(42L)
            .customerEmail("max@example.com")
            .customerFullName("Max Mustermann")
            .status(OrderStatus.CONFIRMED)
            .currency("EUR")
            .total(new BigDecimal("449.98"))
            .notes("Leave package at the side entrance.")
            .createdAt(Instant.parse("2026-03-13T19:00:00Z"))
            .updatedAt(Instant.parse("2026-03-13T19:05:00Z"))
            .items(List.of(
                OrderCreatedItemPayload.builder()
                    .productId(10L)
                    .productSku("CPU-AMD-7800X3D")
                    .productName("AMD Ryzen 7 7800X3D")
                    .quantity(1)
                    .unitPrice(new BigDecimal("449.98"))
                    .lineTotal(new BigDecimal("449.98"))
                    .build()
            ))
            .build();
    }
}
