package com.haeger.kafkachallenge.streaming.inventory.repository;

import com.haeger.kafkachallenge.streaming.inventory.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    @EntityGraph(attributePaths = {"product", "product.category"})
    List<InventoryItem> findAllBy();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"product", "product.category"})
    List<InventoryItem> findAllByProduct_IdIn(Collection<Long> productIds);

    @EntityGraph(attributePaths = {"product", "product.category"})
    Optional<InventoryItem> findByProduct_Id(Long productId);
}
