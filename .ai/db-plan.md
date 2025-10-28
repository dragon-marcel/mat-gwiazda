# Schemat bazy danych PostgreSQL — MatGwiazda (MVP)

Poniższy dokument zawiera kompletny schemat bazy danych zaprojektowany dla aplikacji MatGwiazda (MVP). Zawiera definicje tabel, kolumn, typów danych, ograniczeń, indeksów, relacji oraz przykładowe reguły Row Level Security (RLS) zgodne z wymaganiami PRD i notatek sesji planowania.

---

## 1. Lista tabel z ich kolumnami, typami danych i ograniczeniami

Uwaga: klucze główne są typu UUID (zalecane dla rozproszonego środowiska). Czasowe pola wykorzystują timestamptz.

### 1.1. `users` — dane kont użytkowników i ich status
Tabela users bedzie wykorzystywana w Supabase Auth do zarządzania uwierzytelnianiem.
CREATE TABLE (opis):
- id: uuid PRIMARY KEY DEFAULT gen_random_uuid()
- email: varchar(255) NOT NULL UNIQUE
- password: varchar(255) NOT NULL -- hash hasła (bcrypt/argon2)
- user_name: varchar(100) -- NOT NULL
- role: user_role NOT NULL DEFAULT 'student' -- enum (student, teacher, admin)
- current_level: smallint NOT NULL DEFAULT 1 CHECK (current_level >= 1 AND current_level <= 8)
- points: integer NOT NULL DEFAULT 0 CHECK (points >= 0)
- stars: integer NOT NULL DEFAULT 0 CHECK (stars >= 0)
- is_active: boolean NOT NULL DEFAULT true
- created_at: timestamptz NOT NULL DEFAULT now()
- updated_at: timestamptz NOT NULL DEFAULT now()
- last_active_at: timestamptz

Notatki:
- `points` i `stars` są przechowywane tutaj dla szybkiego odczytu profilu użytkownika.
- Większe operacje akumulacji punktów będą zapisywane również w tabeli `progress` (historycznie).


### 1.2. `tasks` — repozytorium zadań (treść zadania, opcje, poprawna odpowiedź, wyjaśnienie)

CREATE TABLE (opis):
- id: uuid PRIMARY KEY DEFAULT gen_random_uuid()
- level: smallint NOT NULL CHECK (level >= 1 AND level <= 8)
- prompt: text NOT NULL -- treść zadania (krótka), jeżeli zadania są generowane przez AI można tu przechowywać wzorce/stan
- options: jsonb NOT NULL CHECK (jsonb_typeof(options) = 'array') -- tablica 4 elementów z tekstami odpow.; format: ["A", "B", "C", "D"]
- correct_option_index: smallint NOT NULL CHECK (correct_option_index >= 0) -- indeks poprawnej odpowiedzi w tablicy options (0..3)
- explanation: text -- krótkie wyjaśnienie rozwiązania
- created_by: uuid REFERENCES users(id) ON DELETE SET NULL -- identyfikator twórcy (system/AI/admin)
- is_active: boolean NOT NULL DEFAULT true
- metadata: jsonb -- dodatkowe dane (np. tags, source=ai, template_id)
- created_at: timestamptz NOT NULL DEFAULT now()
- updated_at: timestamptz NOT NULL DEFAULT now()

Notatki:
- `options` jako JSONB daje elastyczność (np. lokalizacje, dodatkowe atrybuty przy opcjach). Jeśli preferowane jest pełne normalizowanie, można zamiast tego wprowadzić tabelę `task_options` z FK do `tasks`.
- Zakładamy 4 opcje na zadanie (zgodnie z PRD). Aplikacja powinna walidować długość tablicy `options` i zakres `correct_option_index`.
- W modelu MVP zadania będą generowane dynamicznie dla każdej próby użytkownika i nie będą powtarzane. Dlatego każde wygenerowane `tasks` jest powiązane z dokładnie jednym wpisem w `progress` (relacja 1:1).


