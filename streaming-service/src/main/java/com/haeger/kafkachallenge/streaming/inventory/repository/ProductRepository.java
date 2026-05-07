package com.haeger.kafkachallenge.streaming.inventory.repository;

import com.haeger.kafkachallenge.streaming.inventory.entity.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = "category")
    Optional<Product> findBySku(String sku);
}
