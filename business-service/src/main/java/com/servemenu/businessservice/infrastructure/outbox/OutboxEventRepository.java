package com.servemenu.businessservice.infrastructure.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Outbox Event pattern
 * Ensures transactional outbox for reliable event publishing
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Find unpublished events with retry limit using pagination (2025 best practice)
     * @param maxRetries Maximum retry count
     * @param pageable Pagination parameters (use PageRequest.of(0, batchSize))
     * @return List of unpublished events
     */
    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.processed = false AND oe.retryCount < :maxRetries ORDER BY oe.createdAt ASC")
    List<OutboxEvent> findUnpublishedEventsBatch(@Param("maxRetries") int maxRetries, Pageable pageable);

    /**
     * Count unpublished events for monitoring
     */
    @Query("SELECT COUNT(oe) FROM OutboxEvent oe WHERE oe.processed = false")
    long countUnpublished();

    /**
     * Delete old published events for cleanup
     * @param threshold Threshold date
     * @return Number of deleted records
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent oe WHERE oe.processed = true AND oe.processedAt < :threshold")
    int deletePublishedBefore(@Param("threshold") Instant threshold);
}