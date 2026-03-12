package com.haeger.kafkachallenge.common.events;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCatalogProduct {
    private Long productId;
    private String sku;
    private String name;
    private BigDecimal price;
}
