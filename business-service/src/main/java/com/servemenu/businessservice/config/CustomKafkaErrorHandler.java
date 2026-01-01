package com.servemenu.businessservice.config;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

/**
 * Custom error handler for Kafka consumer errors
 * Logs errors and allows Spring Kafka to handle recovery
 */
public class CustomKafkaErrorHandler implements CommonErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomKafkaErrorHandler.class);

    @Override
    public boolean handleOne(Exception exception, ConsumerRecord<?, ?> record,
                             Consumer<?, ?> consumer, MessageListenerContainer container) {

        logger.error("Error processing Kafka message. Topic: {}, Partition: {}, Offset: {}, Key: {}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                exception);

        return false;
    }

    @Override
    public void handleOtherException(Exception exception, Consumer<?, ?> consumer,
                                     MessageListenerContainer container, boolean batchListener) {
        logger.error("Unexpected error in Kafka listener container", exception);
    }
}
