package com.haeger.kafkachallenge.inventory.repository;

import com.haeger.kafkachallenge.inventory.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    /**
     * Retrieves all inventory items from the database with their associated product and product category.
     * The method uses an entity graph to eagerly fetch the related entities.
     *
     * @return a list of {@code InventoryItem} entities, each containing its associated product and product category.
     */
    @EntityGraph(attributePaths = {"product", "product.category"})
    List<InventoryItem> findAllBy();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "product")
    List<InventoryItem> findAllByProduct_IdIn(Collection<Long> productIds);
}
