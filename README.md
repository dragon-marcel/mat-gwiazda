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
- (Optional) Docker, if you prefer containerized services

1. Clone the repository

```bash
git clone https://github.com/<your-org-or-username>/mat-gwiazda.git
cd mat-gwiazda
```

2. Backend (Spring Boot)

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

3. Frontend (Astro + React)

- Navigate to the frontend directory (if present):

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

Notes:
- The repository uses Supabase for data and auth in the planned architecture. If you run locally with Supabase, provide connection details via env variables or a local configuration file.
- Provide your AI service key as an environment variable (e.g., `OPENROUTER_API_KEY`) and ensure backend code reads it securely.

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
