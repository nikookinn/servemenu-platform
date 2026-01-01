/**
 * Kafka Consumer for QR Generation Events with Avro deserialization
 * Handles: TABLE, BUSINESS, and WIFI QR code generation
 * Uses Schema Registry to deserialize Avro binary messages
 */

import { Kafka, Consumer, EachMessagePayload, CompressionTypes, CompressionCodecs } from 'kafkajs';
import SnappyCodec from 'kafkajs-snappy';
import { SchemaRegistry } from '@kafkajs/confluent-schema-registry';
import { logger } from '../utils/logger';
import { QRGeneratorCanvasService } from '../services/qr-generator-canvas.service';
import { qrProducer } from './producer';
import { keycloakAuthService } from '../services/keycloak-auth.service';
import type { QRCustomization } from '../types/qr-customization.types';
import axios from 'axios';

// Register Snappy compression codec
CompressionCodecs[CompressionTypes.Snappy] = SnappyCodec;

export class QRKafkaConsumer {
  private kafka: Kafka;
  private consumer: Consumer;
  private apiGatewayUrl: string;
  private schemaRegistry: SchemaRegistry;

  constructor() {
    this.kafka = new Kafka({
      clientId: 'qr-service',
      brokers: (process.env.KAFKA_BROKERS || 'localhost:9092,localhost:9093').split(',')
    });

    this.consumer = this.kafka.consumer({
      groupId: 'qr-service-group-v6', // New group after Avro migration
      sessionTimeout: 30000,
      heartbeatInterval: 3000
    });

    // Initialize Schema Registry for Avro deserialization
    this.schemaRegistry = new SchemaRegistry({
      host: process.env.SCHEMA_REGISTRY_URL || 'http://localhost:9081'
    });

    // Use API Gateway for service-to-service communication
    // Gateway provides: service discovery, load balancing, circuit breaker, monitoring
    this.apiGatewayUrl = process.env.API_GATEWAY_URL || 'http://localhost:8080';
  }

  async connect() {
    await this.consumer.connect();
    logger.info('✅ Kafka consumer connected');

    // Subscribe to QRGenerationRequested topic only
    // All QR types (TABLE, BUSINESS, WIFI) use this single event
    await this.consumer.subscribe({
      topics: ['events.business.QRGenerationRequested'],
      fromBeginning: false
    });

    logger.info('✅ Subscribed to QR generation topics');
  }

  async start() {
    await this.consumer.run({
      eachMessage: async (payload: EachMessagePayload) => {
        await this.handleMessage(payload);
      }
    });

    logger.info('🚀 QR Service consumer started with Avro deserialization');
  }

  private async handleMessage({ topic, partition, message }: EachMessagePayload) {
    try {
      if (!message.value) {
        logger.warn('Received empty message');
        return;
      }

      // ✅ Deserialize Avro binary message using Schema Registry
      const decodedMessage = await this.schemaRegistry.decode(message.value) as any;

      logger.info('📨 Received QR Event (Avro)', {
        eventType: decodedMessage.eventType,
        topic
      });

      // Route to appropriate handler based on event type
      if (decodedMessage.eventType === 'QR_GENERATION_REQUESTED') {
        await this.processQRGenerationRequested(decodedMessage);
      } else {
        logger.debug('Ignoring unknown event type', { eventType: decodedMessage.eventType });
      }

    } catch (error) {
      logger.error('❌ Error processing message', {
        error: error instanceof Error ? {
          message: error.message,
          stack: error.stack,
          name: error.name
        } : error,
        topic,
        partition,
        messageOffset: message.offset,
        messageKey: message.key?.toString()
      });
      throw error;
    }
  }

