package com.haeger.kafkachallenge.inventory.repository;

import com.haeger.kafkachallenge.inventory.entity.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
}
