package com.haeger.kafkachallenge.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.common.outbox.OutboxEventStatus;
import com.haeger.kafkachallenge.inventory.entity.OutboxEvent;
import com.haeger.kafkachallenge.inventory.repository.OutboxEventRepository;
import java.util.ArrayList;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxService {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void enqueue(String topic, IntegrationEvent<?> event) {
        outboxEventRepository.save(OutboxEvent.builder()
            .id(event.getEventId())
            .topic(topic)
            .eventKey(event.getReferenceId())
            .eventType(event.getType())
            .referenceId(event.getReferenceId())
            .payload(toJson(event))
            .createdAt(Instant.now())
            .status(OutboxEventStatus.NEW)
            .attemptCount(0)
            .build());
    }

    @Transactional
    public List<OutboxEvent> claimPending() {
        initializeStatuses();

        Instant claimedAt = Instant.now();
        List<String> claimedEventIds = new ArrayList<>();
        for (OutboxEvent candidate : outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW)) {
            if (outboxEventRepository.claim(candidate.getId(), OutboxEventStatus.NEW, OutboxEventStatus.IN_PROGRESS, claimedAt) == 1) {
                claimedEventIds.add(candidate.getId());
            }
        }

        if (claimedEventIds.isEmpty()) {
            return List.of();
        }

        return outboxEventRepository.findAllByIdInOrderByCreatedAtAsc(claimedEventIds);
    }

    @Transactional
    public int markPublished(String outboxEventId) {
        return outboxEventRepository.markPublished(
            outboxEventId,
            OutboxEventStatus.IN_PROGRESS,
            OutboxEventStatus.PUBLISHED,
            Instant.now()
        );
    }

    @Transactional
    public void markFailed(String outboxEventId, Throwable error) {
        outboxEventRepository.markFailed(
            outboxEventId,
            OutboxEventStatus.IN_PROGRESS,
            OutboxEventStatus.NEW,
            abbreviateError(error)
        );
    }

    @Transactional
    public int requeueStaleClaims(Instant cutoff) {
        initializeStatuses();
        return outboxEventRepository.releaseStaleClaims(
            OutboxEventStatus.IN_PROGRESS,
            OutboxEventStatus.NEW,
            cutoff,
            "Outbox claim timed out before publish completed"
        );
    }

    private void initializeStatuses() {
        outboxEventRepository.initializePendingStatuses(OutboxEventStatus.NEW);
        outboxEventRepository.initializePublishedStatuses(OutboxEventStatus.PUBLISHED);
    }

    private String abbreviateError(Throwable error) {
        if (error == null) {
            return null;
        }

        String message = error.getClass().getSimpleName();
        if (error.getMessage() != null && !error.getMessage().isBlank()) {
            message += ": " + error.getMessage();
        }
        return message.length() > 4000 ? message.substring(0, 4000) : message;
    }

    private String toJson(IntegrationEvent<?> event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox event", ex);
        }
    }
}
