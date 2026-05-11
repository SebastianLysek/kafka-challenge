package com.haeger.kafkachallenge.streaming.config;

import com.google.protobuf.Message;
import com.haeger.kafkachallenge.streaming.proto.NotificationRequested;
import com.haeger.kafkachallenge.streaming.proto.OrderStatusChanged;
import com.haeger.kafkachallenge.streaming.proto.ShipmentPreparationStarted;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

@Configuration
public class KafkaListenerConfiguration {
    private final String bootstrapServers;
    private final String schemaRegistryUrl;
    private final String autoOffsetReset;
    private final int concurrency;
    private final boolean autoStartup;

    public KafkaListenerConfiguration(
        @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
        @Value("${spring.kafka.properties.schema.registry.url:mock://streaming-service}") String schemaRegistryUrl,
        @Value("${spring.kafka.consumer.auto-offset-reset:earliest}") String autoOffsetReset,
        @Value("${spring.kafka.listener.concurrency:1}") int concurrency,
        @Value("${spring.kafka.listener.auto-startup:true}") boolean autoStartup
    ) {
        this.bootstrapServers = bootstrapServers;
        this.schemaRegistryUrl = schemaRegistryUrl;
        this.autoOffsetReset = autoOffsetReset;
        this.concurrency = concurrency;
        this.autoStartup = autoStartup;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, OrderStatusChanged> orderStatusChangedKafkaListenerContainerFactory() {
        return protobufListenerContainerFactory(OrderStatusChanged.class);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, ShipmentPreparationStarted>
    shipmentPreparationStartedKafkaListenerContainerFactory() {
        return protobufListenerContainerFactory(ShipmentPreparationStarted.class);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, NotificationRequested>
    notificationRequestedKafkaListenerContainerFactory() {
        return protobufListenerContainerFactory(NotificationRequested.class);
    }

    private <T extends Message> ConcurrentKafkaListenerContainerFactory<String, T> protobufListenerContainerFactory(
        Class<T> valueType
    ) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaProtobufDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        props.put(KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE, valueType.getName());

        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));
        factory.setConcurrency(concurrency);
        factory.setAutoStartup(autoStartup);
        return factory;
    }
}
