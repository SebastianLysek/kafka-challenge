package com.haeger.kafkachallenge.inventory.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.common.events.KafkaTopics;
import com.haeger.kafkachallenge.common.events.ProductCatalogProduct;
import com.haeger.kafkachallenge.common.events.ProductCatalogSnapshotPayload;
import com.haeger.kafkachallenge.inventory.entity.Product;
import com.haeger.kafkachallenge.inventory.service.ProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductCatalogEventPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(ProductCatalogEventPublisher.class);

    private final ProductService productService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishCatalogSnapshot() {
        List<ProductCatalogProduct> products = productService.findAll().stream()
            .map(this::toCatalogProduct)
            .toList();

        IntegrationEvent<ProductCatalogSnapshotPayload> event = IntegrationEvent.of(
            EventTypes.PRODUCT_CATALOG_SNAPSHOT,
            KafkaTopics.PRODUCT_CATALOG,
            ProductCatalogSnapshotPayload.builder()
                .products(products)
                .build()
        );

        kafkaTemplate.send(KafkaTopics.PRODUCT_CATALOG, toJson(event));
        LOG.info("Published {} with {} products", EventTypes.PRODUCT_CATALOG_SNAPSHOT, products.size());
    }

    private ProductCatalogProduct toCatalogProduct(Product product) {
        return ProductCatalogProduct.builder()
            .productId(product.getId())
            .sku(product.getSku())
            .name(product.getName())
            .price(product.getPrice())
            .build();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize product catalog event", ex);
        }
    }
}