  private async processTableCreated(event: any, retryCount: number = 0): Promise<void> {
    const { tableId, storeId, businessId, qrUrl } = event;
    const MAX_RETRIES = 3;
    const RETRY_DELAY_MS = 1000;

    try {
      logger.info('🎨 Processing TABLE_CREATED event', { tableId, storeId, businessId, qrUrl, retryCount });

      if (!qrUrl) {
        logger.error('❌ qrUrl is null or undefined in event', { tableId, storeId, event });
        throw new Error('qrUrl is required but was null or undefined');
      }

      // 1. Fetch QR customization from Business Service (with businessId for security)
      const customizationStart = Date.now();
      const customization = await this.fetchQRCustomizationByType('TABLE', storeId, businessId);
      const customizationTime = Date.now() - customizationStart;
      logger.info(`⏱️ QR customization fetched in ${customizationTime}ms`, { storeId, businessId });

      // Generate QR code with customization using Canvas (fast + beautiful!)
      const qrGenStart = Date.now();
      const qrGeneratorService = new QRGeneratorCanvasService();
      const qrImageBase64 = await qrGeneratorService.generateQR(qrUrl, customization);
      const qrGenTime = Date.now() - qrGenStart;
      logger.info(`⏱️ Total QR generation time: ${qrGenTime}ms`, { tableId, qrUrl });

      // 3. Calculate image size
      const qrImageSize = Buffer.from(qrImageBase64.split(',')[1], 'base64').length;

      logger.info('✅ QR code generated', {
        tableId,
        size: qrImageSize,
        sizeKB: (qrImageSize / 1024).toFixed(2)
      });

      // 4. Publish QRGeneratedEvent to Kafka
      await qrProducer.publishQRGenerated({
        qrType: 'TABLE',
        storeId,
        tableId,
        qrImageBase64,
        qrImageSize,
        qrUrl
      });

      logger.info('✅ QRGeneratedEvent published', { tableId });

    } catch (error) {
      logger.error('❌ Failed to process TABLE_CREATED event', {
        error,
        tableId,
        storeId,
        retryCount
      });

      if (retryCount < MAX_RETRIES) {
        const delay = RETRY_DELAY_MS * Math.pow(2, retryCount);
        logger.warn(`⏳ Retrying in ${delay}ms... (attempt ${retryCount + 1}/${MAX_RETRIES})`, { tableId });

        await new Promise(resolve => setTimeout(resolve, delay));
        return this.processTableCreated(event, retryCount + 1);
      } else {
        logger.error('💀 Max retries exceeded for TABLE_CREATED event', { tableId, storeId });
        throw error;
      }
    }
  }

  private async processQRGenerationRequested(event: any, retryCount: number = 0): Promise<void> {
    const { storeId, qrType, targetUrl } = event;
    const MAX_RETRIES = 3;
    const RETRY_DELAY_MS = 1000;

    try {
      logger.info('🎨 Processing QR_GENERATION_REQUESTED event', { 
        qrType, 
        storeId, 
        retryCount 
      });

      if (!targetUrl) {
        logger.error('❌ targetUrl is missing in event', { qrType, storeId, event });
        throw new Error('targetUrl is required but was missing');
      }

      // 1. Fetch QR customization by type
      const customizationStart = Date.now();
      const customization = await this.fetchQRCustomizationByType(
        qrType, 
        qrType === 'BUSINESS' ? event.businessId : storeId,
        event.businessId // Pass businessId for store-level QR
      );
      const customizationTime = Date.now() - customizationStart;
      logger.info(`⏱️ QR customization fetched in ${customizationTime}ms`, { 
        qrType, 
        id: qrType === 'BUSINESS' ? event.businessId : storeId,
        businessId: event.businessId
      });

      // 2. Generate QR code with customization
      const qrGenStart = Date.now();
      const qrGeneratorService = new QRGeneratorCanvasService();
      const qrImageBase64 = await qrGeneratorService.generateQR(targetUrl, customization);
      const qrGenTime = Date.now() - qrGenStart;
      logger.info(`⏱️ QR generation time: ${qrGenTime}ms`, { qrType, targetUrl });

      // 3. Calculate image size
      const qrImageSize = Buffer.from(qrImageBase64.split(',')[1], 'base64').length;

      logger.info('✅ QR code generated', {
        qrType,
        size: qrImageSize,
        sizeKB: (qrImageSize / 1024).toFixed(2)
      });

      // 4. Publish QRGeneratedEvent to Kafka with QR type info
      await qrProducer.publishQRGenerated({
        qrType,
        storeId,
        businessId: event.businessId,
        tableId: event.metadata?.tableId,
        wifiSettingsId: event.metadata?.wifiSettingsId,
        qrImageBase64,
        qrImageSize,
        qrUrl: targetUrl,
        displayName: event.metadata?.displayName // Table name, WiFi SSID, or Business name
      });

      logger.info('✅ QRGeneratedEvent published', { qrType, storeId });

    } catch (error) {
      logger.error('❌ Failed to process QR_GENERATION_REQUESTED event', {
        error,
        qrType,
        storeId,
        retryCount
      });

      if (retryCount < MAX_RETRIES) {
        const delay = RETRY_DELAY_MS * Math.pow(2, retryCount);
        logger.warn(`⏳ Retrying in ${delay}ms... (attempt ${retryCount + 1}/${MAX_RETRIES})`, { qrType });

        await new Promise(resolve => setTimeout(resolve, delay));
        return this.processQRGenerationRequested(event, retryCount + 1);
      } else {
        logger.error('💀 Max retries exceeded for QR_GENERATION_REQUESTED event', { qrType, storeId });
        throw error;
      }
    }
  }

