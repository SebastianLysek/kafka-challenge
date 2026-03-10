package com.haeger.kafkachallenge.orders.util.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.haeger.kafkachallenge.common.dto.OrderDto;
import com.haeger.kafkachallenge.common.dto.OrderItemDto;
import com.haeger.kafkachallenge.common.dto.OrderStatus;
import com.haeger.kafkachallenge.orders.entity.Order;
import com.haeger.kafkachallenge.orders.entity.OrderItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class PojoMapperTest {

    private final PojoMapper pojoMapper = new PojoMapper();

    @Test
    void toOrderDtoMapsOrderAndItems() {
        Order order = Order.builder()
            .id(1001L)
            .customerId(42L)
            .customerEmail("max@example.com")
            .customerFullName("Max Mustermann")
            .status(OrderStatus.CREATED)
            .currency("EUR")
            .total(new BigDecimal("889.97"))
            .notes("Ring the bell")
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

        OrderDto dto = pojoMapper.toOrderDto(order);

        assertThat(dto.getId()).isEqualTo(1001L);
        assertThat(dto.getItems()).hasSize(1);
        OrderItemDto itemDto = dto.getItems().iterator().next();
        assertThat(itemDto.getOrderId()).isEqualTo(1001L);
        assertThat(itemDto.getProductSku()).isEqualTo("CPU-AMD-7800X3D");
    }

    @Test
    void toOrderEntityMapsOrderAndItems() {
        OrderDto dto = OrderDto.builder()
            .id(1001L)
            .customerId(42L)
            .customerEmail("max@example.com")
            .customerFullName("Max Mustermann")
            .status(OrderStatus.CREATED)
            .currency("EUR")
            .total(new BigDecimal("889.97"))
            .notes("Ring the bell")
            .createdAt(Instant.parse("2026-03-08T19:00:00Z"))
            .updatedAt(Instant.parse("2026-03-08T19:00:00Z"))
            .items(new LinkedHashSet<>(List.of(
                OrderItemDto.builder()
                    .id(1L)
                    .orderId(1001L)
                    .productId(10L)
                    .productSku("CPU-AMD-7800X3D")
                    .productName("AMD Ryzen 7 7800X3D")
                    .unitPrice(new BigDecimal("399.99"))
                    .quantity(2)
                    .lineTotal(new BigDecimal("799.98"))
                    .build()
            )))
            .build();

        Order entity = pojoMapper.toOrderEntity(dto);

        assertThat(entity.getId()).isEqualTo(1001L);
        assertThat(entity.getCustomerId()).isEqualTo(42L);
        assertThat(entity.getItems()).hasSize(1);
        OrderItem item = entity.getItems().getFirst();
        assertThat(item.getOrder()).isSameAs(entity);
        assertThat(item.getLineTotal()).isEqualByComparingTo("799.98");
    }
}
