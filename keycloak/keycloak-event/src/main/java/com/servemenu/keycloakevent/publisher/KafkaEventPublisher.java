package com.servemenu.keycloakevent.publisher;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

public class KafkaEventPublisher {

    private static final Logger LOG = Logger.getLogger(KafkaEventPublisher.class);
    private static final String TYPE_ID_HEADER = "__TypeId__";

    private final KafkaProducer<String, String> producer;
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);

    public KafkaEventPublisher(String bootstrapServers) {
        if (bootstrapServers == null || bootstrapServers.isEmpty()) {
            throw new IllegalArgumentException("Kafka bootstrap servers cannot be null or empty");
        }

        LOG.infof("Initializing Kafka producer with servers: %s", bootstrapServers);

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Production-ready settings
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "keycloak-event-publisher");

        this.producer = new KafkaProducer<>(props);
        LOG.info("Kafka producer initialized successfully");
    }

    public void publish(String topic, String key, String value, String typeId) {
        if (topic == null || topic.isEmpty()) {
            LOG.warn("Skipping null or empty topic");
            failureCount.incrementAndGet();
            return;
        }

        if (key == null || value == null) {
            LOG.warnf("Skipping null key or value for topic: %s", topic);
            failureCount.incrementAndGet();
            return;
        }

        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);

            if (typeId != null && !typeId.isEmpty()) {
                record.headers().add(new RecordHeader(
                        TYPE_ID_HEADER,
                        typeId.getBytes(StandardCharsets.UTF_8)
                ));
                LOG.tracef("Added __TypeId__ header: %s", typeId);
            }

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    failureCount.incrementAndGet();
                    LOG.errorf(exception, "Failed to send event to Kafka. Topic: %s, Key: %s",
                            topic, key);
                } else {
                    successCount.incrementAndGet();
                    if (LOG.isTraceEnabled()) {
                        LOG.tracef("Event sent. Topic: %s, Partition: %d, Offset: %d, TypeId: %s",
                                metadata.topic(), metadata.partition(), metadata.offset(), typeId);
                    }
                }
            });

        } catch (Exception e) {
            failureCount.incrementAndGet();
            LOG.errorf(e, "Unexpected error while sending event. Topic: %s, Key: %s", topic, key);
        }
    }

    public void publish(String topic, String key, String value) {
        publish(topic, key, value, null);
    }

    public void close() {
        try {
            LOG.infof("Closing Kafka producer. Success: %d, Failures: %d",
                    successCount.get(), failureCount.get());

            producer.flush();
            producer.close(Duration.ofSeconds(10));

            LOG.info("Kafka producer closed successfully");
        } catch (Exception e) {
            LOG.error("Error while closing Kafka producer", e);
        }
    }

    public long getSuccessCount() {
        return successCount.get();
    }

    public long getFailureCount() {
        return failureCount.get();
    }
}