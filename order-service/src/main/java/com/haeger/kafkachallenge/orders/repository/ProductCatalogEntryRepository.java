package com.haeger.kafkachallenge.orders.repository;

import com.haeger.kafkachallenge.orders.entity.ProductCatalogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCatalogEntryRepository extends JpaRepository<ProductCatalogEntry, Long> {
}
