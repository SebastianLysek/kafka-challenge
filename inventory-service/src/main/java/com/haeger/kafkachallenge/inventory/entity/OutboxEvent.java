package com.haeger.kafkachallenge.inventory.entity;

import com.haeger.kafkachallenge.common.outbox.OutboxEventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
    @Id
    @Column(nullable = false, length = 64)
    private String id;

    @Column(nullable = false, length = 255)
    private String topic;

    @Column(nullable = false, length = 255)
    private String eventKey;

    @Column(nullable = false, length = 128)
    private String eventType;

    @Column(length = 128)
    private String referenceId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private OutboxEventStatus status;

    private Instant claimedAt;

    private Instant publishedAt;

    private Integer attemptCount;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String lastError;
}
