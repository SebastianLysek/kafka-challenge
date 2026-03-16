package com.haeger.kafkachallenge.common.outbox;

public enum OutboxEventStatus {
    NEW,
    IN_PROGRESS,
    PUBLISHED
}