### 1.3. `progress` — historia prób/rozwiązań użytkowników (główna tabela analityczna)

CREATE TABLE (opis):
- id: uuid PRIMARY KEY DEFAULT gen_random_uuid()
- user_id: uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE
- task_id: uuid NOT NULL REFERENCES tasks(id) ON DELETE CASCADE ON UPDATE CASCADE  -- each task instance is unique per attempt
- selected_option_index: smallint -- indeks wybranej opcji (NULL jeśli użytkownik przerwał)
- is_correct: boolean NOT NULL
- points_awarded: integer NOT NULL DEFAULT 0 CHECK (points_awarded >= 0)
- time_taken_ms: integer -- czas w ms spędzony na zadaniu
- updated_at: timestamptz NOT NULL DEFAULT now()

Notatki:
- `progress` łączy `users` z `tasks`, ale w tej wersji relacja `tasks` -> `progress` jest jeden-do-jednego (1 task instance = 1 progress record). Aby wymusić to zachowanie, dodajemy unikalne ograniczenie na `progress.task_id`.
- FK z `ON DELETE CASCADE` zgodnie z decyzją sesji planowania: usunięcie użytkownika usuwa powiązane rekordy historyczne; usunięcie instancji zadania usuwa powiązany wpis w `progress`.


## 2. Relacje między tabelami (kardynalność)

- users 1 --- * progress
  - Jeden użytkownik może mieć wiele wpisów w `progress` (wiele prób / wygenerowanych zadań dla jednego użytkownika).
- tasks 1 --- 1 progress
  - Każde wygenerowane zadanie jest unikalne i powiązane z dokładnie jednym wpisem `progress` (zadania są generowane per-attempt).

Kardynalność: relacja `users` <-> `tasks` nie jest modelowana jako tradycyjne wiele-do-wielu; zamiast tego `progress` przechowuje próby użytkownika, a każde `task` jest jednorazową instancją powiązaną 1:1 z `progress`.

---

## 3. Indeksy (zalecane)

Tworzenie indeksów do szybkiego wyszukiwania i agregacji:

- UNIQUE INDEX na `users(email)` (już wymieniony jako UNIQUE constraint).

- Indexy na `progress`:
  - CREATE INDEX idx_progress_user_id ON progress (user_id);
  - CREATE UNIQUE INDEX idx_progress_task_id_unique ON progress (task_id); -- gwarantuje relację 1:1 tasks->progress
  - CREATE INDEX idx_progress_user_created_at ON progress (user_id, created_at DESC);
  - CREATE INDEX idx_progress_user_is_correct ON progress (user_id, is_correct);

- Indexy na `tasks`:
  - CREATE INDEX idx_tasks_level_active ON tasks (level, is_active);
  - CREATE INDEX idx_tasks_created_by ON tasks (created_by);
  - CREATE INDEX idx_tasks_metadata_gin ON tasks USING GIN (metadata);
  - (opcjonalnie) CREATE INDEX idx_tasks_options_gin ON tasks USING GIN (options jsonb_path_ops);
    - Uwaga: używaj GIN dla wyszukiwań w JSONB; jsonb_path_ops jest szybszy dla niektórych operacji (ale ograniczony).

- Indexy na `users`:
  - CREATE INDEX idx_users_current_level ON users (current_level);
  - CREATE INDEX idx_users_last_active_at ON users (last_active_at DESC);

Wskazówki:
- Indeksy kompozytowe (np. user_id + created_at) przyspieszają odczyt historii użytkownika i budowanie dashboardów.
- GIN dla pól jsonb tylko gdy rzeczywiście wyszukujemy po polach JSON.

---

## 4. Zasady PostgreSQL (Row Level Security - RLS) i polityki dostępu

Założenia bezpieczeństwa:
- Użytkownik powinien móc czytać i modyfikować własne wpisy w `progress` oraz odczytywać swój wiersz w `users`.
- Role administracyjne (np. `admin`) mają dostęp do pełnych danych.
- Supabase oferuje własny system auth; poniższe polityki zakładają standardowe role Postgres (`app_user` jako rola aplikacji) lub role Supabase i mechanizm JWT.

