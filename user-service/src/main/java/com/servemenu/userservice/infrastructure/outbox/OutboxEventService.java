package com.servemenu.userservice.infrastructure.outbox;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
@Service
public class OutboxEventService {
    private static final Logger log = LoggerFactory.getLogger(OutboxEventService.class);
    
    private final OutboxEventRepository outboxEventRepository;
    private final Tracer tracer;
    
    @Value("${spring.application.name}")
    private String applicationName;

    public OutboxEventService(OutboxEventRepository outboxEventRepository, Tracer tracer) {
        this.outboxEventRepository = outboxEventRepository;
        this.tracer = tracer;
    }

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

        OutboxEvent event = new OutboxEvent(eventType, topic, aggregateId, aggregateType, payload);
        event.setTraceId(traceId);
        event.setCorrelationId(correlationId);
        event.setSource(applicationName);
        
        outboxEventRepository.save(event);
        log.info("✅ Event saved to outbox (Avro): {} [{}] - {} bytes", eventType, aggregateId, payload.length);
    }
}
