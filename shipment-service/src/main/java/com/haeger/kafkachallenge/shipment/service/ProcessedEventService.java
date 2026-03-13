package com.haeger.kafkachallenge.shipment.service;

import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.shipment.entity.ProcessedEvent;
import com.haeger.kafkachallenge.shipment.repository.ProcessedEventRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {
    private final ProcessedEventRepository processedEventRepository;

    /**
     * Checks if an event with the given identifier has already been processed.
     *
     * @param eventId the unique identifier of the event to check
     * @return {@code true} if the event has been processed, {@code false} otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasProcessed(String eventId) {
        return processedEventRepository.existsById(eventId);
    }

    /**
     * Marks the given event as processed by persisting it to the database.
     *
     * @param event the integration event to be marked as processed,
     *              containing event details such as ID, type, and reference ID
     */
    @Transactional
    public void markProcessed(IntegrationEvent<?> event) {
        processedEventRepository.save(ProcessedEvent.builder()
            .eventId(event.getEventId())
            .eventType(event.getType())
            .referenceId(event.getReferenceId())
            .processedAt(Instant.now())
            .build());
    }
}
