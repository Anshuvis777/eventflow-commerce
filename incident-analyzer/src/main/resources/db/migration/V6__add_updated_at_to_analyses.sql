-- V6: Add updated_at column to analyses table
ALTER TABLE analyses ADD COLUMN updated_at TIMESTAMPTZ;
