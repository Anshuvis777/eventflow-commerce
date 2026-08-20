ALTER TABLE log_entries ADD COLUMN updated_at TIMESTAMPTZ;
ALTER TABLE similar_incidents ADD COLUMN updated_at TIMESTAMPTZ;
