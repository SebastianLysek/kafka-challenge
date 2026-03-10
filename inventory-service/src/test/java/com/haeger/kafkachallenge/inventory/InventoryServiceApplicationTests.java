package com.haeger.kafkachallenge.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.kafka.bootstrap-servers=localhost:0",
    "app.messaging.product-catalog.startup.enabled=false"
})
public class InventoryServiceApplicationTests {

    @Test
    void contextLoads() { }
}
