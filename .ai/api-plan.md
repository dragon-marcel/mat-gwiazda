# REST API Plan

## Overview
This API is designed for the MatGwiazda MVP using Java + Spring Boot, Postgres (Supabase), and JWT-based authentication. It covers user account management, AI-driven task generation, answer submission, progress tracking, and admin operations.

---

## 1. Resources
- users -> table `users`
- tasks -> table `tasks` (task instances generated per attempt)
- progress -> table `progress` (user attempts / history)
- auth -> authentication endpoints (login/register, token refresh)
- admin -> administrative endpoints for management (optional, role-protected)

---

## 2. Endpoints

Common notes:
- All endpoints that modify or read user-specific data require Authorization: Bearer <JWT>.
- Pagination query params: `page` (>=0), `size` (1..100), `sort` (field[,asc|desc]).
- Standard success codes: 200 OK, 201 Created, 204 No Content.
- Standard error codes: 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 409 Conflict, 429 Too Many Requests, 500 Internal Server Error.

### Auth

1) POST /api/v1/auth/register
- Description: Register a new user.
- Request JSON:
  {
    "email": "user@example.com",
    "password": "plaintext_password",
    "userName": "Janek"
  }
- Validation: email format, password min length 8, userName non-empty (<=100).
- Response 201 Created:
  {
    "id": "uuid",
    "email": "user@example.com",
    "userName": "Janek",
    "role": "student",
    "currentLevel": 1,
    "points": 0,
    "stars": 0
  }
- Errors: 400 (validation), 409 (email already exists)

2) POST /api/v1/auth/login
- Description: Authenticate and return JWT access token (+ optional refresh token).
- Request JSON:
  {"email":"user@example.com","password":"plaintext_password"}
- Response 200 OK:
  {"accessToken":"<jwt>", "expiresIn":3600, "refreshToken":"<token>"}
- Errors: 401 Unauthorized

3) POST /api/v1/auth/refresh
- Description: Refresh access token using refresh token
- Response 200 OK: {"accessToken":"<jwt>", "expiresIn":3600}

### Users

1) GET /api/v1/users/me
- Description: Get current user profile.
- Response 200:
  {
    "id":"uuid",
    "email":"...",
    "userName":"...",
    "role":"student|teacher|admin",
    "currentLevel":1,
    "points": 12,
    "stars": 0,
    "isActive": true,
    "createdAt":"iso8601",
    "updatedAt":"iso8601",
    "lastActiveAt":"iso8601"
  }
- Errors: 401

2) PATCH /api/v1/users/me
- Description: Partial update of profile (userName, password)
- Request JSON (any subset):
  {"userName":"New name","password":"new_password"}
- Validations: userName length <=100; password >=8.
- Response 200: updated user object.

3) GET /api/v1/users/{id}
- Description: Admin-only: read other users
- Response 200: user object
- Auth: role=admin

4) DELETE /api/v1/users/me
- Description: Soft-delete or disable account (implementation choice). For MVP, set `is_active=false`.
- Response 204 No Content
- Note: Deleting cascades to `progress` in DB if hard-delete used. Prefer soft-delete to preserve analytics.

### Tasks (task instances)
Tasks in DB are treated as per-attempt instances created by AI or admin. For the user flow, the server will generate or fetch a task instance and return it.

1) POST /api/v1/tasks/generate
- Description: Generate a new task for the current user at their current level (or specified level <= currentLevel+1).
- Request JSON (optional):
  {"level": 2, "source":"ai"}
- Validation: level between 1 and 8; only allow generation for levels appropriate to user (e.g. user.currentLevel or +/-1 depending policy).
- Rate limiting: per-user and per-IP (e.g. 10/min) to avoid abuse of AI cost.
- Response 201 Created:
  {
    "taskId":"uuid",
    "level":2,
    "prompt":"What is 7+5?",
    "options":["10","11","12","13"],
    "createdAt":"iso",
    "isActive":true
  }
- Side effects: create `tasks` row and a corresponding `progress` row with `is_correct=false` and no selected_option yet, or alternatively defer creating `progress` until answer submission. For strict 1:1 tasks->progress, create both now and return progressId.

2) GET /api/v1/tasks/{taskId}
- Description: Fetch a task instance (only owner or admin).
- Response 200: task object.

3) PATCH /api/v1/tasks/{taskId}
- Description: Admin/editor: update explanation, deactivate task, or patch metadata.
- Auth: role=admin or creator.
- Response 200

4) GET /api/v1/tasks?level=&isActive=&page=&size=&sort=
- Description: Admin or content editors list and filter tasks.
- Supports filters: level (1..8), isActive (true/false), createdBy, metadata tag.

### Progress (submit answers & history)

1) POST /api/v1/progress/submit
- Description: Submit an answer for a task. This implements key business logic (award points, stars, level up).
- Request JSON:
  {
    "taskId":"uuid",
    "selectedOptionIndex": 2,
    "timeTakenMs": 12345
  }
- Validation:
  - Ensure task exists and belongs to user (or task was generated for user) or that task->progress relationship is enforced.
  - selectedOptionIndex in [0..3]
- Server logic (atomic transaction):
  1. Mark `progress.selected_option_index`, `progress.is_correct` (compare with `tasks.correct_option_index`).
  2. Compute `points_awarded` (PRD: 1 point per correct answer). Could include bonus for speed/accuracy in future.
  3. Insert/update `progress` row with `points_awarded`, `time_taken_ms` and `updated_at`.
  4. Update `users.points` = users.points + points_awarded.
  5. If `users.points` >= 50 -> increment `stars` by 1, decrement `points` by 50 and increment `current_level` by 1 (max 8). Return level-up flag.
  6. Return result payload.
