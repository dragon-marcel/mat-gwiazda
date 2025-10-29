-- migration: fix missing/nullable finalized and level_up columns on progress
-- filename: 20251029_fix_progress_finalized.sql
-- purpose: ensure `finalized` and `level_up` columns exist, set default false, and make NOT NULL
-- note: run after backup. This migration is safe to run idempotently.

BEGIN;

-- Add finalized column if it doesn't exist
ALTER TABLE progress ADD COLUMN IF NOT EXISTS finalized boolean;
-- Set missing values to false
UPDATE progress SET finalized = false WHERE finalized IS NULL;
-- Ensure default and not null
ALTER TABLE progress ALTER COLUMN finalized SET DEFAULT false;
ALTER TABLE progress ALTER COLUMN finalized SET NOT NULL;

-- Add level_up column if it doesn't exist
ALTER TABLE progress ADD COLUMN IF NOT EXISTS level_up boolean;

-- If an older column `level_updated` exists (typo in earlier migration), copy its values into level_up
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'progress' AND column_name = 'level_updated'
  ) THEN
    -- copy values where level_up is null
    EXECUTE 'UPDATE progress SET level_up = level_updated WHERE level_up IS NULL';
  END IF;
END
$$;

-- Set default and not null for level_up
UPDATE progress SET level_up = false WHERE level_up IS NULL;
ALTER TABLE progress ALTER COLUMN level_up SET DEFAULT false;
ALTER TABLE progress ALTER COLUMN level_up SET NOT NULL;

COMMIT;

