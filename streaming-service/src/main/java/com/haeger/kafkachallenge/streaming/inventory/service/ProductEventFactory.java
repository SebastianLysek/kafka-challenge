package com.haeger.kafkachallenge.streaming.inventory.service;

import com.haeger.kafkachallenge.streaming.inventory.entity.InventoryItem;
import com.haeger.kafkachallenge.streaming.proto.ProductUpserted;
import org.springframework.stereotype.Component;

@Component
public class ProductEventFactory {
    public ProductUpserted toProductUpserted(InventoryItem item) {
        return ProductUpserted.newBuilder()
            .setInventoryItemId(item.getId())
            .setProductId(item.getProduct().getId())
            .setSku(item.getProduct().getSku())
            .setName(item.getProduct().getName())
            .setCurrency("EUR")
            .setPrice(item.getProduct().getPrice().toPlainString())
            .setQuantity(item.getQuantity())
            .build();
    }
}
