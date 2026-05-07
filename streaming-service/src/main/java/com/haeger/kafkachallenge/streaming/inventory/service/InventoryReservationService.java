package com.haeger.kafkachallenge.streaming.inventory.service;

import com.haeger.kafkachallenge.streaming.inventory.entity.InventoryItem;
import com.haeger.kafkachallenge.streaming.inventory.repository.InventoryItemRepository;
import com.haeger.kafkachallenge.streaming.proto.OrderCreated;
import com.haeger.kafkachallenge.streaming.streams.topology.ProtoEventPublisher;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryReservationService {
    private final InventoryItemRepository inventoryItemRepository;
    private final ProductEventFactory productEventFactory;
    private final ProtoEventPublisher eventPublisher;

    @Transactional
    public void reserve(OrderCreated order) {
        Map<Long, Integer> requiredQuantities = new LinkedHashMap<>();
        order.getItemsList().forEach(item -> requiredQuantities.merge(item.getProductId(), item.getQuantity(), Integer::sum));

        Map<Long, InventoryItem> inventoryByProductId = inventoryItemRepository.findAllByProduct_IdIn(requiredQuantities.keySet())
            .stream()
            .collect(java.util.stream.Collectors.toMap(item -> item.getProduct().getId(), Function.identity()));

        requiredQuantities.forEach((productId, quantity) -> {
            InventoryItem item = inventoryByProductId.get(productId);
            if (item == null || item.getQuantity() < quantity) {
                throw new IllegalStateException("Inventory changed before order could be reserved");
            }
            item.setQuantity(item.getQuantity() - quantity);
        });

        inventoryItemRepository.saveAll(inventoryByProductId.values()).forEach(item ->
            eventPublisher.publishProductUpserted(productEventFactory.toProductUpserted(item)));
    }
}