Przykładowe polecenia SQL do włączenia RLS i utworzenia polityk (do zaadaptowania w migracjach):

-- Włączenie RLS dla tabeli progress
ALTER TABLE progress ENABLE ROW LEVEL SECURITY;

-- Polityka: użytkownicy mogą wstawiać swoje własne wpisy (aplikacja będzie zapewniać user_id z jwt)
CREATE POLICY progress_insert_own ON progress FOR INSERT USING (true) WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

-- Polityka: użytkownicy mogą czytać tylko swoje wiersze
CREATE POLICY progress_select_own ON progress FOR SELECT USING (user_id = current_setting('app.current_user_id')::uuid);

-- Polityka: użytkownicy mogą aktualizować tylko swoje wiersze
CREATE POLICY progress_update_own ON progress FOR UPDATE USING (user_id = current_setting('app.current_user_id')::uuid) WITH CHECK (user_id = current_setting('app.current_user_id')::uuid);

-- Polityka: admin (rola) może mieć pełny dostęp (przykład)
CREATE POLICY progress_admin ON progress FOR ALL TO role_admin USING (true);

-- Podobne polityki dla users: użytkownicy mogą SELECT/UPDATE swój własny wiersz
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
CREATE POLICY users_select_own ON users FOR SELECT USING (id = current_setting('app.current_user_id')::uuid);
CREATE POLICY users_update_own ON users FOR UPDATE USING (id = current_setting('app.current_user_id')::uuid) WITH CHECK (id = current_setting('app.current_user_id')::uuid);

Uwagi dotyczące `current_setting('app.current_user_id')`:
- W migracjach i konfiguracji serwera aplikacja (Spring Boot) przed zapytaniem ustawia wartość kontekstu: `SET LOCAL app.current_user_id = '<user-uuid>';` lub używa mechanizmu Supabase JWT claims mapowania (np. `auth.uid()` w Supabase).
- Alternatywnie, gdy używasz Supabase, możesz dopasować polityki do `auth.uid()` oraz ról Supabase.

---

## 5. Ograniczenia, checki i integralność danych

- `users.email` UNIQUE NOT NULL.
- `users.current_level` CHECK (1..8).
- `tasks.options` musi być tablicą JSON o długości 4 — wymuszane na poziomie aplikacji lub przez trigger.
- `tasks.correct_option_index` musi wskazywać istniejący indeks w `options` — walidacja aplikacyjna lub trigger/constraint.
- `progress.points_awarded` nieujemne.
- Wszystkie FK mają `ON DELETE/UPDATE CASCADE` tam gdzie zadecydowano w sesji (szczególnie w `progress`). W `tasks.created_by` zastosowano `ON DELETE SET NULL` by zachować historię zadań przy usunięciu użytkownika-tworcy.

Przykładowy check na długość tablicy options (możliwy do dodania jako check wykorzystujący jsonb_array_length):
- CHECK (jsonb_typeof(options) = 'array' AND jsonb_array_length(options) = 4)

---

## 6. Przykładowe polecenia CREATE TABLE (skrócone, gotowe do adaptacji jako migracje)

-- Typ enum dla roli użytkownika
CREATE TYPE user_role AS ENUM ('student', 'teacher', 'admin');

-- users
CREATE TABLE users (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email varchar(255) NOT NULL UNIQUE,
  password varchar(255) NOT NULL,
  user_name varchar(100) NOT NULL,
  role user_role NOT NULL DEFAULT 'student',
  current_level smallint NOT NULL DEFAULT 1 CHECK (current_level >= 1 AND current_level <= 8),
  points integer NOT NULL DEFAULT 0 CHECK (points >= 0),
  stars integer NOT NULL DEFAULT 0 CHECK (stars >= 0),
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  last_active_at timestamptz
);

