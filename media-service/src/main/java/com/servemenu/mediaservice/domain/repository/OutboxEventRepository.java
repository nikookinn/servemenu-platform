package com.servemenu.mediaservice.domain.repository;

import com.servemenu.mediaservice.domain.model.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Find unprocessed events for processing
     * Used by polling mechanism (will be replaced by Debezium CDC)
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = false AND e.retryCount < :maxRetries ORDER BY e.createdAt ASC")
    List<OutboxEvent> findUnpublishedEventsBatch(@Param("maxRetries") int maxRetries, Pageable pageable);

    /**
     * Delete old processed events (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.processed = true AND e.processedAt < :before")
    int deletePublishedBefore(@Param("before") Instant before);

    /**
     * Count unprocessed events (monitoring)
     */
    long countByProcessedFalse();
}
