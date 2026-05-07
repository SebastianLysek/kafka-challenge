package com.haeger.kafkachallenge.streaming;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.kafka.streams.auto-startup=false",
    "spring.kafka.listener.auto-startup=false",
    "spring.kafka.admin.auto-create=false"
})
class StreamingServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
