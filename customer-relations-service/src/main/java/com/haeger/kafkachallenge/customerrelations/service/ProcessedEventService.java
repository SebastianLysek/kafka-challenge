package com.haeger.kafkachallenge.customerrelations.service;

import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import java.sql.Timestamp;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {
    private static final String CLAIM_SQL = """
        insert into processed_events (event_id, event_type, reference_id, processed_at)
        values (?, ?, ?, ?)
        """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * Atomically claims the specified integration event for processing by inserting it into the dedupe table.
     *
     * @param event the integration event to claim
     * @return {@code true} if the current transaction claimed the event, {@code false} if it was already claimed
     */
    @Transactional
    public boolean claimEvent(IntegrationEvent<?> event) {
        try {
            return jdbcTemplate.update(
                CLAIM_SQL,
                event.getEventId(),
                event.getType(),
                event.getReferenceId(),
                Timestamp.from(Instant.now())
            ) == 1;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }
}
