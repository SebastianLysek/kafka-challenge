package com.haeger.kafkachallenge.inventory.util.mapper;

import com.haeger.kafkachallenge.common.dto.InventoryItemDto;
import com.haeger.kafkachallenge.common.dto.ProductCategoryDto;
import com.haeger.kafkachallenge.common.dto.ProductDto;
import com.haeger.kafkachallenge.inventory.entity.InventoryItem;
import com.haeger.kafkachallenge.inventory.entity.Product;
import com.haeger.kafkachallenge.inventory.entity.ProductCategory;
import com.haeger.kafkachallenge.inventory.repository.ProductCategoryRepository;
import com.haeger.kafkachallenge.inventory.repository.ProductRepository;
import org.springframework.stereotype.Component;

@Component
public class PojoMapper {
    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;

    public PojoMapper(ProductRepository productRepository, ProductCategoryRepository productCategoryRepository) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
    }

    /**
     * Converts an {@code InventoryItem} entity to an {@code InventoryItemDto}.
     * If the input entity is {@code null}, this method returns {@code null}.
     *
     * @param entity the {@code InventoryItem} entity to convert
     * @return the {@code InventoryItemDto} representation of the entity, or {@code null} if the input entity is {@code null}
     */
    public InventoryItemDto toInventoryItemDto(InventoryItem entity) {
        if (entity == null) {
            return null;
        }
        return InventoryItemDto.builder()
            .id(entity.getId())
            .quantity(entity.getQuantity())
            .product(toProductDto(entity.getProduct()))
            .build();
    }

    /**
     * Converts a {@code Product} entity to a {@code ProductDto}.
     * If the input entity is {@code null}, this method returns {@code null}.
     *
     * @param entity the {@code Product} entity to convert
     * @return the {@code ProductDto} representation of the entity, or {@code null} if the input entity is {@code null}
     */
    public ProductDto toProductDto(Product entity) {
        if (entity == null) {
            return null;
        }
        return ProductDto.builder()
            .id(entity.getId())
            .sku(entity.getSku())
            .name(entity.getName())
            .price(entity.getPrice())
            .specs(entity.getSpecs())
            .category(toProductCategoryDto(entity.getCategory()))
            .build();
    }

    /**
     * Converts a {@code ProductCategory} entity to a {@code ProductCategoryDto}.
     * If the input entity is {@code null}, this method returns {@code null}.
     *
     * @param entity the {@code ProductCategory} entity to convert
     * @return the {@code ProductCategoryDto} representation of the entity, or {@code null} if the input entity is {@code null}
     */
    public ProductCategoryDto toProductCategoryDto(ProductCategory entity) {
        if (entity == null) {
            return null;
        }
        return ProductCategoryDto.builder()
            .id(entity.getId())
            .code(entity.getCode())
            .name(entity.getName())
            .sortOrder(entity.getSortOrder())
            .build();
    }

    /**
     * Converts an {@code InventoryItemDto} to an {@code InventoryItem} entity.
     * If the input DTO is {@code null}, this method returns {@code null}.
     *
     * @param dto the {@code InventoryItemDto} containing data to map to an {@code InventoryItem} entity
     * @return the {@code InventoryItem} entity mapped from the provided DTO, or {@code null} if the input DTO is {@code null}
     */
    public InventoryItem toInventoryItemEntity(InventoryItemDto dto) {
        if (dto == null) {
            return null;
        }
        InventoryItem entity = new InventoryItem();
        applyInventoryItemWrite(dto, entity);
        return entity;
    }

    /**
     * Updates the specified {@code InventoryItem} entity with data from the provided {@code InventoryItemDto}.
     * If either the DTO or the entity is {@code null}, the method performs no operation.
     *
     * @param dto the {@code InventoryItemDto} containing data to update the {@code InventoryItem} entity
     * @param entity the {@code InventoryItem} entity to be updated with data from the DTO
     */
    public void applyInventoryItemWrite(InventoryItemDto dto, InventoryItem entity) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setQuantity(dto.getQuantity());
        if (dto.getProductId() != null) {
            entity.setProduct(productRepository.getReferenceById(dto.getProductId()));
        }
    }

    /**
     * Converts a {@code ProductDto} to a {@code Product} entity.
     * If the input DTO is {@code null}, this method returns {@code null}.
     *
     * @param dto the {@code ProductDto} containing data to map to a {@code Product} entity
     * @return the {@code Product} entity mapped from the provided DTO, or {@code null} if the input DTO is {@code null}
     */
    public Product toProductEntity(ProductDto dto) {
        if (dto == null) {
            return null;
        }
        Product entity = new Product();
        applyProductWrite(dto, entity);
        return entity;
    }

    /**
     * Updates the specified {@code Product} entity with data from the provided {@code ProductDto}.
     * If either the DTO or the entity is {@code null}, the method performs no operation.
     *
     * @param dto the {@code ProductDto} containing data to update the {@code Product} entity
     * @param entity the {@code Product} entity to be updated with data from the DTO
     */
    public void applyProductWrite(ProductDto dto, Product entity) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setSku(dto.getSku());
        entity.setName(dto.getName());
        entity.setPrice(dto.getPrice());
        entity.setSpecs(dto.getSpecs());
        if (dto.getProductCategoryId() != null) {
            entity.setCategory(productCategoryRepository.getReferenceById(dto.getProductCategoryId()));
        }
    }

    /**
     * Converts a {@code ProductCategoryDto} to a {@code ProductCategory} entity.
     * If the input DTO is {@code null}, this method returns {@code null}.
     *
     * @param dto the {@code ProductCategoryDto} containing data to map to a {@code ProductCategory} entity
     * @return the {@code ProductCategory} entity mapped from the provided DTO, or {@code null} if the input DTO is {@code null}
     */
    public ProductCategory toProductCategoryEntity(ProductCategoryDto dto) {
        if (dto == null) {
            return null;
        }
        return ProductCategory.builder()
            .id(dto.getId())
            .code(dto.getCode())
            .name(dto.getName())
            .sortOrder(dto.getSortOrder())
            .build();
    }
}
