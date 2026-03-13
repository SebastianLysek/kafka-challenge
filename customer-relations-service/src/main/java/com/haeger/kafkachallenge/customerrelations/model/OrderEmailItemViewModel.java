package com.haeger.kafkachallenge.customerrelations.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEmailItemViewModel {
    private String productSku;
    private String productName;
    private Integer quantity;
    private String unitPrice;
    private String lineTotal;
}
