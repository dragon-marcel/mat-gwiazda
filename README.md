# ⭐ MatGwiazda

## Table of Contents
- Project Description
- User Stories
- Tech Stack
- Getting Started Locally
- Available Scripts
- Project Scope
- Project Status
- License

<a name="project-description"></a>
## Project Description
MatGwiazda is an interactive web application designed to help students in grades 1–8 learn mathematics through AI-generated exercises and gamification.
The app generates level-appropriate math problems (multiple choice with four options and one correct answer), provides short explanations,
awards points for correct answers, and grants "stars" (level-ups) after accumulating points.

![s](https://raw.githubusercontent.com/dragon-marcel/mat-gwiazda/refs/heads/main/matGwiazda-demo.gif)

Level 1: Addition and subtraction within 100, comparing numbers, simple word problems.
- Level 2: Multiplication and division within 100, simple fractions.
- Level 3: Operations up to 1000, multiplication tables, division with remainders, fractions, units of measure (length, mass, time).
- Level 4: Multi-digit numbers, fractions and comparing them.
- Level 5: Decimals, percentages, algebraic expressions.
- Level 6: Operations with fractions, proportions, arithmetic mean.
- Level 7: Powers and radicals, equations and inequalities, percentage calculations.
- Level 8: Linear functions, systems of equations, the Pythagorean Theorem, statistics, and probability.

Key goals:
- Increase engagement through adaptive tasks and gamification.
- Provide immediate feedback and real-time progress tracking.

## 5. User Stories

- US-001: Registration & Login — Users can create an account and log in to secure their data and progress.
- US-002: Automatic Task Generation — The system generates level-appropriate math tasks (4 options, 1 correct) and records an in-progress `progress` entry per user.
- US-003: Points & Stars System — Users earn points for correct answers; accumulating points grants stars and level advancement.
- US-004: Solving Tasks — Users can attempt tasks, select an answer, and receive immediate feedback.
- US-005: Progress Overview — Users can view summary statistics about completed tasks, points and stars.
- US-006: Review Completed Tasks — Users can browse completed tasks with basic result details and explanations.
- US-007: Edit Username & Password — Users can update their display name and password in account settings.
- US-008: Level Management (admin) — Admins can add, edit, and remove levels and their prompts.

## Tech Stack
- Frontend: Astro + React 19, Tailwind CSS
- Backend: Java 17+ with Spring Boot (version referenced in Gradle: 3.5.6)
- Database / Auth: Supabase (PostgreSQL)
- AI: Integration with openrouter.ai (or another AI endpoint) for dynamic task generation
- CI/CD & Hosting: GitHub Actions (recommended), hosting on DigitalOcean (recommended)

### Testing
- Unit tests:
  - Backend: JUnit 5, Mockito and Spring Boot Test (recommended to use Testcontainers for database-dependent tests).
  - Frontend: Vitest + React Testing Library; MSW (Mock Service Worker) for mocking APIs in unit and integration tests.
- E2E tests:
  - Playwright (recommended) — automate user scenarios; integrate with axe-core for accessibility testing.
  - Alternative: Cypress.
- Supplementary tools:
  - REST-assured or Postman/Newman for contract/endpoint tests.
  - WireMock for stubbing external services (e.g., openrouter.ai) in integration tests.

## Getting Started Locally
Prerequisites:
- Java 17 or later
- Node.js (18+) and npm (or pnpm)
- Git
- Docker & Docker Compose (recommended for containerized run)
- (Optional) Supabase CLI if you run Supabase locally

### Running with Docker (recommended for local full-stack testing)
This project includes Dockerfiles for both services and a `docker-compose.yml` at the repository root:

- `backend/Dockerfile` — builds the Spring Boot backend (multi-stage Gradle build) and produces a runnable JAR image.
- `frontend/Dockerfile` — builds the Astro/Vite frontend (Node 18) and serves the static build with nginx on port 5174.
- `docker-compose.yml` — builds and runs `backend` and `frontend`. The compose file is configured to read secrets from a local `.env` file.

Quick start (assumes you run Supabase locally on the host):

1. Create and populate `.env` in the repository root:

Open `.env` and set at least:
```
OPENROUTER_API_KEY=your_openrouter_api_key
JWT_SECRET=your_jwt_secret
SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:54322/postgres
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
```

2. Start local Supabase (if you use Supabase locally):

Open a new terminal and change into the `db` folder (this folder should contain your migration scripts, e.g. `db/migration`).
Then start the local Supabase services and apply migrations. Finally return to the repository root and start the Docker services:

```cmd
supabase start
supabase migration up
```

3. From the repository root, run Docker Compose:
```cmd
docker compose up -d --build
```

This will build `backend` and `frontend` images and run them. After startup:
- Backend: http://localhost:8080
- Frontend: http://localhost:5174

Notes about networking
- The backend container connects to the host Supabase using `host.docker.internal:54322` (this is the default in the provided compose file). This requires Docker Desktop / WSL2 on Windows or equivalent support for `host.docker.internal`.
- Frontend Nginx is configured in `frontend/nginx.conf` to proxy `/api/` requests to `http://backend:8080` when both services run in Docker Compose, so frontend API calls will reach the backend without CORS issues.

## Project Status
Status: Active development (MVP)

The core application flow and backend API scaffolding are present. Frontend sources use Astro + React; backend uses Spring Boot. Continued work is planned for: improving AI prompt quality, user profiles, Supabase integration, test coverage, and CI/CD automation.

<a name="license"></a>
## License
For now this repository is unlicensed.

<!-- Note: if you want to show `matGwiazda-demo.gif`, add that file to the repository root or to `public/` and update the path here (example: `![demo](public/matGwiazda-demo.gif`) -->
