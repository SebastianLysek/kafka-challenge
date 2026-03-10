package com.haeger.kafkachallenge.orders.config;

import com.haeger.kafkachallenge.common.events.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfiguration {

    @Bean
    public KafkaAdmin.NewTopics kafkaTopics() {
        return new KafkaAdmin.NewTopics(
            topic(KafkaTopics.PRODUCT_CATALOG, 1, (short) 1),
            topic(KafkaTopics.ORDERS, 1, (short) 1)
        );
    }

    private NewTopic topic(String name, int partitions, short replicas) {
        return TopicBuilder.name(name)
            .partitions(partitions)
            .replicas(replicas)
            .build();
    }
}
