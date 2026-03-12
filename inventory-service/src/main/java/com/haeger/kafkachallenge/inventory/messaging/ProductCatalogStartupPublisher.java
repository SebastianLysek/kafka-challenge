package com.haeger.kafkachallenge.inventory.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.messaging.product-catalog.startup.enabled", havingValue = "true", matchIfMissing = true)
public class ProductCatalogStartupPublisher {
    private final ProductCatalogEventPublisher productCatalogEventPublisher;

    @EventListener(ApplicationReadyEvent.class)
    public void publishInitialCatalogSnapshot() {
        productCatalogEventPublisher.publishCatalogSnapshot();
    }
}
