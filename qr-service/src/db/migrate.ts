import { db } from './client';
import { logger } from '../utils/logger';
import * as fs from 'fs';
import * as path from 'path';

/**
 * Simple migration runner for QR Service
 * Runs SQL migration files on startup
 */
export class MigrationRunner {
  private migrationsPath: string;

  constructor() {
    this.migrationsPath = path.join(__dirname, '../../migrations');
  }

  /**
   * Run all pending migrations
   */
  async runMigrations(): Promise<void> {
    try {
      logger.info('🔄 Starting database migrations...');

      // Create migrations tracking table if not exists
      await this.createMigrationsTable();

      // Get all migration files
      const migrationFiles = this.getMigrationFiles();

      if (migrationFiles.length === 0) {
        logger.info('✅ No migrations found');
        return;
      }

      // Run each migration
      for (const file of migrationFiles) {
        await this.runMigration(file);
      }

      logger.info('✅ All migrations completed successfully');

    } catch (error) {
      logger.error('❌ Migration failed', { error });
      throw error;
    }
  }

  /**
   * Create migrations tracking table
   */
  private async createMigrationsTable(): Promise<void> {
    const query = `
      CREATE TABLE IF NOT EXISTS schema_migrations (
        id SERIAL PRIMARY KEY,
        filename VARCHAR(255) NOT NULL UNIQUE,
        executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
      );
    `;

    await db.query(query);
    logger.debug('✅ Migrations tracking table ready');
  }

  /**
   * Get all migration files sorted by name
   */
  private getMigrationFiles(): string[] {
    if (!fs.existsSync(this.migrationsPath)) {
      logger.warn('⚠️ Migrations directory not found', { path: this.migrationsPath });
      return [];
    }

    const files = fs.readdirSync(this.migrationsPath)
      .filter(file => file.endsWith('.sql'))
      .sort();

    return files;
  }

  /**
   * Run a single migration file
   */
  private async runMigration(filename: string): Promise<void> {
    try {
      // Check if already executed
      const checkQuery = 'SELECT id FROM schema_migrations WHERE filename = $1';
      const result = await db.query(checkQuery, [filename]);

      if (result.rows.length > 0) {
        logger.debug(`⏭️  Skipping migration (already executed): ${filename}`);
        return;
      }

      // Read migration file
      const filePath = path.join(this.migrationsPath, filename);
      const sql = fs.readFileSync(filePath, 'utf8');

      logger.info(`🔄 Running migration: ${filename}`);

      // Execute migration
      await db.query(sql);

      // Record migration
      const recordQuery = 'INSERT INTO schema_migrations (filename) VALUES ($1)';
      await db.query(recordQuery, [filename]);

      logger.info(`✅ Migration completed: ${filename}`);

    } catch (error) {
      logger.error(`❌ Migration failed: ${filename}`, { error });
      throw error;
    }
  }

  /**
   * Rollback last migration (for development)
   */
  async rollbackLastMigration(): Promise<void> {
    try {
      const query = `
        SELECT filename FROM schema_migrations 
        ORDER BY executed_at DESC 
        LIMIT 1
      `;

      const result = await db.query(query);

      if (result.rows.length === 0) {
        logger.info('ℹ️  No migrations to rollback');
        return;
      }

      const filename = result.rows[0].filename;
      logger.warn(`⚠️  Rolling back migration: ${filename}`);

      // Delete migration record
      await db.query('DELETE FROM schema_migrations WHERE filename = $1', [filename]);

      logger.info(`✅ Migration rolled back: ${filename}`);
      logger.warn('⚠️  Note: This only removes the tracking record. You must manually revert database changes!');

    } catch (error) {
      logger.error('❌ Rollback failed', { error });
      throw error;
    }
  }
}

export const migrationRunner = new MigrationRunner();
