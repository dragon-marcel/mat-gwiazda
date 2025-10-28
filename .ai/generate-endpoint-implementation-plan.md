# API Endpoint Implementation Plan: MatGwiazda

This document is a unified, English-language implementation plan for the MatGwiazda REST API. It covers endpoints for Authentication, Users, Tasks (creation and generation), Progress submission, and Admin management. Each endpoint includes request/response details, DTOs, data flow, validation, security considerations, error handling, performance notes, and implementation steps.

Table of contents
- Auth
  - POST /api/v1/auth/register
  - POST /api/v1/auth/login
  - POST /api/v1/auth/refresh
- Users
  - GET /api/v1/users/me
  - PATCH /api/v1/users/me
  - GET /api/v1/users/{id}
  - DELETE /api/v1/users/me
- Tasks
  - POST /api/v1/tasks
  - POST /api/v1/tasks/generate
  - GET /api/v1/tasks/{taskId}
  - GET /api/v1/tasks
- Progress
  - POST /api/v1/progress/submit
  - GET /api/v1/progress/all
- Admin
  - GET /api/v1/admin/users
  - GET /api/v1/admin/learning-levels
  - GET /api/v1/admin/learning-levels/{level}
  - POST /api/v1/admin/learning-levels
  - PUT /api/v1/admin/learning-levels/{level}
  - DELETE /api/v1/admin/learning-levels/{level}
- Cross-cutting: validation, security, rate limiting, RLS, logging, testing
- Implementation roadmap and priorities

Notes: this plan assumes a Spring Boot application using Jakarta Persistence (JPA), Jackson for JSON, and JWT-based authentication (existing DTOs observed: TaskCreateCommand, TaskDto, Auth DTOs, Progress DTOs). Adjust minor details to existing infrastructure if different.

---

AUTH

POST /api/v1/auth/register

1. Overview
- Register a new user. Create a user row, hash the password, and return a basic profile or optionally an auth response.

2. Request details
- Method: POST
- URL: /api/v1/auth/register
- Headers: Content-Type: application/json
- Body DTO: `AuthRegisterCommand` (email, password, userName)
- Validation:
  - email: required, valid email format
  - password: required, min length 8
  - userName: required, non-empty, max length 100

3. Response
- 201 Created: Body: `UserDto` or `AuthResponseDto` if auto-login used
- 400 Bad Request: validation errors
- 409 Conflict: email already exists
- 500 Internal Server Error: unexpected

4. Data flow
- Controller receives `AuthRegisterCommand` (@Valid).
- `AuthService.register(...)` does:
  - Check uniqueness of email via `UserRepository`.
  - Hash password using a PasswordEncoder (bcrypt/argon2).
  - Create and save `User` entity.
  - Optionally generate tokens and return `AuthResponseDto`.

5. Security
- Never return password fields.
- Rate-limit registrations by IP.

6. Errors
- Return structured validation errors (field -> message).
- 409 for duplicate email.

7. Implementation steps
- Implement `AuthController.register` and `AuthService.register`.
- Add PasswordEncoder bean.
- Add unit/integration tests for happy path and failure cases.

POST /api/v1/auth/login

1. Overview
- Authenticate user credentials and return tokens.

2. Request details
- Method: POST
- URL: /api/v1/auth/login
- Body DTO: `AuthLoginCommand` (email, password)

3. Response
- 200 OK: `AuthResponseDto` (accessToken, expiresIn, refreshToken)
- 401 Unauthorized: invalid credentials
- 400 Bad Request: validation

4. Data flow
- Controller -> `AuthService.login`:
  - Find user by email.
  - Verify password using PasswordEncoder.matches.
  - Generate JWT access token (claims: sub=user.id, roles=userRole) and refresh token.
  - Persist refresh token if using stored refresh tokens.

5. Security
- Use uniform error messages to avoid user/email enumeration.
- Apply rate limiting and lockout for repeated failures.

6. Implementation steps
- Implement `JwtService` or token provider and the login flow.
- Add tests for correct and incorrect credentials.

POST /api/v1/auth/refresh

1. Overview
- Exchange a refresh token for a new access token; optionally rotate refresh tokens.

2. Request details
- Method: POST
- URL: /api/v1/auth/refresh
- Body DTO: `AuthRefreshCommand` (refreshToken)

3. Response
- 200 OK: `AuthResponseDto` with new access token (and optionally rotated refresh token)
- 401 Unauthorized: invalid or expired refresh token