-- tasks
CREATE TABLE tasks (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  level smallint NOT NULL CHECK (level >= 1 AND level <= 8),
  prompt text NOT NULL,
  options jsonb NOT NULL CHECK (jsonb_typeof(options) = 'array' AND jsonb_array_length(options) = 4),
  correct_option_index smallint NOT NULL CHECK (correct_option_index >= 0 AND correct_option_index < 4),
  explanation text,
  created_by uuid REFERENCES users(id) ON DELETE SET NULL,
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
-- progress
CREATE TABLE progress (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
  task_id uuid NOT NULL REFERENCES tasks(id) ON DELETE CASCADE ON UPDATE CASCADE,
  attempt_number integer NOT NULL DEFAULT 1,
  selected_option_index smallint,
  level_updated boolean NOT NULL DEFAULT false,
  is_correct boolean NOT NULL,
  points_awarded integer NOT NULL DEFAULT 0 CHECK (points_awarded >= 0),
  created_at timestamptz NOT NULL DEFAULT now(),
  selected_option_index smallint NOT NULL CHECK (selected_option_index >= 0 AND selected_option_index < 4),
  updated_at timestamptz NOT NULL DEFAULT now(),

  updated_at timestamptz NOT NULL DEFAULT now()
);

## 6a. `learning_levels` — opis kompetencji / zakresu dla poziomów 1..8

Aby w przejrzysty sposób przechowywać opis zakresu materiału i reguły generowania zadań dla poszczególnych poziomów, dodajemy tabelę `learning_levels`. Pozwala to na łatwe rozszerzanie i edytowanie opisów poziomów z audytem kto i kiedy wprowadził zmianę.

CREATE TABLE (opis):
- level: smallint PRIMARY KEY CHECK (level >= 1 AND level <= 8) -- numer poziomu
- title: varchar(128) NOT NULL -- krótka nazwa poziomu (opcjonalnie)
- description: text NOT NULL -- szczegółowy opis zakresu (użyj poniższych tekstów)
- created_by: uuid REFERENCES users(id) ON DELETE SET NULL -- kto utworzył wpis
- created_at: timestamptz NOT NULL DEFAULT now() -- kiedy utworzono
- modified_by: uuid REFERENCES users(id) ON DELETE SET NULL -- kto ostatnio modyfikował
- modified_at: timestamptz -- kiedy ostatnio zmodyfikowano

Przykładowa definicja SQL do migracji:

-- learning_levels
CREATE TABLE learning_levels (
  level smallint PRIMARY KEY CHECK (level >= 1 AND level <= 8),
  title varchar(128) NOT NULL,
  description text NOT NULL,
  created_by uuid REFERENCES users(id) ON DELETE SET NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  modified_by uuid REFERENCES users(id) ON DELETE SET NULL,
  modified_at timestamptz
);

-- Opcjonalnie: powiązać `tasks.level` jako FK do `learning_levels(level)` zamiast surowego CHECK na zakres
-- ALTER TABLE tasks ADD CONSTRAINT fk_tasks_level_learning_levels FOREIGN KEY (level) REFERENCES learning_levels(level);

-- Seed / przykładowe wpisy dla poziomów 1..8 (użyj w migracji lub skrypcie seedującym):
INSERT INTO learning_levels (level, title, description) VALUES
  (1, 'Poziom 1', 'Dodawanie i odejmowanie w zakresie 100, porównywanie liczb, proste zadania tekstowe.'),
  (2, 'Poziom 2', 'Mnożenie i dzielenie w zakresie 100, proste ułamki.'),
  (3, 'Poziom 3', 'Działania do 1000, tabliczka mnożenia, dzielenie z resztą, ułamki zwykłe, jednostki miary (długość, masa, czas).'),
  (4, 'Poziom 4', 'Liczby wielocyfrowe, ułamki i ich porównywanie.'),
  (5, 'Poziom 5', 'Ułamki dziesiętne, procenty, wyrażenia algebraiczne.'),
  (6, 'Poziom 6', 'Działania na ułamkach, proporcje, średnia arytmetyczna.'),
  (7, 'Poziom 7', 'Potęgi i pierwiastki, równania i nierówności, obliczenia procentowe.'),
  (8, 'Poziom 8', 'Funkcje liniowe, układy równań, twierdzenie Pitagorasa, statystyka i prawdopodobieństwo.');

Notatki do implementacji:
- `description` powinno być tekstem sformatowanym (markdown / HTML) jeśli planujesz je wyświetlać w panelu administracyjnym.
- `created_by`/`modified_by` zakładają istnienie konta użytkownika (np. admin) — migracja seedująca może ustawić `created_by` NULL lub wskazać konto systemowe.
- Jeżeli chcesz później umożliwić wersjonowanie opisów poziomów, można dodać tabelę `learning_levels_history` z pełnym śladem zmian.

---

(Umieszczono powyżej jako uzupełnienie do sekcji CREATE TABLE i mapy konceptualnej; zadbaj o dodanie migracji SQL do repozytorium migracji, np. Flyway/Liquibase, aby zapewnić spójność środowisk.)

## 7. Dodatkowe uwagi i decyzje projektowe

1. Stosowanie JSONB w `tasks.options` i `tasks.metadata` daje elastyczność dla generowanych przez AI treści (np. warianty tłumaczeń, formaty renderowania). Jeśli wymagana będzie silna normalizacja raportów (np. agregowanie po treści opcji), można wprowadzić tabelę `task_options` z FK do `tasks`.

2. `progress` jest miejscem prawdy (single source of truth) dla analityki i historii. Dzięki indeksom i właściwemu modelowi można efektywnie obliczać:
   - liczbę ukończonych zadań,
   - zdobyte punkty w czasie,
   - tempo rozwiązywania zadań,
   - procent poprawnych odpowiedzi.

3. Aktualizacja `users.points` i `users.stars`:
   - Można je utrzymywać w czasie rzeczywistym (transakcja aktualizująca `users` + insert do `progress`) — MVP nie wymaga skomplikowanych transakcyjnych blokad.
   - Alternatywnie, prowadzić batchowe przeliczanie punktów z `progress` (cron) w przypadku skalowania dużej liczby użytkowników.

4. Polityki RLS muszą być zsynchronizowane z mechanizmem uwierzytelniania (Supabase lub JWT używany przez Spring Boot). W Supabase rekomenduje się użycie `auth.uid()` w politykach zamiast `current_setting`.

5. Backup i retencja: zaprojektuj politykę usuwania danych (np. anonimizacja / soft delete) zgodnie z wymaganiami prawnymi (RODO) i PRD. W tym szkicu nie wprowadzono pola `deleted_at` — można dodać `is_active`/`deleted_at` do każdej tabeli biznesowej.

6. Monitorowanie i metryki: rozważ tworzenie widoków agregujących (materialized views) dla dashboardów (np. dzienne sumy punktów, nowe gwiazdki), które będą odświeżane periodycznie.

---

## 8. Mapa zgodności z wymaganiami PRD / sesji planowania

- Rejestracja i logowanie: `users` (email, password) — DONE
- Generowanie zadań przez AI: `tasks` (prompt, options, correct_option_index, explanation, metadata) — DONE (z opcją oznaczenia źródła AI w `metadata`)
- System punktów i gwiazdek: `progress` zapisuje punkty_awarded; `users.points` i `users.stars` przechowują bieżący stan — DONE
- Śledzenie ukończonych zadań i czasu: `progress.time_taken_ms`, `created_at` — DONE
- RLS: przykładowe polityki i wskazówki (Supabase/Spring Boot) — DONE

---

Plik ten jest gotowy do użycia jako baza do tworzenia migracji SQL lub migracji w narzędziu takim jak Flyway / Liquibase. Zalecane kroki następcze:
- Doprecyzować mapowanie ról i integrację JWT (Supabase) z politykami RLS.
- Zaimplementować migracje SQL z powyższych CREATE TABLE i CREATE TYPE.
- Dodać testy migracji i krótkie skrypty smoke-test (np. insert/select, sprawdzenie RLS).
