package com.haeger.kafkachallenge.customerrelations;

import com.haeger.kafkachallenge.customerrelations.config.MailProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MailProperties.class)
public class CustomerRelationsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CustomerRelationsServiceApplication.class, args);
    }
}
