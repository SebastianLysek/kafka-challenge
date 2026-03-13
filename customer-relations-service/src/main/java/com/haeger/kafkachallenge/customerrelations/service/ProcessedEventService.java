package com.haeger.kafkachallenge.customerrelations.service;

import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.customerrelations.entity.ProcessedEvent;
import com.haeger.kafkachallenge.customerrelations.repository.ProcessedEventRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {
    private final ProcessedEventRepository processedEventRepository;

    /**
     * Checks if an event with the specified ID has been processed.
     *
     * @param eventId the unique identifier of the event to check
     * @return {@code true} if the event has been processed, {@code false} otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasProcessed(String eventId) {
        return processedEventRepository.existsById(eventId);
    }

    /**
     * Marks the specified integration event as processed by saving its details to the repository.
     *
     * @param event the integration event to be marked as processed
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
