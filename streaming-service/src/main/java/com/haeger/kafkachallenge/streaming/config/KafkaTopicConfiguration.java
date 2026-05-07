package com.haeger.kafkachallenge.streaming.config;

import com.haeger.kafkachallenge.streaming.streams.topology.StreamingTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfiguration {
    @Bean
    NewTopic streamingProductUpsertedEventsTopic() {
        return topic(StreamingTopics.PRODUCT_UPSERTED);
    }

    @Bean
    NewTopic streamingOrderCreatedEventsTopic() {
        return topic(StreamingTopics.ORDER_CREATED);
    }

    @Bean
    NewTopic streamingOrderStatusChangedEventsTopic() {
        return topic(StreamingTopics.ORDER_STATUS_CHANGED);
    }

    @Bean
    NewTopic streamingShipmentPreparationStartedEventsTopic() {
        return topic(StreamingTopics.SHIPMENT_PREPARATION_STARTED);
    }

    @Bean
    NewTopic streamingShipmentCompletedEventsTopic() {
        return topic(StreamingTopics.SHIPMENT_COMPLETED);
    }

    @Bean
    NewTopic streamingNotificationRequestedEventsTopic() {
        return topic(StreamingTopics.NOTIFICATION_REQUESTED);
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(1).replicas(1).build();
    }
}
