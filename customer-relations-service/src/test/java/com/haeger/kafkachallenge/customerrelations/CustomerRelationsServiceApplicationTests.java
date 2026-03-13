package com.haeger.kafkachallenge.customerrelations;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.kafka.bootstrap-servers=localhost:0",
    "spring.kafka.listener.auto-startup=false"
})
public class CustomerRelationsServiceApplicationTests {

    @Test
    void contextLoads() { }
}
