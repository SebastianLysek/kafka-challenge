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

    @Transactional(readOnly = true)
    public boolean hasProcessed(String eventId) {
        return processedEventRepository.existsById(eventId);
    }

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
