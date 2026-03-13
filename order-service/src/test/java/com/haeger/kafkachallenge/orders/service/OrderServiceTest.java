package com.haeger.kafkachallenge.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.haeger.kafkachallenge.common.dto.OrderDto;
import com.haeger.kafkachallenge.common.dto.OrderItemDto;
import com.haeger.kafkachallenge.common.dto.OrderStatus;
import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.common.events.KafkaTopics;
import com.haeger.kafkachallenge.orders.entity.Order;
import com.haeger.kafkachallenge.orders.entity.OrderItem;
import com.haeger.kafkachallenge.orders.entity.ProductCatalogEntry;
import com.haeger.kafkachallenge.orders.repository.OrderRepository;
import com.haeger.kafkachallenge.orders.util.mapper.PojoMapper;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductCatalogProjectionService productCatalogProjectionService;

    @Mock
    private OutboxService outboxService;

    @Spy
    private PojoMapper pojoMapper;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrderBuildsSnapshotsAndEnqueuesOrderCreated() {
        OrderDto request = OrderDto.builder()
            .customerId(42L)
            .customerEmail("max@example.com")
            .customerFullName("Max Mustermann")
            .currency("EUR")
            .notes("Ring the bell")
            .items(new LinkedHashSet<>(List.of(
                OrderItemDto.builder().productId(10L).quantity(2).build(),
                OrderItemDto.builder().productId(16L).quantity(1).build()
            )))
            .build();

        when(productCatalogProjectionService.findByProductIds(List.of(10L, 16L))).thenReturn(List.of(
            ProductCatalogEntry.builder()
                .productId(10L)
                .sku("CPU-AMD-7800X3D")
                .name("AMD Ryzen 7 7800X3D")
                .price(new BigDecimal("399.99"))
                .build(),
            ProductCatalogEntry.builder()
                .productId(16L)
                .sku("STORAGE-SSD-1TB-NVME")
                .name("Samsung 990 EVO 1TB")
                .price(new BigDecimal("89.99"))
                .build()
        ));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1001L);
            AtomicLong ids = new AtomicLong(1L);
            order.getItems().forEach(item -> item.setId(ids.getAndIncrement()));
            return order;
        });

        OrderDto result = orderService.createOrder(request);

        assertThat(result.getId()).isEqualTo(1001L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.getTotal()).isEqualByComparingTo("889.97");
        assertThat(result.getItems())
            .extracting(OrderItemDto::getProductSku)
            .containsExactly("CPU-AMD-7800X3D", "STORAGE-SSD-1TB-NVME");

        ArgumentCaptor<IntegrationEvent> eventCaptor = ArgumentCaptor.forClass(IntegrationEvent.class);
        verify(outboxService).enqueue(eq(KafkaTopics.ORDERS), eventCaptor.capture());
        IntegrationEvent<?> event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo(EventTypes.ORDER_CREATED);
        assertThat(event.getReferenceId()).isEqualTo("1001");
        assertThat(event.getEventId()).isNotBlank();
    }

    @Test
    void createOrderFailsWhenProductIsUnknown() {
        OrderDto request = OrderDto.builder()
            .customerId(42L)
            .customerEmail("max@example.com")
            .customerFullName("Max Mustermann")
            .currency("EUR")
            .items(new LinkedHashSet<>(List.of(
                OrderItemDto.builder().productId(99L).quantity(1).build()
            )))
            .build();

        when(productCatalogProjectionService.findByProductIds(List.of(99L))).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Unknown productId 99");

        verify(orderRepository, never()).save(any(Order.class));
        verify(outboxService, never()).enqueue(any(), any());
    }

    @Test
    void listOrdersReturnsMappedDtos() {
        Order order = sampleOrder(1001L, 42L);
        when(orderRepository.findAll()).thenReturn(List.of(order));

        List<OrderDto> result = orderService.listOrders();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1001L);
        assertThat(result.getFirst().getItems()).hasSize(1);
        assertThat(result.getFirst().getItems().iterator().next().getProductSku()).isEqualTo("CPU-AMD-7800X3D");
    }

    @Test
    void listOrdersByCustomerIdReturnsFilteredDtos() {
        Order order = sampleOrder(1002L, 99L);
        when(orderRepository.findAllByCustomerId(99L)).thenReturn(List.of(order));

        List<OrderDto> result = orderService.listOrdersByCustomerId(99L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getCustomerId()).isEqualTo(99L);
        assertThat(result.getFirst().getId()).isEqualTo(1002L);
    }

    @Test
    void confirmOrderUpdatesStatusAndEnqueuesOrderConfirmed() {
        Order order = sampleOrder(1003L, 42L);
        Instant originalUpdatedAt = order.getUpdatedAt();
        when(orderRepository.findById(1003L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.confirmOrder(1003L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getUpdatedAt()).isAfter(originalUpdatedAt);

        ArgumentCaptor<IntegrationEvent> eventCaptor = ArgumentCaptor.forClass(IntegrationEvent.class);
        verify(outboxService).enqueue(eq(KafkaTopics.ORDERS), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getType()).isEqualTo(EventTypes.ORDER_CONFIRMED);
    }

    @Test
    void declineOrderUpdatesStatusAndEnqueuesOrderDeclined() {
        Order order = sampleOrder(1004L, 42L);
        when(orderRepository.findById(1004L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.declineOrder(1004L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DECLINED);

        ArgumentCaptor<IntegrationEvent> eventCaptor = ArgumentCaptor.forClass(IntegrationEvent.class);
        verify(outboxService).enqueue(eq(KafkaTopics.ORDERS), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getType()).isEqualTo(EventTypes.ORDER_DECLINED);
    }

    @Test
    void startShipmentPreparationUpdatesStatusWithoutPublishingEvent() {
        Order order = sampleOrder(1005L, 42L);
        order.setStatus(OrderStatus.CONFIRMED);
        Instant originalUpdatedAt = order.getUpdatedAt();
        when(orderRepository.findById(1005L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.startShipmentPreparation(1005L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREP_STARTED);
        assertThat(order.getUpdatedAt()).isAfter(originalUpdatedAt);
        verify(outboxService, never()).enqueue(any(), any());
    }

    @Test
    void markShippedUpdatesStatusWithoutPublishingEvent() {
        Order order = sampleOrder(1006L, 42L);
        order.setStatus(OrderStatus.PREP_STARTED);
        when(orderRepository.findById(1006L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.markShipped(1006L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        verify(outboxService, never()).enqueue(any(), any());
    }

    private Order sampleOrder(Long orderId, Long customerId) {
        Order order = Order.builder()
            .id(orderId)
            .customerId(customerId)
            .customerEmail("max@example.com")
            .customerFullName("Max Mustermann")
            .status(OrderStatus.CREATED)
            .currency("EUR")
            .total(new BigDecimal("799.98"))
            .createdAt(Instant.parse("2026-03-08T19:00:00Z"))
            .updatedAt(Instant.parse("2026-03-08T19:00:00Z"))
            .build();
        order.addItem(OrderItem.builder()
            .id(1L)
            .productId(10L)
            .productSku("CPU-AMD-7800X3D")
            .productName("AMD Ryzen 7 7800X3D")
            .unitPrice(new BigDecimal("399.99"))
            .quantity(2)
            .lineTotal(new BigDecimal("799.98"))
            .build());
        return order;
    }
}
