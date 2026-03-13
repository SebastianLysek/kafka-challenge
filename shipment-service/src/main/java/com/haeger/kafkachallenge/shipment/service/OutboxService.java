package com.haeger.kafkachallenge.shipment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.shipment.entity.OutboxEvent;
import com.haeger.kafkachallenge.shipment.repository.OutboxEventRepository;
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

    /**
     * Adds an integration event to the outbox for processing.
     * The event is saved as an {@link OutboxEvent} entity in the outbox table.
     *
     * @param topic the target topic where the event should be published
     * @param event the integration event containing the data to be persisted in the outbox
     */
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

    /**
     * Retrieves a list of pending {@link OutboxEvent} entities that have not yet been published.
     * The results are ordered by their creation time in ascending order and limited to a maximum of 100 entries.
     *
     * @return a list of the first 100 {@link OutboxEvent} entities with a null {@code publishedAt} field,
     *         ordered by their {@code createdAt} field in ascending order
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> findPending() {
        return outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
    }

    /**
     * Marks the specified outbox event as published by setting its {@code publishedAt} timestamp to the current time.
     * If the event with the given ID is not found, no action is performed.
     *
     * @param outboxEventId the unique identifier of the outbox event to be marked as published
     */
    @Transactional
    public void markPublished(String outboxEventId) {
        outboxEventRepository.findById(outboxEventId)
            .ifPresent(outboxEvent -> outboxEvent.setPublishedAt(Instant.now()));
    }

    /**
     * Converts the provided integration event into its JSON string representation.
     *
     * @param event the integration event to be serialized into JSON
     * @return the JSON string representation of the given integration event
     * @throws IllegalStateException if the serialization process fails
     */
    private String toJson(IntegrationEvent<?> event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox event", ex);
        }
    }
}
