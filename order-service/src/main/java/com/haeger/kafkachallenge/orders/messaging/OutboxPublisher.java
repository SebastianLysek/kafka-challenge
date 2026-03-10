package com.haeger.kafkachallenge.orders.messaging;

import com.haeger.kafkachallenge.orders.entity.OutboxEvent;
import com.haeger.kafkachallenge.orders.service.OutboxService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxService outboxService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.publish-interval-ms:1000}")
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxService.findPending();
        for (OutboxEvent outboxEvent : pendingEvents) {
            kafkaTemplate.send(outboxEvent.getTopic(), outboxEvent.getEventKey(), outboxEvent.getPayload())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        LOG.warn("Failed to publish outbox event {} to topic {}", outboxEvent.getId(), outboxEvent.getTopic(), ex);
                        return;
                    }
                    outboxService.markPublished(outboxEvent.getId());
                    LOG.info("Published outbox event {} to topic {}", outboxEvent.getId(), outboxEvent.getTopic());
                });
        }
    }
}
