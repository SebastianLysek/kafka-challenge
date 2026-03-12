package com.haeger.kafkachallenge.inventory.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.common.events.OrderCreatedItemPayload;
import com.haeger.kafkachallenge.common.events.OrderCreatedPayload;
import com.haeger.kafkachallenge.inventory.service.OrderFulfillmentService;
import com.haeger.kafkachallenge.inventory.service.ProcessedEventService;
import java.util.List;
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
    private OrderFulfillmentService orderFulfillmentService;

    private OrderEventListener orderEventListener;

    @BeforeEach
    void setUp() {
        orderEventListener = new OrderEventListener(
            objectMapper,
            processedEventService,
            orderFulfillmentService
        );
    }

    @Test
    void orderCreatedEventTriggersFulfillmentCheck() throws Exception {
        OrderCreatedPayload order = OrderCreatedPayload.builder()
            .orderId(1001L)
            .items(List.of(
                OrderCreatedItemPayload.builder().productId(10L).quantity(2).build()
            ))
            .build();
        String message = objectMapper.writeValueAsString(IntegrationEvent.of(
            EventTypes.ORDER_CREATED,
            "1001",
            order
        ));
        IntegrationEvent<?> event = objectMapper.readValue(message, IntegrationEvent.class);
        when(processedEventService.hasProcessed(event.getEventId())).thenReturn(false);
        doNothing().when(processedEventService).markProcessed(any());

        orderEventListener.onOrderEvent(message);

        verify(orderFulfillmentService).handleOrderCreated(order);
        verify(processedEventService).markProcessed(any());
    }

    @Test
    void duplicateEventIsIgnored() throws Exception {
        String message = objectMapper.writeValueAsString(IntegrationEvent.of(
            EventTypes.ORDER_CREATED,
            "1001",
            OrderCreatedPayload.builder()
                .orderId(1001L)
                .items(List.of(
                    OrderCreatedItemPayload.builder().productId(10L).quantity(2).build()
                ))
                .build()
        ));
        IntegrationEvent<?> event = objectMapper.readValue(message, IntegrationEvent.class);
        when(processedEventService.hasProcessed(event.getEventId())).thenReturn(true);

        orderEventListener.onOrderEvent(message);

        verify(orderFulfillmentService, never()).handleOrderCreated(any());
        verify(processedEventService, never()).markProcessed(any());
    }
}
