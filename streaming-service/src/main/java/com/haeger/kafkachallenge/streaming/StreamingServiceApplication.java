package com.haeger.kafkachallenge.streaming;

import com.haeger.kafkachallenge.streaming.notification.config.MailProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@EnableKafka
@EnableKafkaStreams
@SpringBootApplication
@EnableConfigurationProperties(MailProperties.class)
public class StreamingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(StreamingServiceApplication.class, args);
    }
}
