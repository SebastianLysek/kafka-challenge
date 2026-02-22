package com.haeger.kafkachallenge.shipment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=localhost:0")
public class ShipmentServiceApplicationTests {

    @Test
    void contextLoads() { }
}
