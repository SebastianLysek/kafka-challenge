package com.haeger.kafkachallenge.orders.repository;

import com.haeger.kafkachallenge.common.outbox.OutboxEventStatus;
import com.haeger.kafkachallenge.orders.entity.OutboxEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

    List<OutboxEvent> findAllByIdInOrderByCreatedAtAsc(List<String> ids);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update OutboxEvent e
        set e.status = :targetStatus,
            e.claimedAt = :claimedAt,
            e.attemptCount = coalesce(e.attemptCount, 0) + 1,
            e.lastError = null
        where e.id = :id
          and e.status = :expectedStatus
        """)
    int claim(
        @Param("id") String id,
        @Param("expectedStatus") OutboxEventStatus expectedStatus,
        @Param("targetStatus") OutboxEventStatus targetStatus,
        @Param("claimedAt") Instant claimedAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update OutboxEvent e
        set e.status = :publishedStatus,
            e.claimedAt = null,
            e.publishedAt = :publishedAt,
            e.lastError = null
        where e.id = :id
          and e.status = :expectedStatus
        """)
    int markPublished(
        @Param("id") String id,
        @Param("expectedStatus") OutboxEventStatus expectedStatus,
        @Param("publishedStatus") OutboxEventStatus publishedStatus,
        @Param("publishedAt") Instant publishedAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update OutboxEvent e
        set e.status = :targetStatus,
            e.claimedAt = null,
            e.lastError = :lastError
        where e.id = :id
          and e.status = :expectedStatus
        """)
    int markFailed(
        @Param("id") String id,
        @Param("expectedStatus") OutboxEventStatus expectedStatus,
        @Param("targetStatus") OutboxEventStatus targetStatus,
        @Param("lastError") String lastError
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update OutboxEvent e
        set e.status = :targetStatus,
            e.claimedAt = null,
            e.lastError = :lastError
        where e.status = :expectedStatus
          and e.claimedAt < :cutoff
        """)
    int releaseStaleClaims(
        @Param("expectedStatus") OutboxEventStatus expectedStatus,
        @Param("targetStatus") OutboxEventStatus targetStatus,
        @Param("cutoff") Instant cutoff,
        @Param("lastError") String lastError
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update OutboxEvent e
        set e.status = :status,
            e.attemptCount = coalesce(e.attemptCount, 0)
        where e.publishedAt is null
          and e.status is null
        """)
    int initializePendingStatuses(@Param("status") OutboxEventStatus status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update OutboxEvent e
        set e.status = :status,
            e.attemptCount = coalesce(e.attemptCount, 0)
        where e.publishedAt is not null
          and e.status is null
        """)
    int initializePublishedStatuses(@Param("status") OutboxEventStatus status);
}
