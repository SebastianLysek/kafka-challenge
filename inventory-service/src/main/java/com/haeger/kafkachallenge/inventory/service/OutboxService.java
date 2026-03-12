package com.haeger.kafkachallenge.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.inventory.entity.OutboxEvent;
import com.haeger.kafkachallenge.inventory.repository.OutboxEventRepository;
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
            .build());
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> findPending() {
        return outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
    }

    @Transactional
    public void markPublished(String outboxEventId) {
        outboxEventRepository.findById(outboxEventId)
            .ifPresent(outboxEvent -> outboxEvent.setPublishedAt(Instant.now()));
    }

    private String toJson(IntegrationEvent<?> event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox event", ex);
        }
    }
}
