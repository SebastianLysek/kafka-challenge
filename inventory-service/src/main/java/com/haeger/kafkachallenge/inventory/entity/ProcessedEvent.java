package com.haeger.kafkachallenge.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {
    @Id
    @Column(nullable = false, length = 64)
    private String eventId;

    @Column(nullable = false, length = 128)
    private String eventType;

    @Column(length = 128)
    private String referenceId;

    @Column(nullable = false)
    private Instant processedAt;
}
