-- Migration: create learning_levels table and seed levels 1..8
-- Created: 2025-10-28

BEGIN;

-- Create learning_levels table
CREATE TABLE IF NOT EXISTS learning_levels (
  level smallint PRIMARY KEY CHECK (level >= 1),
  title varchar(128) NOT NULL,
  description text NOT NULL,
  created_by uuid REFERENCES users(id) ON DELETE SET NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  modified_by uuid REFERENCES users(id) ON DELETE SET NULL,
  modified_at timestamptz
);

-- Seed values for levels 1..8 (idempotent using ON CONFLICT DO NOTHING)
INSERT INTO learning_levels (level, title, description)
VALUES
  (1, 'Poziom 1', 'Dodawanie i odejmowanie w zakresie 100, porównywanie liczb, proste zadania tekstowe.'),
  (2, 'Poziom 2', 'Mnożenie i dzielenie w zakresie 100, proste ułamki.'),
  (3, 'Poziom 3', 'Działania do 1000, tabliczka mnożenia, dzielenie z resztą, ułamki zwykłe, jednostki miary (długość, masa, czas).'),
  (4, 'Poziom 4', 'Liczby wielocyfrowe, ułamki i ich porównywanie.'),
  (5, 'Poziom 5', 'Ułamki dziesiętne, procenty, wyrażenia algebraiczne.'),
  (6, 'Poziom 6', 'Działania na ułamkach, proporcje, średnia arytmetyczna.'),
  (7, 'Poziom 7', 'Potęgi i pierwiastki, równania i nierówności, obliczenia procentowe.'),
  (8, 'Poziom 8', 'Funkcje liniowe, układy równań, twierdzenie Pitagorasa, statystyka i prawdopodobieństwo.')
ON CONFLICT (level) DO NOTHING;

-- Optionally add FK from tasks.level to learning_levels(level)
-- If you prefer referential integrity between tasks and learning_levels, uncomment the ALTER below.
-- Note: ensure learning_levels are created before applying this constraint and tasks.level values are compatible.

-- ALTER TABLE tasks
--   ADD CONSTRAINT fk_tasks_level_learning_levels FOREIGN KEY (level) REFERENCES learning_levels(level);

COMMIT;

