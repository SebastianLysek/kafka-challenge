package com.haeger.kafkachallenge.inventory.service;

import com.haeger.kafkachallenge.inventory.entity.Product;
import com.haeger.kafkachallenge.inventory.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    /**
     * Checks if there are any products available in the repository.
     * Note: Used for seeding purposes.
     *
     * @return {@code true} if at least one product exists, otherwise {@code false}.
     */
    @Transactional(readOnly = true)
    public boolean hasProducts() {
        return productRepository.count() > 0;
    }

    /**
     * Retrieves a product based on its unique SKU (Stock Keeping Unit).
     * Note: Used for seeding purposes.
     *
     * @param sku the unique SKU associated with the product to be retrieved
     * @return an {@code Optional} containing the {@code Product} if found,
     *         or an empty {@code Optional} if no product is found with the given SKU
     */
    @Transactional(readOnly = true)
    public Optional<Product> findBySku(String sku) {
        return productRepository.findBySku(sku);
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Transactional
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public List<Product> saveAll(List<Product> products) {
        return productRepository.saveAll(products);
    }
}
