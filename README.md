# ⭐ MatGwiazda

## Table of Contents
- Project Description
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

<a name="tech-stack"></a>
## Tech Stack
- Frontend: Astro + React 19, Tailwind CSS
- Backend: Java 17+ with Spring Boot (version referenced in Gradle: 3.5.6)
- Database / Auth: Supabase (PostgreSQL)
- AI: Integration with openrouter.ai (or another AI endpoint) for dynamic task generation
- CI/CD & Hosting: GitHub Actions (recommended), hosting on DigitalOcean (recommended)

### Testowanie
- Testy jednostkowe:
  - Backend: JUnit 5, Mockito oraz Spring Boot Test (zalecane użycie Testcontainers dla testów zależnych od bazy danych).
  - Frontend: Vitest + React Testing Library; MSW (Mock Service Worker) do mockowania API w testach jednostkowych i integracyjnych.
- Testy E2E:
  - Playwright (zalecany) — automatyzacja scenariuszy użytkownika, integracja z axe-core dla testów dostępności.
  - Alternatywa: Cypress.
- Narzędzia uzupełniające:
  - REST-assured lub Postman/Newman do testów kontraktów/endpointów backendu.
  - WireMock do stubowania zewnętrznych serwisów (np. openrouter.ai) w testach integracyjnych.

<a name="getting-started-locally"></a>
## Getting Started Locally
Prerequisites:
- Java 17 or later
- Node.js (18+) and npm (or pnpm)
- Git
- Docker & Docker Compose (recommended for containerized run)
- (Optional) Supabase CLI if you run Supabase locally

1. Clone the repository

```bash
git clone https://github.com/<your-org-or-username>/mat-gwiazda.git
cd mat-gwiazda
```

### Backend (Spring Boot)

- Configure environment variables (example):
  - `SPRING_PROFILES_ACTIVE` (optional)
  - `SPRING_DATASOURCE_URL` (if using your own Postgres)
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
  - `SUPABASE_URL` and `SUPABASE_KEY` (if using Supabase)
  - `OPENROUTER_API_KEY` (or other AI service key)

- Run using the Gradle wrapper (Linux/macOS):

```bash
./gradlew bootRun
```

- Run on Windows (cmd.exe / PowerShell):

```cmd
gradlew.bat bootRun
```

- Build jar:

```bash
./gradlew build
```

### Frontend (Astro + React)

- Navigate to the frontend directory:

```bash
cd frontend
```

- Install dependencies:

```bash
npm install
```

- Start development server:

```bash
npm run dev
```

- Build for production:

```bash
npm run build
```

### Running with Docker (recommended for local full-stack testing)
This project includes Dockerfiles for both services and a `docker-compose.yml` at the repository root:

- `backend/Dockerfile` — builds the Spring Boot backend (multi-stage Gradle build) and produces a runnable JAR image.
- `frontend/Dockerfile` — builds the Astro/Vite frontend (Node 18) and serves the static build with nginx on port 5174.
- `docker-compose.yml` — builds and runs `backend` and `frontend`. The compose file is configured to read secrets from a local `.env` file.

Quick start (assumes you run Supabase locally on the host):

1. Create and populate `.env` in the repository root. You can copy the example file:

```cmd
copy .env.example .env
```

Open `.env` and set at least:
```
OPENROUTER_API_KEY=your_openrouter_api_key
JWT_SECRET=your_jwt_secret
SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:54322/postgres
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
```

2. Start local Supabase (if you use Supabase locally):

```cmd
supabase start
```

Make sure the Postgres instance is listening on port `54322`. You can test with:

```bash
pg_isready -h localhost -p 54322 -U postgres
# or using dockerized psql:
# docker run --rm postgres:15-alpine sh -c "pg_isready -h host.docker.internal -p 54322 -U postgres"
```

3. Apply database migration scripts

This project expects SQL migration scripts to be applied to your Supabase/Postgres instance. By convention they are located in `./db` (project root) — if you don't have a `db/` folder at the repo root, check `./backend/db`.

You can apply migrations in multiple ways:

- Using `psql` (if you have the client installed):

```bash
psql "host=localhost port=54322 user=postgres dbname=postgres" -f db/init.sql
```

- Using a dockerized psql client (cross-platform):

```cmd
# from repo root (Windows cmd example)
docker run --rm -v "%CD%/db:/db" postgres:15-alpine sh -c "psql -h host.docker.internal -p 54322 -U postgres -d postgres -f /db/init.sql"
```

- If you use Supabase migrations or the Supabase CLI migration workflow, use the Supabase commands (e.g. `supabase db push` / `supabase migrations deploy`) according to your project setup.

4. Build and start the services with Docker Compose:

```cmd
docker compose up -d --build
```

This will build `backend` and `frontend` images and run them. After startup:
- Backend: http://localhost:8080
- Frontend: http://localhost:5174

Notes about networking
- The backend container connects to the host Supabase using `host.docker.internal:54322` (this is the default in the provided compose file). This requires Docker Desktop / WSL2 on Windows or equivalent support for `host.docker.internal`.
- Frontend Nginx is configured in `frontend/nginx.conf` to proxy `/api/` requests to `http://backend:8080` when both services run in Docker Compose, so frontend API calls will reach the backend without CORS issues.

### Running single containers (optional)
You can run a single service image and pass secrets via an env file:

```cmd
# build backend image
docker build -t mat-gwiazda-backend:local ./backend
# run with .env
docker run --rm --env-file .env -p 8080:8080 mat-gwiazda-backend:local
```

### .env and secrets
- A `.env.example` is provided. Copy it to `.env` and fill in the real values before starting with Docker Compose.
- For local development `.env` is acceptable but **do not commit** it to the repository. For production use a secrets manager (Docker Secrets, Vault, or cloud provider secrets).

<a name="available-scripts"></a>
## Available Scripts

### Frontend (see `frontend/package.json`)
- `npm run dev` — start Astro development server
- `npm run build` — build frontend for production
- `npm run preview` — preview production build

### Backend (Gradle wrapper)
- `./gradlew bootRun` or `gradlew.bat bootRun` — run Spring Boot application
- `./gradlew build` — build project artifacts
- `./gradlew test` — run tests

<a name="project-scope"></a>
## Project Scope
Core features (MVP):
- AI-driven generation of math problems adjusted to user level (1–8)
- Multiple choice questions: 4 options, 1 correct, plus brief explanation
- User registration and login (email + password)
- Points and stars system: 1 point per correct answer; 50 points → 1 star and level up
- Real-time progress display: points, stars, completed tasks, time spent
- Recording of completed tasks, scores, and session times

Out of scope for MVP (to consider later):
- Advanced analytics and reporting
- Integration with school platforms / gradebooks
- Teacher-created custom tasks and classroom management features
- Native mobile apps (web-only MVP)

<a name="project-status"></a>
## Project Status
Status: Active development (MVP)

The core application flow and backend API scaffolding are present. Frontend sources use Astro + React; backend uses Spring Boot. Continued work is planned for: improving AI prompt quality, user profiles, Supabase integration, test coverage, and CI/CD automation.

<a name="license"></a>
## License
For now this repository is unlicensed.
