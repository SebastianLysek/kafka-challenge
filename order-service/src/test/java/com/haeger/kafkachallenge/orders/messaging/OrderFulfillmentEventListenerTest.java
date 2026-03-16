package com.haeger.kafkachallenge.orders.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.common.events.OrderCreatedPayload;
import com.haeger.kafkachallenge.common.events.OrderFulfillmentCheckResultPayload;
import com.haeger.kafkachallenge.orders.service.OrderService;
import com.haeger.kafkachallenge.orders.service.ProcessedEventService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderFulfillmentEventListenerTest {

    private final JsonMapper objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build();

    @Mock
    private ProcessedEventService processedEventService;

    @Mock
    private OrderService orderService;

    private OrderFulfillmentEventListener orderFulfillmentEventListener;

    @BeforeEach
    void setUp() {
        orderFulfillmentEventListener = new OrderFulfillmentEventListener(
            objectMapper,
            processedEventService,
            orderService
        );
    }

    @Test
    void successEventConfirmsOrder() throws Exception {
        String message = objectMapper.writeValueAsString(IntegrationEvent.of(
            EventTypes.ORDER_FULFILLMENT_CHECK_SUCCEEDED,
            "1001",
            OrderFulfillmentCheckResultPayload.builder()
                .orderId(1001L)
                .message("Inventory reserved for order")
                .items(List.of())
                .build()
        ));
        when(processedEventService.claimEvent(any())).thenReturn(true);

        orderFulfillmentEventListener.onOrderEvent(message);

        verify(orderService).confirmOrder(1001L);
        verify(processedEventService).claimEvent(any());
    }

    @Test
    void failedEventDeclinesOrder() throws Exception {
        String message = objectMapper.writeValueAsString(IntegrationEvent.of(
            EventTypes.ORDER_FULFILLMENT_CHECK_FAILED,
            "1002",
            OrderFulfillmentCheckResultPayload.builder()
                .orderId(1002L)
                .message("Insufficient inventory")
                .items(List.of())
                .build()
        ));
        when(processedEventService.claimEvent(any())).thenReturn(true);

        orderFulfillmentEventListener.onOrderEvent(message);

        verify(orderService).declineOrder(1002L);
        verify(processedEventService).claimEvent(any());
    }

    @Test
    void shipmentPreparationStartedEventUpdatesOrderStatus() throws Exception {
        String message = objectMapper.writeValueAsString(IntegrationEvent.of(
            EventTypes.SHIPMENT_PREPARATION_STARTED,
            "1003",
            OrderCreatedPayload.builder()
                .orderId(1003L)
                .customerId(42L)
                .customerEmail("max@example.com")
                .customerFullName("Max Mustermann")
                .items(List.of())
                .build()
        ));
        when(processedEventService.claimEvent(any())).thenReturn(true);

        orderFulfillmentEventListener.onOrderEvent(message);

        verify(orderService).startShipmentPreparation(1003L);
        verify(processedEventService).claimEvent(any());
    }

    @Test
    void orderShippedEventUpdatesOrderStatus() throws Exception {
        String message = objectMapper.writeValueAsString(IntegrationEvent.of(
            EventTypes.ORDER_SHIPPED,
            "1004",
            OrderCreatedPayload.builder()
                .orderId(1004L)
                .customerId(42L)
                .customerEmail("max@example.com")
                .customerFullName("Max Mustermann")
                .items(List.of())
                .build()
        ));
        when(processedEventService.claimEvent(any())).thenReturn(true);

        orderFulfillmentEventListener.onOrderEvent(message);

        verify(orderService).markShipped(1004L);
        verify(processedEventService).claimEvent(any());
    }

    @Test
    void duplicateEventIsIgnored() throws Exception {
        String message = objectMapper.writeValueAsString(IntegrationEvent.of(
            EventTypes.ORDER_FULFILLMENT_CHECK_SUCCEEDED,
            "1001",
            OrderFulfillmentCheckResultPayload.builder()
                .orderId(1001L)
                .items(List.of())
                .build()
        ));
        when(processedEventService.claimEvent(any())).thenReturn(false);

        orderFulfillmentEventListener.onOrderEvent(message);

        verify(orderService, never()).confirmOrder(anyLong());
        verify(orderService, never()).declineOrder(anyLong());
        verify(processedEventService).claimEvent(any());
    }
}
