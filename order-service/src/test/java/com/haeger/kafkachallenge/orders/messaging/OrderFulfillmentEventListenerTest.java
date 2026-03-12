package com.haeger.kafkachallenge.orders.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
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
        IntegrationEvent<?> event = objectMapper.readValue(message, IntegrationEvent.class);
        when(processedEventService.hasProcessed(event.getEventId())).thenReturn(false);
        doNothing().when(processedEventService).markProcessed(any());

        orderFulfillmentEventListener.onOrderEvent(message);

        verify(orderService).confirmOrder(1001L);
        verify(processedEventService).markProcessed(any());
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
        IntegrationEvent<?> event = objectMapper.readValue(message, IntegrationEvent.class);
        when(processedEventService.hasProcessed(event.getEventId())).thenReturn(false);
        doNothing().when(processedEventService).markProcessed(any());

        orderFulfillmentEventListener.onOrderEvent(message);

        verify(orderService).declineOrder(1002L);
        verify(processedEventService).markProcessed(any());
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
        IntegrationEvent<?> event = objectMapper.readValue(message, IntegrationEvent.class);
        when(processedEventService.hasProcessed(event.getEventId())).thenReturn(true);

        orderFulfillmentEventListener.onOrderEvent(message);

        verify(orderService, never()).confirmOrder(anyLong());
        verify(orderService, never()).declineOrder(anyLong());
        verify(processedEventService, never()).markProcessed(any());
    }
}
