package com.haeger.kafkachallenge.inventory.repository;

import com.haeger.kafkachallenge.inventory.entity.ProductCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Integer> {
    Optional<ProductCategory> findByCode(String code);
}
