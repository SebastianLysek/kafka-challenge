package com.haeger.kafkachallenge.streaming.streams.topology;

import com.google.protobuf.Message;
import com.haeger.kafkachallenge.streaming.proto.NotificationRequested;
import com.haeger.kafkachallenge.streaming.proto.OrderCreated;
import com.haeger.kafkachallenge.streaming.proto.OrderStatusChanged;
import com.haeger.kafkachallenge.streaming.proto.ProductUpserted;
import com.haeger.kafkachallenge.streaming.proto.ShipmentCompleted;
import com.haeger.kafkachallenge.streaming.proto.ShipmentPreparationStarted;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class ProtoEventPublisher {
    private final KafkaTemplate<Object, Object> kafkaTemplate;

    public void publishProductUpserted(ProductUpserted event) {
        publish(StreamingTopics.PRODUCT_UPSERTED, Long.toString(event.getProductId()), event);
    }

    public void publishOrderCreated(OrderCreated event) {
        publish(StreamingTopics.ORDER_CREATED, Long.toString(event.getOrderId()), event);
    }

    public void publishOrderStatusChanged(OrderStatusChanged event) {
        publish(StreamingTopics.ORDER_STATUS_CHANGED, Long.toString(event.getOrderId()), event);
    }

    public void publishShipmentPreparationStarted(ShipmentPreparationStarted event) {
        publish(StreamingTopics.SHIPMENT_PREPARATION_STARTED, Long.toString(event.getOrderId()), event);
    }

    public void publishShipmentCompleted(ShipmentCompleted event) {
        publish(StreamingTopics.SHIPMENT_COMPLETED, Long.toString(event.getOrderId()), event);
    }

    public void publishNotificationRequested(NotificationRequested event) {
        publish(StreamingTopics.NOTIFICATION_REQUESTED, Long.toString(event.getOrderId()), event);
    }

    private void publish(String topic, String key, Message event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    kafkaTemplate.send(topic, key, event);
                }
            });
            return;
        }
        kafkaTemplate.send(topic, key, event);
    }
}
