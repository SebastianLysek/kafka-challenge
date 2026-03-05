package com.haeger.kafkachallenge.common.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class OrderDto {
    private Long id;
    private Long customerId;
    private CustomerDto customer;
    private OrderStatus status;
    private String currency;
    private BigDecimal total;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private Set<OrderItemDto> items = new HashSet<>();
}
