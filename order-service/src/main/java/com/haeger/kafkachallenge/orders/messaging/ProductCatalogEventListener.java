package com.haeger.kafkachallenge.orders.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.common.events.KafkaTopics;
import com.haeger.kafkachallenge.common.events.ProductCatalogSnapshotPayload;
import com.haeger.kafkachallenge.common.events.ProductUpsertedPayload;
import com.haeger.kafkachallenge.orders.service.ProcessedEventService;
import com.haeger.kafkachallenge.orders.service.ProductCatalogProjectionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductCatalogEventListener {
    private static final TypeReference<IntegrationEvent<JsonNode>> EVENT_TYPE = new TypeReference<>() { };
    private static final Logger LOG = LoggerFactory.getLogger(ProductCatalogEventListener.class);

    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;
    private final ProductCatalogProjectionService productCatalogProjectionService;

    @KafkaListener(topics = KafkaTopics.PRODUCT_CATALOG)
    @Transactional
    public void onCatalogEvent(String message) {
        try {
            IntegrationEvent<JsonNode> event = objectMapper.readValue(message, EVENT_TYPE);

            if (event.getEventId() == null || event.getEventId().isBlank()) {
                throw new IllegalArgumentException("Catalog event is missing eventId");
            }
            if (!processedEventService.claimEvent(event)) {
                LOG.info("Skipping duplicate {} event {}", event.getType(), event.getEventId());
                return;
            }

            switch (event.getType()) {
                case EventTypes.PRODUCT_CATALOG_SNAPSHOT -> handleSnapshot(event.getPayload());
                case EventTypes.PRODUCT_UPSERTED -> handleUpsert(event.getPayload());
                default -> LOG.debug("Ignoring unsupported catalog event type {}", event.getType());
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to deserialize catalog event", ex);
        }
    }

    private void handleSnapshot(JsonNode payload) throws JsonProcessingException {
        ProductCatalogSnapshotPayload snapshotPayload = objectMapper.treeToValue(payload, ProductCatalogSnapshotPayload.class);
        productCatalogProjectionService.replaceCatalog(snapshotPayload.getProducts());
        LOG.info("Applied {} with {} products", EventTypes.PRODUCT_CATALOG_SNAPSHOT, snapshotPayload.getProducts().size());
    }

    private void handleUpsert(JsonNode payload) throws JsonProcessingException {
        ProductUpsertedPayload upsertedPayload = objectMapper.treeToValue(payload, ProductUpsertedPayload.class);
        productCatalogProjectionService.upsertProduct(upsertedPayload.getProduct());
        LOG.info("Applied {} for product {}", EventTypes.PRODUCT_UPSERTED, upsertedPayload.getProduct().getProductId());
    }
}
