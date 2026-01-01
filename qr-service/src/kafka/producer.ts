/**
 * Kafka Producer for QRGeneratedEvent using Outbox Pattern
 * Events are saved to PostgreSQL outbox table
 * Debezium CDC reads from outbox and publishes to Kafka
 *
 * File location: src/kafka/producer.ts
 */

import { SchemaRegistry, SchemaType } from '@kafkajs/confluent-schema-registry';
import { logger } from '../utils/logger';
import { v4 as uuidv4 } from 'uuid';
import { outboxService } from '../services/outbox.service';

// Avro Schema for QRGeneratedEvent (unified for all QR types)
// BACKWARD COMPATIBLE: All new fields must have default values
const QR_GENERATED_EVENT_SCHEMA = {
  type: 'record',
  name: 'QRGeneratedEvent',
  namespace: 'com.servemenu.shared.events.avro',
  fields: [
    { name: 'eventId', type: 'string' },
    { name: 'eventType', type: 'string' },
    { name: 'timestamp', type: { type: 'long', logicalType: 'timestamp-millis' } },
    { name: 'qrType', type: 'string', default: 'TABLE' }, // BUSINESS, TABLE, WIFI - default for backward compatibility
    { name: 'storeId', type: ['null', 'string'], default: null }, // Nullable for BUSINESS QR
    { name: 'businessId', type: ['null', 'string'], default: null },
    { name: 'tableId', type: ['null', 'string'], default: null },
    { name: 'wifiSettingsId', type: ['null', 'string'], default: null },
    { name: 'qrImageBase64', type: 'string' },
    { name: 'qrImageSize', type: 'int' },
    { name: 'qrUrl', type: 'string' },
    { name: 'displayName', type: ['null', 'string'], default: null } // Table name, WiFi SSID, or Business name for filename
  ]
};

export class QRKafkaProducer {
  private registry: SchemaRegistry;
  private schemaId: number | null = null;

  constructor() {
    // Initialize Schema Registry client
    this.registry = new SchemaRegistry({
      host: process.env.SCHEMA_REGISTRY_URL || 'http://localhost:9081'
    });
  }

  async connect() {
    // Register schema if not already registered
    try {
      logger.info('📡 Connecting to Schema Registry...', { 
        url: process.env.SCHEMA_REGISTRY_URL || 'http://localhost:9081' 
      });
      
      const registeredSchema = await this.registry.register({
        type: SchemaType.AVRO,
        schema: JSON.stringify(QR_GENERATED_EVENT_SCHEMA)
      });
      
      this.schemaId = registeredSchema.id;
      logger.info('✅ QRGeneratedEvent schema registered (Outbox Pattern)', { schemaId: this.schemaId });
    } catch (error) {
      logger.error('❌ Schema registration failed - QR events will NOT be published!', { 
        error: error instanceof Error ? {
          message: error.message,
          stack: error.stack,
          name: error.name
        } : error,
        registryUrl: process.env.SCHEMA_REGISTRY_URL || 'http://localhost:9081'
      });
      throw error; // Throw to prevent service from starting with broken producer
    }
  }

  /**
   * Publish QRGeneratedEvent using Outbox Pattern
   * Event is saved to outbox table, Debezium CDC will publish to Kafka
   * Supports all QR types: BUSINESS, TABLE, WIFI
   */
  async publishQRGenerated(payload: {
    qrType: string;
    storeId: string | null;
    businessId?: string;
    tableId?: string;
    wifiSettingsId?: string;
    qrImageBase64: string;
    qrImageSize: number;
    qrUrl: string;
    displayName?: string; // Table name, WiFi SSID, or Business name for filename
  }) {
    const eventId = uuidv4();

    // Create event object
    const event = {
      eventId,
      eventType: 'QRGenerated',
      timestamp: Date.now(),
      qrType: payload.qrType,
      storeId: payload.storeId,
      businessId: payload.businessId || null,
      tableId: payload.tableId || null,
      wifiSettingsId: payload.wifiSettingsId || null,
      qrImageBase64: payload.qrImageBase64,
      qrImageSize: payload.qrImageSize,
      qrUrl: payload.qrUrl,
      displayName: payload.displayName || null // For filename in ZIP download
    };

    try {
      // Save to outbox table (Debezium will publish to Kafka)
      // aggregateId depends on QR type
      const aggregateId = payload.tableId || payload.businessId || payload.wifiSettingsId || payload.storeId || 'unknown';
      
      await outboxService.saveEvent(
        aggregateId,               // aggregateId (table/business/wifi/store)
        'qr',                      // aggregateType
        'QRGenerated',             // eventType
        'events.qr.QRGenerated',   // topic
        event,                     // eventData
        this.schemaId!             // schemaId
      );

      logger.info('📦 QRGeneratedEvent saved to outbox (Debezium will publish)', {
        eventId,
        qrType: payload.qrType,
        aggregateId,
        topic: 'events.qr.QRGenerated',
        schemaId: this.schemaId
      });

    } catch (error) {
      logger.error('❌ Failed to save QRGeneratedEvent to outbox', { error, payload });
      throw error;
    }
  }

  async disconnect() {
    logger.info('👋 Producer disconnected (using outbox pattern)');
  }
}

export const qrProducer = new QRKafkaProducer();