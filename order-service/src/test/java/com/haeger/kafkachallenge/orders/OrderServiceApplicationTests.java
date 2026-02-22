package com.haeger.kafkachallenge.orders;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=localhost:0")
class OrderServiceApplicationTests {

    @Test
    void contextLoads() { }
}