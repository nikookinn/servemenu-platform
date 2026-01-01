/**
 * QR Service Entry Point
 * Professional QR generation with full customization support
 */

// CRITICAL: Load in EXACT order!
import 'dotenv/config';           // 1. Environment variables
// NOTE: polyfills.ts removed - Puppeteer uses real browser, no need for polyfills
import { qrConsumer } from './kafka/consumer';
import { qrProducer } from './kafka/producer';
import { logger } from './utils/logger';
import { db } from './db/client';
import { migrationRunner } from './db/migrate';
import { outboxService } from './services/outbox.service';
import express from 'express';
import { register, collectDefaultMetrics, Counter, Histogram } from 'prom-client';

const app = express();
const PORT = process.env.PORT || 3001;

// Prometheus metrics
collectDefaultMetrics();

export const qrGenerationCounter = new Counter({
  name: 'qr_generation_total',
  help: 'Total number of QR codes generated',
  labelNames: ['status']
});

export const qrGenerationDuration = new Histogram({
  name: 'qr_generation_duration_seconds',
  help: 'Duration of QR code generation in seconds',
  buckets: [0.1, 0.5, 1, 2, 5]
});

// Health check
app.get('/health', (req, res) => {
  res.json({
    status: 'healthy',
    service: 'qr-service',
    timestamp: new Date().toISOString()
  });
});

// Readiness check
app.get('/ready', (req, res) => {
  res.json({
    status: 'ready',
    kafka: 'connected'
  });
});

// Prometheus metrics
app.get('/metrics', async (req, res) => {
  res.set('Content-Type', register.contentType);
  res.end(await register.metrics());
});

async function start() {
  try {
    logger.info('🚀 Starting QR Service...');

    // 1. Test database connection
    logger.info('📊 Testing database connection...');
    const dbReady = await db.testConnection();
    if (!dbReady) {
      throw new Error('Database connection failed');
    }

    // 2. Run migrations
    logger.info('🔄 Running database migrations...');
    await migrationRunner.runMigrations();

    // 3. Connect to Kafka
    logger.info('📡 Connecting to Kafka...');
    await qrProducer.connect();
    await qrConsumer.connect();
    await qrConsumer.start();

    // 4. Schedule outbox cleanup (every 6 hours)
    const CLEANUP_INTERVAL_MS = 6 * 60 * 60 * 1000; // 6 hours
    setInterval(async () => {
      try {
        logger.info('🧹 Running scheduled outbox cleanup...');
        const deletedCount = await outboxService.cleanupOldEvents(7); // Delete events older than 7 days
        logger.info(`🧹 Outbox cleanup completed: ${deletedCount} events deleted`);
      } catch (error) {
        logger.error('❌ Outbox cleanup failed', { error });
      }
    }, CLEANUP_INTERVAL_MS);

    // 5. Start HTTP server
    app.listen(PORT, () => {
      logger.info(`✅ QR Service started on port ${PORT}`);
      logger.info('🎨 Ready to generate professional QR codes');
      logger.info('📦 Outbox pattern enabled - events will be published via Debezium CDC');
      logger.info('🧹 Outbox cleanup scheduled every 6 hours (deletes events older than 7 days)');
    });

  } catch (error) {
    logger.error('❌ Failed to start QR Service', { 
      error: error instanceof Error ? {
        message: error.message,
        stack: error.stack,
        name: error.name
      } : error
    });
    process.exit(1);
  }
}

// Graceful shutdown
process.on('SIGTERM', async () => {
  logger.info('👋 SIGTERM received, shutting down...');
  await qrConsumer.disconnect();
  await qrProducer.disconnect();
  process.exit(0);
});

process.on('SIGINT', async () => {
  logger.info('👋 SIGINT received, shutting down...');
  await qrConsumer.disconnect();
  await qrProducer.disconnect();
  process.exit(0);
});

start();