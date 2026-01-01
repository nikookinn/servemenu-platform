package com.servemenu.userservice.infrastructure.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Outbox Event pattern
 * Ensures transactional outbox for reliable event publishing
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    
    /**
     * Find unprocessed events with retry limit using pagination (2025 best practice)
     * @param maxRetries Maximum retry count
     * @param pageable Pagination parameters (use PageRequest.of(0, batchSize))
     * @return List of unprocessed events
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = false AND e.retryCount < :maxRetries ORDER BY e.createdAt ASC")
    List<OutboxEvent> findUnpublishedEventsBatch(@Param("maxRetries") int maxRetries, Pageable pageable);

    /**
     * Count unprocessed events for monitoring
     */
    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.processed = false")
    long countUnpublished();

    /**
     * Delete old processed events for cleanup
     * @param beforeDate Threshold date
     * @return Number of deleted records
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.processed = true AND e.processedAt < :beforeDate")
    int deletePublishedBefore(@Param("beforeDate") Instant beforeDate);
}
