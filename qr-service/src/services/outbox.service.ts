import { db } from '../db/client';
import { SchemaRegistry } from '@kafkajs/confluent-schema-registry';
import { logger } from '../utils/logger';
import { v4 as uuidv4 } from 'uuid';

/**
 * Outbox Pattern Service for QR Service
 * Saves events to PostgreSQL outbox table with Avro serialization
 * Debezium CDC reads from outbox and publishes to Kafka
 */
export class OutboxService {
  private registry: SchemaRegistry;

  constructor() {
    this.registry = new SchemaRegistry({
      host: process.env.SCHEMA_REGISTRY_URL || 'http://localhost:9081'
    });
  }

  /**
   * Save event to outbox table with Avro serialization
   * Debezium CDC will read and publish to Kafka automatically
   * 
   * @param aggregateId - ID of the aggregate (e.g., tableId)
   * @param aggregateType - Type of aggregate (e.g., 'Table')
   * @param eventType - Type of event (e.g., 'QRGenerated')
   * @param topic - Kafka topic to publish to
   * @param eventData - Event data object
   * @param schemaId - Avro schema ID from Schema Registry
   */
  async saveEvent(
    aggregateId: string,
    aggregateType: string,
    eventType: string,
    topic: string,
    eventData: any,
    schemaId: number
  ): Promise<void> {
    try {
      // 1. Serialize to Avro binary using Schema Registry
      const avroBytes = await this.registry.encode(schemaId, eventData);

      // 2. Save to outbox table as BYTEA (raw binary)
      const query = `
        INSERT INTO outbox_events (
          id, aggregate_id, aggregate_type, event_type, topic, payload, published, created_at
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, NOW())
      `;

      const values = [
        uuidv4(),
        aggregateId,
        aggregateType,
        eventType,
        topic,
        Buffer.from(avroBytes), // Store as BYTEA
        false
      ];

      await db.query(query, values);

      logger.info('📦 Event saved to outbox (Avro binary)', {
        aggregateId,
        eventType,
        topic,
        schemaId,
        payloadSizeBytes: avroBytes.length,
        payloadSizeKB: (avroBytes.length / 1024).toFixed(2)
      });

    } catch (error) {
      logger.error('❌ Failed to save event to outbox', {
        error: error instanceof Error ? error.message : error,
        aggregateId,
        eventType,
        topic
      });
      throw error;
    }
  }

  /**
   * Cleanup old events
   * Debezium doesn't update 'published' flag, so we delete by age
   * Assumes Debezium has processed events within retention period
   * 
   * @param daysOld - Delete events older than this many days
   * @returns Number of deleted events
   */
  async cleanupOldEvents(daysOld: number = 7): Promise<number> {
    try {
      const query = `
        DELETE FROM outbox_events
        WHERE created_at < NOW() - INTERVAL '${daysOld} days'
      `;

      const result = await db.query(query);
      const deletedCount = result.rowCount || 0;

      logger.info('🧹 Cleaned up old outbox events', { deletedCount, daysOld });
      return deletedCount;

    } catch (error) {
      logger.error('❌ Failed to cleanup old events', { error });
      throw error;
    }
  }

  /**
   * Get count of unpublished events (for monitoring)
   */
  async getUnpublishedCount(): Promise<number> {
    try {
      const result = await db.query(
        'SELECT COUNT(*) as count FROM outbox_events WHERE published = false'
      );
      return parseInt(result.rows[0].count);
    } catch (error) {
      logger.error('❌ Failed to get unpublished count', { error });
      return 0;
    }
  }

  /**
   * Get recent events (for debugging)
   */
  async getRecentEvents(limit: number = 10): Promise<any[]> {
    try {
      const result = await db.query(
        'SELECT id, aggregate_id, event_type, topic, published, created_at FROM outbox_events ORDER BY created_at DESC LIMIT $1',
        [limit]
      );
      return result.rows;
    } catch (error) {
      logger.error('❌ Failed to get recent events', { error });
      return [];
    }
  }
}

export const outboxService = new OutboxService();
