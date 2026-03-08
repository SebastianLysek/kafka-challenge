package com.haeger.kafkachallenge.inventory.service;

import com.haeger.kafkachallenge.inventory.entity.ProductCategory;
import com.haeger.kafkachallenge.inventory.repository.ProductCategoryRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductCategoryService {
    private final ProductCategoryRepository productCategoryRepository;

    /**
     * Checks if there are any product categories available in the repository.
     * Note: Used for seeding purposes.
     *
     * @return {@code true} if at least one product category exists, otherwise {@code false}.
     */
    @Transactional(readOnly = true)
    public boolean hasCategories() {
        return productCategoryRepository.count() > 0;
    }

    /**
     * Retrieves a product category based on its unique code.
     * Note: Used for seeding purposes.
     *
     * @param code the unique code of the product category to be retrieved
     * @return an {@code Optional} containing the {@code ProductCategory} if found,
     *         or an empty {@code Optional} if no category is found with the given code
     */
    @Transactional(readOnly = true)
    public Optional<ProductCategory> findByCode(String code) {
        return productCategoryRepository.findByCode(code);
    }

    @Transactional
    public ProductCategory save(ProductCategory category) {
        return productCategoryRepository.save(category);
    }

    @Transactional
    public List<ProductCategory> saveAll(List<ProductCategory> categories) {
        return productCategoryRepository.saveAll(categories);
    }
}
