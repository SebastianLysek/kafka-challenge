package com.haeger.kafkachallenge.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.common.events.KafkaTopics;
import com.haeger.kafkachallenge.common.events.OrderCreatedItemPayload;
import com.haeger.kafkachallenge.common.events.OrderCreatedPayload;
import com.haeger.kafkachallenge.common.events.OrderFulfillmentCheckResultPayload;
import com.haeger.kafkachallenge.inventory.entity.InventoryItem;
import com.haeger.kafkachallenge.inventory.entity.Product;
import com.haeger.kafkachallenge.inventory.repository.InventoryItemRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderFulfillmentServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private OrderFulfillmentService orderFulfillmentService;

    @Test
    void handleOrderCreatedReservesInventoryAndPublishesSucceededEvent() {
        OrderCreatedPayload order = OrderCreatedPayload.builder()
            .orderId(1001L)
            .items(List.of(
                OrderCreatedItemPayload.builder()
                    .productId(10L)
                    .productSku("CPU-AMD-7800X3D")
                    .productName("AMD Ryzen 7 7800X3D")
                    .quantity(2)
                    .build(),
                OrderCreatedItemPayload.builder()
                    .productId(16L)
                    .productSku("STORAGE-SSD-1TB-NVME")
                    .productName("Samsung 990 EVO 1TB")
                    .quantity(1)
                    .build()
            ))
            .build();
        InventoryItem cpuInventory = InventoryItem.builder()
            .id(1L)
            .product(Product.builder().id(10L).build())
            .quantity(5)
            .build();
        InventoryItem storageInventory = InventoryItem.builder()
            .id(2L)
            .product(Product.builder().id(16L).build())
            .quantity(3)
            .build();
        when(inventoryItemRepository.findAllByProduct_IdIn(java.util.Set.of(10L, 16L)))
            .thenReturn(List.of(cpuInventory, storageInventory));

        orderFulfillmentService.handleOrderCreated(order);

        assertThat(cpuInventory.getQuantity()).isEqualTo(3);
        assertThat(storageInventory.getQuantity()).isEqualTo(2);

        ArgumentCaptor<IntegrationEvent> eventCaptor = ArgumentCaptor.forClass(IntegrationEvent.class);
        verify(outboxService).enqueue(eq(KafkaTopics.ORDERS), eventCaptor.capture());
        IntegrationEvent<?> event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo(EventTypes.ORDER_FULFILLMENT_CHECK_SUCCEEDED);
        assertThat(event.getReferenceId()).isEqualTo("1001");

        OrderFulfillmentCheckResultPayload payload = (OrderFulfillmentCheckResultPayload) event.getPayload();
        assertThat(payload.getOrderId()).isEqualTo(1001L);
        assertThat(payload.getItems()).hasSize(2);
    }

    @Test
    void handleOrderCreatedPublishesFailedEventWhenInventoryIsInsufficient() {
        OrderCreatedPayload order = OrderCreatedPayload.builder()
            .orderId(1002L)
            .items(List.of(
                OrderCreatedItemPayload.builder()
                    .productId(10L)
                    .productSku("CPU-AMD-7800X3D")
                    .productName("AMD Ryzen 7 7800X3D")
                    .quantity(8)
                    .build()
            ))
            .build();
        InventoryItem cpuInventory = InventoryItem.builder()
            .id(1L)
            .product(Product.builder().id(10L).build())
            .quantity(5)
            .build();
        when(inventoryItemRepository.findAllByProduct_IdIn(java.util.Set.of(10L))).thenReturn(List.of(cpuInventory));

        orderFulfillmentService.handleOrderCreated(order);

        assertThat(cpuInventory.getQuantity()).isEqualTo(5);

        ArgumentCaptor<IntegrationEvent> eventCaptor = ArgumentCaptor.forClass(IntegrationEvent.class);
        verify(outboxService).enqueue(eq(KafkaTopics.ORDERS), eventCaptor.capture());
        IntegrationEvent<?> event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo(EventTypes.ORDER_FULFILLMENT_CHECK_FAILED);

        OrderFulfillmentCheckResultPayload payload = (OrderFulfillmentCheckResultPayload) event.getPayload();
        assertThat(payload.getOrderId()).isEqualTo(1002L);
        assertThat(payload.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductId()).isEqualTo(10L);
            assertThat(item.getRequestedQuantity()).isEqualTo(8);
            assertThat(item.getAvailableQuantity()).isEqualTo(5);
        });
    }
}
