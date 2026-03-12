package com.haeger.kafkachallenge.common.events;

import com.haeger.kafkachallenge.common.dto.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedPayload {
    private Long orderId;
    private Long customerId;
    private String customerEmail;
    private String customerFullName;
    private OrderStatus status;
    private String currency;
    private BigDecimal total;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private List<OrderCreatedItemPayload> items = new ArrayList<>();
}
