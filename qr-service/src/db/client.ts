import { Pool, PoolClient } from 'pg';
import { logger } from '../utils/logger';

/**
 * PostgreSQL Database Client for QR Service
 * Handles connection pooling and query execution
 */
class DatabaseClient {
  private pool: Pool;

  constructor() {
    this.pool = new Pool({
      host: process.env.DB_HOST || 'localhost',
      port: parseInt(process.env.DB_PORT || '5432'),
      database: process.env.DB_NAME || 'qr_service_db',
      user: process.env.DB_USER || 'qr_service_user',
      password: process.env.DB_PASSWORD || 'qr_service_password',
      max: 20,
      idleTimeoutMillis: 30000,
      connectionTimeoutMillis: 2000,
    });

    this.pool.on('error', (err) => {
      logger.error('💥 Unexpected database error', { error: err });
    });

    this.pool.on('connect', () => {
      logger.debug('🔌 New database connection established');
    });
  }

  /**
   * Execute a query
   */
  async query(text: string, params?: any[]) {
    const start = Date.now();
    try {
      const result = await this.pool.query(text, params);
      const duration = Date.now() - start;
      logger.debug('✅ Query executed', { 
        duration: `${duration}ms`, 
        rows: result.rowCount 
      });
      return result;
    } catch (error) {
      logger.error('❌ Query error', { error, text, params });
      throw error;
    }
  }

  /**
   * Get a client from the pool for transactions
   */
  async getClient(): Promise<PoolClient> {
    return this.pool.connect();
  }

  /**
   * Close all connections
   */
  async close() {
    await this.pool.end();
    logger.info('👋 Database pool closed');
  }

  /**
   * Test database connection
   */
  async testConnection(): Promise<boolean> {
    try {
      const result = await this.query('SELECT NOW()');
      logger.info('✅ Database connection test successful', { 
        timestamp: result.rows[0].now 
      });
      return true;
    } catch (error) {
      logger.error('❌ Database connection test failed', { error });
      return false;
    }
  }
}

export const db = new DatabaseClient();