4. Data flow
- Controller -> `AuthService.refresh`:
  - Validate refresh token (against DB or token service).
  - Issue new access token; optionally issue new refresh token and revoke old.

5. Security
- Persist refresh tokens securely (hashed) or use opaque tokens stored server-side.
- Provide revoke path (logout) to invalidate refresh tokens.

6. Implementation steps
- Implement refresh logic, storage, and rotation if required.

---

USERS

GET /api/v1/users/me

1. Overview
- Return the profile of the currently authenticated user.

2. Request details
- Method: GET
- URL: /api/v1/users/me
- Auth: Bearer token

3. Response
- 200 OK: `UserDto`
- 401 Unauthorized: missing/invalid token

4. Data flow
- Controller extracts userId from SecurityContext and calls `UserService.getProfile(userId)`.

5. Implementation steps
- Implement `UserController.getMe` and `UserService.findById`.
- Tests for authorized/unauthorized scenarios.

PATCH /api/v1/users/me

1. Overview
- Partial update of current user's profile (e.g. userName, password).

2. Request details
- Method: PATCH
- URL: /api/v1/users/me
- Auth: Bearer token
- Body DTO: `UserUpdateCommand` (userName, password)

3. Response
- 200 OK: updated `UserDto`
- 400 Bad Request: validation
- 401 Unauthorized

4. Data flow
- Controller -> `UserService.updateProfile(userId, cmd)`.
- If password provided, hash it and optionally revoke refresh tokens.

5. Implementation steps
- Implement update logic and tests.

GET /api/v1/users/{id} (admin)

1. Overview
- Admin-only endpoint to read other users' profiles.

2. Request details
- Method: GET
- URL: /api/v1/users/{id}
- Auth: admin role required (`@PreAuthorize("hasRole('ADMIN')")`)

3. Responses
- 200 OK: `UserDto`
- 404 Not Found: user not found
- 403 Forbidden: not admin

4. Implementation steps
- Implement `UserController.getById` with method-level security.
- Tests for admin and non-admin access.

DELETE /api/v1/users/me

1. Overview
- Deactivate or soft-delete current user (set isActive=false).

2. Request details
- Method: DELETE
- URL: /api/v1/users/me
- Auth: Bearer

3. Responses
- 204 No Content: success
- 401 Unauthorized

4. Data flow
- Controller -> `UserService.deactivate(userId)` which sets isActive=false and persists.

5. Implementation steps
- Implement deactivate and tests.

---

TASKS

POST /api/v1/tasks (admin-created curated task)

1. Overview
- Create a curated task/template. Input: `TaskCreateCommand`. This was planned previously; included here for completeness.

2. Request details
- Method: POST
- URL: /api/v1/tasks
- Auth: admin or content-editor role
- Body DTO: `TaskCreateCommand` (level, prompt, options, correctOptionIndex, explanation, createdById, isActive)

3. Validation rules
- level: required, reasonable range (e.g., 1..10) — adjust to business rules
- prompt: required, non-empty
- options: required, exactly 4 non-empty strings
- correctOptionIndex: required, within [0, options.size()-1]
- createdById: optional (use acting user if null)
- isActive: default true if null

4. Response
- 201 Created: `TaskDto` and Location header `/api/v1/tasks/{id}`
- 400 Bad Request: validation issues
- 401/403: unauthorized or forbidden

5. Data flow
- Controller -> `TaskService.createTask(cmd, actingUserId)`; service validates, serializes options to JSON, resolves createdBy, saves Task entity, returns TaskDto.

6. Implementation steps
- Implement `TaskController.createTask` and `TaskService.createTask` with `@Transactional` and proper validation.

POST /api/v1/tasks/generate

1. Overview
- Generate a new task instance for a user: based on templates or via an AI provider. Typically used during user practice.

2. Request details
- Method: POST
- URL: /api/v1/tasks/generate
- Auth: Bearer
- Body DTO: `TaskGenerateCommand` (optional level, source)
- Rate limit: enforce per-user limits (e.g., 10/min)

3. Responses
- 201 Created: `TaskDto`
- 400: invalid params
- 401: unauthorized
- 429: too many requests

4. Data flow
- Controller -> `TaskGenerationService.generateForUser(userId, cmd)`.
- `TaskGenerationService` either selects a suitable curated task or calls an AI provider (external API) to produce a prompt + options.
- Map result to Task entity and save.

