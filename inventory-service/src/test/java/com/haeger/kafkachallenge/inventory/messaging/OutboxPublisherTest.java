package com.haeger.kafkachallenge.inventory.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.haeger.kafkachallenge.common.outbox.OutboxEventStatus;
import com.haeger.kafkachallenge.inventory.entity.OutboxEvent;
import com.haeger.kafkachallenge.inventory.service.OutboxService;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        outboxPublisher = new OutboxPublisher(outboxService, kafkaTemplate);
        ReflectionTestUtils.setField(outboxPublisher, "claimTimeoutMs", 120_000L);
    }

    @Test
    void successfulPublishMarksEventAsPublished() {
        OutboxEvent outboxEvent = sampleEvent();
        when(outboxService.requeueStaleClaims(any(Instant.class))).thenReturn(0);
        when(outboxService.claimPending()).thenReturn(List.of(outboxEvent));
        when(kafkaTemplate.send(outboxEvent.getTopic(), outboxEvent.getEventKey(), outboxEvent.getPayload()))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(outboxService.markPublished(outboxEvent.getId())).thenReturn(1);

        outboxPublisher.publishPendingEvents();

        verify(outboxService).requeueStaleClaims(any(Instant.class));
        verify(outboxService).claimPending();
        verify(outboxService).markPublished(outboxEvent.getId());
        verify(outboxService, never()).markFailed(eq(outboxEvent.getId()), any());
    }

    @Test
    void failedPublishReturnsEventToNewState() {
        OutboxEvent outboxEvent = sampleEvent();
        RuntimeException failure = new RuntimeException("boom");
        when(outboxService.requeueStaleClaims(any(Instant.class))).thenReturn(0);
        when(outboxService.claimPending()).thenReturn(List.of(outboxEvent));
        when(kafkaTemplate.send(outboxEvent.getTopic(), outboxEvent.getEventKey(), outboxEvent.getPayload()))
            .thenReturn(CompletableFuture.failedFuture(failure));

        outboxPublisher.publishPendingEvents();

        verify(outboxService).markFailed(outboxEvent.getId(), failure);
        verify(outboxService, never()).markPublished(outboxEvent.getId());
    }

    private OutboxEvent sampleEvent() {
        return OutboxEvent.builder()
            .id("event-1")
            .topic("orders")
            .eventKey("1001")
            .eventType("ORDER_CREATED")
            .payload("{\"eventId\":\"event-1\"}")
            .createdAt(Instant.parse("2026-03-16T09:00:00Z"))
            .status(OutboxEventStatus.IN_PROGRESS)
            .attemptCount(1)
            .claimedAt(Instant.parse("2026-03-16T09:00:00Z"))
            .build();
    }
}
