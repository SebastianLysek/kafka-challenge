package com.haeger.kafkachallenge.common.events;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationEvent<T> {
    private String eventId;
    private String type;
    private String referenceId;
    private Instant occurredAt;
    private T payload;

    public static <T> IntegrationEvent<T> of(String type, T payload) {
        return of(type, null, payload);
    }

    public static <T> IntegrationEvent<T> of(String type, String referenceId, T payload) {
        return IntegrationEvent.<T>builder()
            .eventId(UUID.randomUUID().toString())
            .type(type)
            .referenceId(referenceId)
            .occurredAt(Instant.now())
            .payload(payload)
            .build();
    }
}
