package com.haeger.kafkachallenge.streaming.streams.topology;

import com.haeger.kafkachallenge.common.dto.OrderStatus;
import com.haeger.kafkachallenge.streaming.proto.OrderCreated;
import com.haeger.kafkachallenge.streaming.proto.OrderStatusChanged;
import com.haeger.kafkachallenge.streaming.proto.ProductUpserted;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;

public class OrderDecisionTransformer implements Transformer<String, OrderCreated, KeyValue<String, OrderStatusChanged>> {
    private final Consumer<OrderCreated> inventoryReservation;
    private KeyValueStore<String, ValueAndTimestamp<ProductUpserted>> inventoryStore;

    public OrderDecisionTransformer(Consumer<OrderCreated> inventoryReservation) {
        this.inventoryReservation = inventoryReservation;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(ProcessorContext context) {
        inventoryStore = (KeyValueStore<String, ValueAndTimestamp<ProductUpserted>>) context.getStateStore(StreamingTopics.INVENTORY_STORE);
    }

    @Override
    public KeyValue<String, OrderStatusChanged> transform(String key, OrderCreated order) {
        Decision decision = decide(order);
        if (decision.status() == OrderStatus.CONFIRMED) {
            try {
                inventoryReservation.accept(order);
            } catch (RuntimeException ex) {
                decision = new Decision(OrderStatus.DECLINED, ex.getMessage());
            }
        }

        OrderStatusChanged statusChanged = OrderStatusChanged.newBuilder()
            .setOrderId(order.getOrderId())
            .setCustomerId(order.getCustomerId())
            .setCustomerEmail(order.getCustomerEmail())
            .setCustomerFullName(order.getCustomerFullName())
            .setStatus(decision.status().name())
            .setReason(decision.reason())
            .build();
        return KeyValue.pair(Long.toString(order.getOrderId()), statusChanged);
    }

    @Override
    public void close() {
    }

    private Decision decide(OrderCreated order) {
        Map<Long, Integer> requiredQuantities = new LinkedHashMap<>();
        order.getItemsList().forEach(item -> requiredQuantities.merge(item.getProductId(), item.getQuantity(), Integer::sum));

        for (Map.Entry<Long, Integer> entry : requiredQuantities.entrySet()) {
            ValueAndTimestamp<ProductUpserted> storedProduct = inventoryStore.get(Long.toString(entry.getKey()));
            ProductUpserted product = storedProduct == null ? null : storedProduct.value();
            if (product == null) {
                return new Decision(OrderStatus.DECLINED, "Unknown product " + entry.getKey());
            }
            if (product.getQuantity() < entry.getValue()) {
                return new Decision(
                    OrderStatus.DECLINED,
                    "Insufficient inventory for product " + product.getSku()
                );
            }
        }

        return new Decision(OrderStatus.CONFIRMED, "Inventory reserved for order");
    }

    private record Decision(OrderStatus status, String reason) {
    }
}
