package com.haeger.kafkachallenge.inventory.service;

import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.common.events.KafkaTopics;
import com.haeger.kafkachallenge.common.events.OrderCreatedItemPayload;
import com.haeger.kafkachallenge.common.events.OrderCreatedPayload;
import com.haeger.kafkachallenge.common.events.OrderFulfillmentCheckItem;
import com.haeger.kafkachallenge.common.events.OrderFulfillmentCheckResultPayload;
import com.haeger.kafkachallenge.inventory.entity.InventoryItem;
import com.haeger.kafkachallenge.inventory.repository.InventoryItemRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderFulfillmentService {
    private final InventoryItemRepository inventoryItemRepository;
    private final OutboxService outboxService;

    @Transactional
    public void handleOrderCreated(OrderCreatedPayload order) {
        Map<Long, Integer> requiredQuantities = new LinkedHashMap<>();
        Map<Long, OrderCreatedItemPayload> orderItemsByProductId = new LinkedHashMap<>();
        for (OrderCreatedItemPayload item : order.getItems()) {
            requiredQuantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            orderItemsByProductId.putIfAbsent(item.getProductId(), item);
        }

        Map<Long, InventoryItem> inventoryByProductId = inventoryItemRepository.findAllByProduct_IdIn(requiredQuantities.keySet()).stream()
            .collect(java.util.stream.Collectors.toMap(item -> item.getProduct().getId(), Function.identity()));

        List<OrderFulfillmentCheckItem> shortages = new ArrayList<>();
        List<OrderFulfillmentCheckItem> checkedItems = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : requiredQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer requestedQuantity = entry.getValue();
            InventoryItem inventoryItem = inventoryByProductId.get(productId);
            int availableQuantity = inventoryItem != null ? inventoryItem.getQuantity() : 0;
            OrderCreatedItemPayload orderItem = orderItemsByProductId.get(productId);

            OrderFulfillmentCheckItem checkedItem = OrderFulfillmentCheckItem.builder()
                .productId(productId)
                .productSku(orderItem.getProductSku())
                .productName(orderItem.getProductName())
                .requestedQuantity(requestedQuantity)
                .availableQuantity(availableQuantity)
                .build();
            checkedItems.add(checkedItem);

            if (availableQuantity < requestedQuantity) {
                shortages.add(checkedItem);
            }
        }

        if (!shortages.isEmpty()) {
            outboxService.enqueue(
                KafkaTopics.ORDERS,
                IntegrationEvent.of(
                    EventTypes.ORDER_FULFILLMENT_CHECK_FAILED,
                    order.getOrderId().toString(),
                    OrderFulfillmentCheckResultPayload.builder()
                        .orderId(order.getOrderId())
                        .message("Insufficient inventory for one or more order items")
                        .items(shortages)
                        .build()
                )
            );
            return;
        }

        for (Map.Entry<Long, Integer> entry : requiredQuantities.entrySet()) {
            InventoryItem inventoryItem = inventoryByProductId.get(entry.getKey());
            // the changes regarding the quantity will be persisted when the transaction of this method is committed/flushed
            inventoryItem.setQuantity(inventoryItem.getQuantity() - entry.getValue());
        }

        outboxService.enqueue(
            KafkaTopics.ORDERS,
            IntegrationEvent.of(
                EventTypes.ORDER_FULFILLMENT_CHECK_SUCCEEDED,
                order.getOrderId().toString(),
                OrderFulfillmentCheckResultPayload.builder()
                    .orderId(order.getOrderId())
                    .message("Inventory reserved for order")
                    .items(checkedItems)
                    .build()
            )
        );
    }
}
