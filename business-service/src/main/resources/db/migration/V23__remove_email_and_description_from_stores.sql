-- Remove email and description columns from stores table
-- Migration: V23
-- Date: 2025-01-09
-- Description: Remove unused email and description fields from stores table

ALTER TABLE stores DROP COLUMN IF EXISTS email;
ALTER TABLE stores DROP COLUMN IF EXISTS description;