  /**
   * Fetch QR customization by type (TABLE, BUSINESS, WIFI)
   * BUSINESS QR: uses businessId and /businesses/{businessId}/qr-customization
   * TABLE/WIFI QR: uses businessId + storeId and /businesses/{businessId}/stores/{storeId}/qr-customizations/{qrType}
   */
  private async fetchQRCustomizationByType(qrType: string, entityId: string, businessId?: string): Promise<QRCustomization> {
    try {
      logger.debug('🔍 Fetching QR customization by type', { qrType, entityId, businessId });

      // Get OAuth2 access token from Keycloak using Client Credentials Flow
      const accessToken = await keycloakAuthService.getAccessToken();

      // Build endpoint based on QR type
      let endpoint: string;
      if (qrType === 'BUSINESS') {
        // Business-level QR: /api/v1/businesses/{businessId}/qr-customization (singular, no type suffix)
        endpoint = `${this.apiGatewayUrl}/business-service/api/v1/businesses/${entityId}/qr-customization`;
      } else {
        // Store-level QR: /api/v1/businesses/{businessId}/stores/{storeId}/qr-customizations/{qrType}
        // entityId is storeId here, businessId must be provided
        if (!businessId) {
          throw new Error('businessId is required for store-level QR customization');
        }
        endpoint = `${this.apiGatewayUrl}/business-service/api/v1/businesses/${businessId}/stores/${entityId}/qr-customizations/${qrType}`;
      }

      const response = await axios.get<{ data: QRCustomization }>(endpoint, {
        headers: {
          'Authorization': `Bearer ${accessToken}`
        },
        timeout: 5000
      });

      logger.info('✅ QR customization fetched', { 
        qrType,
        entityId,
        frameType: response.data.data.frameType
      });
      return response.data.data;

    } catch (error) {
      logger.error('❌ Failed to fetch QR customization', { error, qrType, entityId });
      logger.warn('⚠️ Using default QR customization', { qrType, entityId });
      return this.getDefaultCustomization(entityId);
    }
  }

  private getDefaultCustomization(storeId: string): QRCustomization {
    return {
      id: 'default',
      storeId,
      backgroundColor: '#FFFFFF',
      patternColorMode: 'single',
      patternColorSingle: '#000000',
      patternType: 'rounded',
      eyeType: 'square',
      eyeColorEnabled: false,
      frameType: 'none',
      margin: 1,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
  }

  async disconnect() {
    await this.consumer.disconnect();
    logger.info('👋 Kafka consumer disconnected');
  }
}

export const qrConsumer = new QRKafkaConsumer();