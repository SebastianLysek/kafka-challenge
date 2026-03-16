package com.haeger.kafkachallenge.orders.messaging;

import com.haeger.kafkachallenge.orders.entity.OutboxEvent;
import com.haeger.kafkachallenge.orders.service.OutboxService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxService outboxService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    @Value("${app.outbox.claim-timeout-ms:120000}")
    private long claimTimeoutMs;

    @Scheduled(fixedDelayString = "${app.outbox.publish-interval-ms:1000}")
    public void publishPendingEvents() {
        int requeued = outboxService.requeueStaleClaims(Instant.now().minusMillis(claimTimeoutMs));
        if (requeued > 0) {
            LOG.warn("Requeued {} stale outbox event claims", requeued);
        }

        List<OutboxEvent> claimedEvents = outboxService.claimPending();
        for (OutboxEvent outboxEvent : claimedEvents) {
            kafkaTemplate.send(outboxEvent.getTopic(), outboxEvent.getEventKey(), outboxEvent.getPayload())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        LOG.warn("Failed to publish outbox event {} to topic {}", outboxEvent.getId(), outboxEvent.getTopic(), ex);
                        outboxService.markFailed(outboxEvent.getId(), ex);
                        return;
                    }
                    if (outboxService.markPublished(outboxEvent.getId()) == 1) {
                        LOG.info("Published outbox event {} to topic {}", outboxEvent.getId(), outboxEvent.getTopic());
                    }
                });
        }
    }
}
