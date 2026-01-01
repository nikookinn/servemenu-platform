package com.servemenu.businessservice.infrastructure.outbox;

import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling outbox events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final Tracer tracer;
    
    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Save event to outbox with 2025 standard fields (Avro binary payload)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveEvent(String eventType, String topic, String aggregateId, 
                         String aggregateType, byte[] payload) {
        // Get distributed tracing context
        String traceId = Optional.ofNullable(tracer.currentSpan())
                .map(span -> span.context().traceId())
                .orElse(UUID.randomUUID().toString());
        
        String correlationId = Optional.ofNullable(tracer.currentSpan())
                .flatMap(span -> Optional.ofNullable(span.context().spanId()))
                .orElse(UUID.randomUUID().toString());
        
        log.debug("Saving event to outbox: type={}, topic={}, traceId={}, payloadSize={} bytes", 
                 eventType, topic, traceId, payload.length);

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventType(eventType)
                .topic(topic)
                .aggregateId(aggregateId)
                .aggregateType(aggregateType)
                .payload(payload)
                .traceId(traceId)
                .correlationId(correlationId)
                .schemaVersion(1)
                .source(applicationName)
                .build();

        outboxEventRepository.save(outboxEvent);
        log.info("✅ Event saved to outbox (Avro): {} [{}] - {} bytes", eventType, aggregateId, payload.length);
    }

    /**
     * Mark event as processed
     */
    @Transactional
    public void markAsProcessed(UUID eventId) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.markAsProcessed();
            outboxEventRepository.save(event);
        });
    }

    /**
     * Increment retry count
     */
    @Transactional
    public void incrementRetryCount(UUID eventId) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.incrementRetryCount();
            outboxEventRepository.save(event);
        });
    }
}