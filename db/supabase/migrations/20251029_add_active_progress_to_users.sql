-- Migration: add active_progress_id to users and ensure progress.task_id uniqueness
-- Created: 2025-10-29

-- 1) Add nullable column to users (safe: IF NOT EXISTS)
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS active_progress_id uuid;

-- 2) Add FK constraint from users.active_progress_id -> progress.id (only if not exists)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint c
    JOIN pg_class t ON c.conrelid = t.oid
    WHERE c.conname = 'fk_users_active_progress'
  ) THEN
    ALTER TABLE users
      ADD CONSTRAINT fk_users_active_progress
        FOREIGN KEY (active_progress_id)
        REFERENCES progress (id)
        ON DELETE SET NULL;
  END IF;
END$$;

-- 3) Index to speed up lookups of active progress
CREATE INDEX IF NOT EXISTS idx_users_active_progress_id ON users (active_progress_id);

-- 4) Ensure progress.task_id is unique (1:1 task -> progress). Add constraint only if not exists.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint c
    WHERE c.conname = 'uq_progress_task'
  ) THEN
    ALTER TABLE progress
      ADD CONSTRAINT uq_progress_task UNIQUE (task_id);
  END IF;
END$$;

-- 5) Helpful indexes for progress (if not already present)
CREATE INDEX IF NOT EXISTS idx_progress_user_id ON progress (user_id);
CREATE INDEX IF NOT EXISTS idx_progress_user_created_at ON progress (user_id, created_at DESC);

-- NOTE:
-- Run this migration after the base schema (users, tasks, progress) exists.
-- If your migration runner supports transactions per migration, the statements above will be atomic.
-- If table or constraint names differ, adjust accordingly.
