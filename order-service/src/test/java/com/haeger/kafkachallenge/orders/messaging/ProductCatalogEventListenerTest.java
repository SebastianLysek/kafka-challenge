package com.haeger.kafkachallenge.orders.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.common.events.KafkaTopics;
import com.haeger.kafkachallenge.common.events.ProductCatalogProduct;
import com.haeger.kafkachallenge.common.events.ProductCatalogSnapshotPayload;
import com.haeger.kafkachallenge.common.events.ProductUpsertedPayload;
import com.haeger.kafkachallenge.orders.service.ProcessedEventService;
import com.haeger.kafkachallenge.orders.service.ProductCatalogProjectionService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductCatalogEventListenerTest {

    private final JsonMapper objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build();

    @Mock
    private ProductCatalogProjectionService productCatalogProjectionService;

    @Mock
    private ProcessedEventService processedEventService;

    private ProductCatalogEventListener productCatalogEventListener;

    @BeforeEach
    void setUp() {
        productCatalogEventListener = new ProductCatalogEventListener(
            objectMapper,
            processedEventService,
            productCatalogProjectionService
        );
    }

    @Test
    void snapshotEventReplacesLocalCatalog() throws Exception {
        ProductCatalogProduct product = ProductCatalogProduct.builder()
            .productId(10L)
            .sku("CPU-AMD-7800X3D")
            .name("AMD Ryzen 7 7800X3D")
            .price(new BigDecimal("399.99"))
            .build();
        String message = objectMapper.writeValueAsString(IntegrationEvent.of(
            EventTypes.PRODUCT_CATALOG_SNAPSHOT,
            KafkaTopics.PRODUCT_CATALOG,
            ProductCatalogSnapshotPayload.builder()
                .products(List.of(product))
                .build()
        ));
        IntegrationEvent<?> event = objectMapper.readValue(message, IntegrationEvent.class);
        when(processedEventService.hasProcessed(event.getEventId())).thenReturn(false);
        doNothing().when(processedEventService).markProcessed(any());

        productCatalogEventListener.onCatalogEvent(message);

        verify(productCatalogProjectionService).replaceCatalog(List.of(product));
        verify(processedEventService).markProcessed(any());
        verify(productCatalogProjectionService, never()).upsertProduct(any());
    }

    @Test
    void upsertEventUpdatesSingleProduct() throws Exception {
        ProductCatalogProduct product = ProductCatalogProduct.builder()
            .productId(16L)
            .sku("STORAGE-SSD-1TB-NVME")
            .name("Samsung 990 EVO 1TB")
            .price(new BigDecimal("89.99"))
            .build();
        String message = objectMapper.writeValueAsString(IntegrationEvent.of(
            EventTypes.PRODUCT_UPSERTED,
            product.getProductId().toString(),
            ProductUpsertedPayload.builder()
                .product(product)
                .build()
        ));
        IntegrationEvent<?> event = objectMapper.readValue(message, IntegrationEvent.class);
        when(processedEventService.hasProcessed(event.getEventId())).thenReturn(false);
        doNothing().when(processedEventService).markProcessed(any());

        productCatalogEventListener.onCatalogEvent(message);

        verify(productCatalogProjectionService).upsertProduct(product);
        verify(processedEventService).markProcessed(any());
        verify(productCatalogProjectionService, never()).replaceCatalog(any());
    }

    @Test
    void duplicateEventIsIgnored() throws Exception {
        ProductCatalogProduct product = ProductCatalogProduct.builder()
            .productId(16L)
            .sku("STORAGE-SSD-1TB-NVME")
            .name("Samsung 990 EVO 1TB")
            .price(new BigDecimal("89.99"))
            .build();
        String message = objectMapper.writeValueAsString(IntegrationEvent.of(
            EventTypes.PRODUCT_UPSERTED,
            product.getProductId().toString(),
            ProductUpsertedPayload.builder()
                .product(product)
                .build()
        ));
        IntegrationEvent<?> event = objectMapper.readValue(message, IntegrationEvent.class);
        when(processedEventService.hasProcessed(event.getEventId())).thenReturn(true);

        productCatalogEventListener.onCatalogEvent(message);

        verify(productCatalogProjectionService, never()).replaceCatalog(any());
        verify(productCatalogProjectionService, never()).upsertProduct(any());
        verify(processedEventService, never()).markProcessed(any());
    }
}
