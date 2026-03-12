package com.haeger.kafkachallenge.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderFulfillmentCheckItem {
    private Long productId;
    private String productSku;
    private String productName;
    private Integer requestedQuantity;
    private Integer availableQuantity;
}
