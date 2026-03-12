package com.haeger.kafkachallenge.orders;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.kafka.bootstrap-servers=localhost:0",
    "spring.kafka.admin.auto-create=false"
})
class OrderServiceApplicationTests {

    @Test
    void contextLoads() { }
}