5. Security and cost
- Protect AI keys, limit calls, and implement caching/templating where possible.

6. Implementation steps
- Implement `TaskGenerationService` with an AI provider abstraction (interface) so it can be mocked in tests.

GET /api/v1/tasks/{taskId}

1. Overview
- Retrieve a task instance. Access is restricted to the owner (user who has the related progress) or admin.

2. Request details
- Method: GET
- URL: /api/v1/tasks/{taskId}
- Auth: Bearer

3. Responses
- 200 OK: `TaskDto`
- 403 Forbidden: not owner nor admin
- 404 Not Found: missing

4. Data flow
- Controller -> `TaskService.getTaskForUser(taskId, actingUserId)` with ownership/role checks.

5. Implementation steps
- Implement ownership check and tests (owner/admin/forbidden).

GET /api/v1/tasks (list)

1. Overview
- Paginated listing/filtering for admins and content editors. Filtering by level, isActive, createdBy.

2. Request details
- Method: GET
- URL: /api/v1/tasks
- Query params: page, size, sort, level, isActive, createdBy

3. Responses
- 200 OK: `PagedResponse<TaskDto>`

4. Data flow
- Controller -> `TaskService.find(filter, pageable)` -> TaskRepository queries (use Specification or QueryDSL).

5. Implementation steps
- Implement pageable repository and tests.

---

PROGRESS

POST /api/v1/progress/submit

1. Overview
- Submit user's answer to a task. This operation updates a `progress` record, computes awarded points/stars, and updates the `users` record atomically.

2. Request details
- Method: POST
- URL: /api/v1/progress/submit
- Auth: Bearer
- Body DTO: `ProgressSubmitCommand` (taskId, selectedOptionIndex, timeTakenMs)

3. Responses
- 200 OK: `ProgressSubmitResponseDto` (isCorrect, pointsAwarded, userPoints, starsAwarded, leveledUp, newLevel, explanation)
- 400 Bad Request: invalid data
- 403 Forbidden: not allowed to submit
- 409 Conflict: duplicate submission (if policy enforces single submission)
- 500 Internal Server Error

4. Data flow
- Controller -> `ProgressService.submitAnswer(cmd, userId)`.
- `ProgressService.submitAnswer` should be `@Transactional` and do:
  1. Load Task entity by id.
  2. Optional: verify a progress baseline/ownership.
  3. Validate selectedOptionIndex within options range.
  4. Determine correctness against `task.correctOptionIndex`.
  5. Persist progress record with isCorrect, pointsAwarded, timeTaken.
  6. Update user's points/stars and level using a locked or versioned update (SELECT FOR UPDATE or optimistic locking).
  7. Return detailed result including explanation text from task if desired.

5. Concurrency concerns
- Use DB-level locking (SELECT FOR UPDATE) on the user row to avoid lost update when incrementing points/stars.
- Alternatively use optimistic locking with a version column and retry on conflict.

6. Implementation steps
- Implement `ProgressService` with transactionality and concurrency-safe user updates. Add tests simulating concurrent submissions.

GET /api/v1/progress/all

1. Overview
- Not paginated; returns all progress records for the authenticated user (filtered by userId).
2. Request details
- Method: GET
- URL: /api/v1/progress/all

3. Response
- 200 OK: `List<ProgressDto>`

4. Implementation steps
- Implement repository queries and controller endpoints.

ADMIN

GET /api/v1/admin/users

1. Overview
- Admin-only paginated listing of users for management.

2. Request details
- Method: GET
- URL: /api/v1/admin/users
- Auth: admin
- Query params: page, size, filters

3. Response
- 200 OK: `PagedResponse<UserDto>`

4. Implementation steps
- Admin controller + service + pageable queries.

GET /api/v1/admin/learning-levels

1. Overview
- Get list of all levels (1..8).

2. Request details
- Method: GET
- URL: /api/v1/admin/learning-levels
- Auth: JWT Bearer

3. Response
- 200 OK: array of `LearningLevelDto`

4. Security notes
- JWT Bearer security required, role `ADMIN`.

GET /api/v1/admin/learning-levels/{level}

1. Overview
- Get single level by number (short).

2. Request details
- Method: GET
- URL: /api/v1/admin/learning-levels/{level}
- Auth: JWT Bearer
- Path param: `level` (short) — required (1..8).

3. Response
- 200 OK + `LearningLevelDto` or 404 Not Found.