- Response 200:
  {
    "progressId":"uuid",
    "isCorrect": true,
    "pointsAwarded": 1,
    "userPoints": 51,
    "starsAwarded": 1, // zero or one
    "leveledUp": true,
    "newLevel": 2,
    "explanation": "Because 7+5=12"
  }
- Errors: 400 (invalid index), 403 (task not owned), 409 (task already submitted if enforcing single submission).

2) GET /api/v1/progress
- Description: Get paginated list of user's attempts.
- Query params: page,size,sort,isCorrect,from,to
- Response 200: {"items":[{progress rows}], "page":0, "size":20, "total":500}

3) GET /api/v1/progress/{id}
- Description: Get single attempt (owner or admin)

### Admin / Management

1) GET /api/v1/admin/users?filter=
- Description: Admin-only user listing and bulk operations.

2) POST /api/v1/admin/tasks
- Description: Create curated task templates (not per-attempt) for reuse. Admin creates tasks with `created_by` set.

3) DELETE /api/v1/admin/tasks/{id}
- Description: Soft-delete or deactivate a task.

---

## 3. Authentication & Authorization
- Mechanism: JWT access tokens issued from Spring Security (OAuth2 / JWT). Use refresh tokens for long-lived sessions.
- Roles: `student`, `teacher`, `admin` (from DB enum `user_role`). Map JWT claims to Spring Security roles.
- DB RLS: For Supabase/Postgres setups, enable Row Level Security and use `current_setting('app.current_user_id')` or `auth.uid()` to match policies. The API server should SET LOCAL `app.current_user_id` = <user-uuid> in DB connection/session before executing queries to honor RLS.
- Authorization rules in API:
  - Users may read/update their own `users` row and their `progress` entries.
  - Admin role can read/write all resources. Teachers may have limited elevated rights (view students in their class) - not in MVP unless required.
- Rate limiting: Protect `tasks/generate` and `auth` endpoints. Suggested default: 60 requests/min per user for general, 10/min for `generate`.

---

## 4. Validation & Business Logic Mapping

Validation derived from DB schema and PRD (enforced both at API and DB levels):

Users
- email: required, valid email format, unique (DB constraint). (db-plan: `email varchar(255) NOT NULL UNIQUE`)
- password: required, min length 8; store hashed using bcrypt/argon2. (db-plan: `password varchar(255) NOT NULL`)
- user_name: required, <=100 chars. (db-plan: `user_name varchar(100) NOT NULL`)
- role: enum: student|teacher|admin (db-plan `CREATE TYPE user_role AS ENUM ('student','teacher','admin')`)
- current_level: 1..8 (db-plan check)
- points, stars: integers >=0

Tasks
- level: 1..8
- prompt: non-empty text
- options: jsonb array length exactly 4 (CHECK(jsonb_array_length(options)=4))
- correct_option_index: 0..3 (db-plan check)
- created_by: nullable uuid (ON DELETE SET NULL)

Progress
- user_id -> exists
- task_id -> exists
- selected_option_index: null or 0..3
- points_awarded >=0
- time_taken_ms optional
- Unique constraint: progress.task_id unique to ensure 1:1 tasks->progress if desired.

Business logic implementation notes
- Answer submission must be transactional: writing progress and updating users.points/stars/current_level must be done in a single DB transaction to avoid race conditions.
- Level-up rule (PRD): every 50 points -> award a star and advance level by 1.
  - Implement as: new_total = users.points + points_awarded; stars_gained = floor(new_total/50); users.stars += stars_gained; users.points = new_total % 50; users.current_level = min(8, users.current_level + stars_gained).
- Points per correct answer: 1 point (PRD US-003). Consider configurable point values in metadata for future.
- AI generation: route requests through server-side integration with openrouter.ai. Protect API key and enforce rate limits / cost controls.

---

## 5. Performance & Indexing considerations
- Use DB indexes (from db-plan): `idx_progress_user_id`, unique `idx_progress_task_id_unique`, `idx_progress_user_created_at`, `idx_tasks_level_active`, `idx_tasks_metadata_gin`, `idx_users_current_level`.
- For listing endpoints (progress/tasks), use OFFSET/LIMIT for MVP; for high scale, migrate to keyset pagination using created_at or id.
- Add caching for public or non-sensitive reads (e.g., curated tasks) using Redis or in-memory cache.

---

## 6. Security & Operational
- Enforce HTTPS.
- Hash passwords with bcrypt/argon2 and never return them.
- Input validation and sanitization on all endpoints (protect against SQL injection; use prepared statements/ORM).
- Rate limiting and abuse protection on expensive endpoints (`tasks/generate`, `auth/login`).
- Audit logs for important actions (user creation, admin changes, level-ups if required).

---

## 7. Example flows mapped to endpoints

1) New user register -> /auth/register -> /auth/login -> /tasks/generate -> /progress/submit -> user levels up.
2) Admin creates curated task -> /admin/tasks -> visible to editors -> can be used as template for generation.

---

## 8. Assumptions
- `tasks` table holds per-attempt instances (per db-plan guidance). Alternatively, a separate `task_templates` table could be created for reusable tasks.
- Auth JWT tokens include `sub` claim = user UUID and `roles` claim.
- Server will set DB session setting for RLS (`app.current_user_id`). If using Supabase, map policies to `auth.uid()`.


---

For more details, adjust endpoints/payloads to match frontend expectations (Astro + React) and the chosen security flows (Supabase vs custom auth).
