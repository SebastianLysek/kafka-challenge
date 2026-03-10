package com.haeger.kafkachallenge.orders.service;

import com.haeger.kafkachallenge.common.events.ProductCatalogProduct;
import com.haeger.kafkachallenge.orders.entity.ProductCatalogEntry;
import com.haeger.kafkachallenge.orders.repository.ProductCatalogEntryRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductCatalogProjectionService {
    private final ProductCatalogEntryRepository productCatalogEntryRepository;

    @Transactional
    public void replaceCatalog(List<ProductCatalogProduct> products) {
        productCatalogEntryRepository.deleteAllInBatch();
        productCatalogEntryRepository.saveAll(products.stream()
            .map(this::toEntry)
            .toList());
    }

    @Transactional
    public void upsertProduct(ProductCatalogProduct product) {
        productCatalogEntryRepository.save(toEntry(product));
    }

    @Transactional(readOnly = true)
    public List<ProductCatalogEntry> findAll() {
        return productCatalogEntryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ProductCatalogEntry> findByProductIds(Collection<Long> productIds) {
        return productCatalogEntryRepository.findAllById(productIds);
    }

    private ProductCatalogEntry toEntry(ProductCatalogProduct product) {
        return ProductCatalogEntry.builder()
            .productId(product.getProductId())
            .sku(product.getSku())
            .name(product.getName())
            .price(product.getPrice())
            .updatedAt(Instant.now())
            .build();
    }
}
