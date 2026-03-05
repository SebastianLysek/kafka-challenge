package com.haeger.kafkachallenge.common.dto;

import java.math.BigDecimal;

public class OrderItemDto {
    private Long id;
    private Long orderId;
    private Long productId;
    private ProductDto product;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal total;
}