4. Security notes
- JWT Bearer security required, role `ADMIN`.

POST /api/v1/admin/learning-levels

1. Overview
- Create new `learning_levels` entry.

2. Request details
- Method: POST
- URL: /api/v1/admin/learning-levels
- Auth: JWT Bearer
- Body DTO: `CreateLearningLevelCommand` (level, title, description)

3. Responses
- 201 Created + Location `/api/v1/admin/learning-levels/{level}` and body `LearningLevelDto`.
- 409 Conflict — if level already exists.
- 400 Bad Request — validation errors.

4. Security notes
- JWT Bearer security required, role `ADMIN`.

PUT /api/v1/admin/learning-levels/{level}

1. Overview
- Update existing level.

2. Request details
- Method: PUT
- URL: /api/v1/admin/learning-levels/{level}
- Auth: JWT Bearer
- Path param: `level` (short) — target level to update.
- Body DTO: `UpdateLearningLevelCommand` (optional: title, description)

3. Responses
- 200 OK + updated `LearningLevelDto`.
- 404 Not Found — if level not present.
- 400 Bad Request — validation errors.

4. Security notes
- JWT Bearer security required, role `ADMIN`.

DELETE /api/v1/admin/learning-levels/{level}

1. Overview
- Delete existing level.

2. Request details
- Method: DELETE
- URL: /api/v1/admin/learning-levels/{level}
- Auth: JWT Bearer
- Path param: `level` (short) — target level to delete.

3. Responses
- 204 No Content — deleted.
- 404 Not Found — if level not present.

4. Security notes
- JWT Bearer security required, role `ADMIN`.

CROSS-CUTTING CONCERNS

Validation
- Use Jakarta Bean Validation annotations on DTOs (`@NotNull`, `@NotBlank`, `@Size`, `@Email`, `@Min/@Max`).
- Implement custom validators where DTO-level constraints depend on multiple fields (e.g., correctOptionIndex < options.size()).
- Add `@ControllerAdvice` to convert validation exceptions into structured JSON error responses.

Security
- Implement JWT-based authentication with a configurable `JwtService`.
- Use method-level security `@PreAuthorize` for admin endpoints.
- Extract `actingUserId` from SecurityContext (JWT subject) for auditing and authorization.

Row-Level Security (RLS)
- If Postgres RLS is enabled, set session variable `app.current_user_id` per transaction or use the DB's auth mechanism.
- Alternatively, enforce ownership checks at the application layer before returning or modifying rows.

Rate limiting
- Apply rate limiting to expensive endpoints (task generation, auth endpoints) via API gateway or an in-app work-limited solution for MVP.

Logging and error tracking
- Use structured logging (SLF4J/Logback) with traceId and actingUserId.
- Log INFO for successful critical operations, WARN for expected failures, ERROR for unexpected exceptions including stack traces.
- Optionally store important errors to a DB table `api_error_logs` or external error tracking service.

Testing
- Unit tests for all services.
- Integration tests using MockMvc for controllers.
- Concurrency tests for `progress/submit` to validate atomic user updates.

Documentation
- Annotate controllers and DTOs for OpenAPI/Swagger generation and include example requests/responses and security schemes.

---

IMPLEMENTATION ROADMAP (PRIORITIZED)
1. Security foundation: PasswordEncoder bean, `JwtService`, SecurityConfig, and authentication filter.
2. Auth endpoints: register, login, refresh + tests.
3. User endpoints: get profile, update profile, deactivate + tests.
4. Task CRUD: create (admin), get, patch, list + tests.
5. Task generation: AI provider abstraction + rate limiting + tests (mock AI provider).
6. Progress submission: transactional submission handling user points/stars/level updates + concurrency tests.
7. Admin endpoints + role enforcement.
8. Global exception handler, DTO validation, and OpenAPI docs.
9. Rate limiting and logging enhancements; optional `api_error_logs` migration.

Deliverables for initial implementation (MVP)
- Controllers and Services for Auth, Users, Task creation, Task generation stub, Progress submit.
- `GlobalExceptionHandler` for validation and error translation.
- Basic JWT auth + PasswordEncoder configuration.
- Unit and integration tests for critical flows.
- OpenAPI annotations for generated API docs.

If you want, I can now generate implementation skeletons for the highest-priority items (Auth controllers/services, SecurityConfig, GlobalExceptionHandler, and a couple of unit tests). Please confirm and I will create the Java files and run a quick error check/build validation.
