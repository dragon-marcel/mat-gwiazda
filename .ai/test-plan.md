# Plan testów — MatGwiazda

> Plik: `ai/test-plan.md`

## 1. Wprowadzenie i cele testowania
Celem testów jest zapewnienie jakości funkcjonalnej i niefunkcjonalnej aplikacji MatGwiazda (Frontend: Astro + React; Backend: Java Spring Boot; Baza: PostgreSQL (integration tests use Testcontainers); integracja z AI/openrouter). Testy mają wykryć regresje, zweryfikować integracje (zwłaszcza integracje AI i warstwę danych), zapewnić wydajność, dostępność oraz bezpieczeństwo.

Cele szczegółowe:
- Weryfikacja krytycznych przepływów użytkownika: rejestracja, logowanie, rozgrywka (quiz), zapisywanie postępów oraz system punktów/poziomów.
- Walidacja integracji pomiędzy frontendem, backendem, bazą danych i openrouter.ai (mocki i sandboxy).
- Zapewnienie stabilności dzięki testom jednostkowym, integracyjnym i E2E oraz kontroli regresji w CI.
- Wykrycie problemów bezpieczeństwa (RLS, uwierzytelnianie, podatności zależności).

## 2. Zakres testów
Obszary objęte testami:
- Backend: REST API, warstwa serwisowa, DTO (immutable records), walidacja (Bean Validation), autoryzacja/uwierzytelnianie, logika punktów/poziomów, migracje DB.
- Frontend: komponenty interaktywne React (PlayView, OptionRow, formularze auth), strony Astro, responsywność i dostępność.
- Integracje: openrouter.ai (generowanie zadań), PostgreSQL (Testcontainers w testach integracyjnych; w środowisku produkcyjnym może być Supabase — patrz uwagi dotyczące RLS).
- E2E: klasyczne ścieżki użytkownika (rejestracja → logowanie → rozgrywka → zapis postępu).
- Niefunkcjonalne: wydajność (Lighthouse/k6), bezpieczeństwo (OWASP ZAP), dostępność (axe-core).

Poza zakresem (do osobnej ścieżki): długotrwałe testy chaos engineering oraz produkcyjne migracje bez środowiska staging.

## 3. Typy testów do przeprowadzenia
- Testy jednostkowe
  - Backend: JUnit 5 + Mockito do testów serwisów, mapperów, walidacji i algorytmów punktów/poziomów.
  - Frontend: Vitest + React Testing Library do testów komponentów i hooków (`src/hooks`).
- Testy integracyjne
  - Backend: Spring Boot Test z Testcontainers (PostgreSQL) — repo zawiera wspólną konfigurację integracyjnych testów z singletonem kontenera (zob. `backend/src/test/java/pl/matgwiazda/integration/IntegrationTestBase.java` i `TestPostgresContainer.java`). Testcontainers jest rejestrowany przed uruchomieniem kontekstu Spring dzięki blokowi statycznemu w `IntegrationTestBase` i przekazuje właściwości JDBC przez `@DynamicPropertySource`.
  - Integracje z openrouter: WireMock lub stuby HTTP, testy z mockami.
  - Frontend: integracyjne testy z MSW (Mock Service Worker) do symulowania API.
- Testy API / kontraktów
  - REST-assured (Java) lub Postman/Newman do testów endpointów i scenariuszy autoryzacji.
- Testy E2E
  - Playwright (zalecany) lub Cypress: scenariusze użytkownika, testy dostępności z axe-core.
- Testy wydajnościowe
  - Lighthouse (frontend), k6/JMeter (backend API).
- Testy bezpieczeństwa
  - OWASP ZAP, skany zależności (Snyk/Dependabot), weryfikacja polityk RLS (patrz uwagi poniżej).
- Testy migracji i danych
  - Uruchamianie skryptów migracyjnych z `db/supabase/migrations` (lub z katalogu migracji projektu) na Testcontainers PostgreSQL i weryfikacja efektów (schemat, dane seedujące, reguły RLS). Używanie Testcontainers umożliwia uruchomienie migracji i sprawdzenie polityk dostępu lokalnie/CI bez zależności od zewnętrznego hosta.
- Testy smoke i regresji
  - Krótkie smoke tests uruchamiane w CI dla szybkiej weryfikacji buildów; pełna regresja przed release.

## 4. Scenariusze testowe dla kluczowych funkcjonalności
(Dla każdego scenariusza: cel, preconditions, kroki, oczekiwany rezultat, edge-case'y)

### A. Rejestracja użytkownika
- Cel: poprawne utworzenie konta, walidacja danych i inicjalizacja profilu.
- Preconditions: czysta baza testowa lub brak użytkownika z danym emailem.
- Kroki:
  1. Otworzyć `RegisterPage`, wprowadzić poprawne dane i wysłać formularz.
  2. Sprawdzić odpowiedź API i rekord w DB (test integracyjny powinien odpytać Testcontainers PostgreSQL).
- Oczekiwany rezultat: użytkownik utworzony, sesja/tokens ustawione, przekierowanie do strony powitalnej.
- Edge cases: istniejący email, słabe hasło, brak połączenia z serwisem uwierzytelniania.

### B. Logowanie i autoryzacja
- Cel: poprawne logowanie i dostęp do chronionych endpointów.
- Preconditions: istniejący użytkownik.
- Kroki:
  1. Zalogować się, odebrać token, wywołać endpoint chroniony (np. zapisz postęp).
- Oczekiwany rezultat: dostęp do zasobów zgodnie z rolą; UI w stanie zalogowanym.
- Edge cases: wygasły token, brak uprawnień (role).

### C. Generowanie zadań przez AI (openrouter)
- Cel: poprawna integracja z AI, obsługa formatu odpowiedzi i fallbacky.
- Preconditions: endpoint backendu skonfigurowany; sandbox/mock openrouter.
- Kroki:
  1. Poprosić o zadania dla poziomu X; backend wywołuje openrouter.
  2. Otrzymane zadania wyświetlane w `PlayView`.
- Oczekiwany rezultat: poprawne parsowanie i wyświetlenie zadań, zapis w DB jeśli wymagany.
- Edge cases: błędy zewnętrznego serwisu, nieprawidłowy format odpowiedzi.

### D. Rozgrywka / mechanika quizu
- Cel: poprawne działanie logiki pytań, sprawdzania odpowiedzi i naliczania punktów.
- Preconditions: zalogowany użytkownik, dostępne zadania.
- Kroki:
  1. Uruchomić sesję gry, odpowiadać na pytania (różne warianty: poprawna/niepoprawna odpowiedź).
  2. Sprawdzić aktualizację punktów i postępu w UI i DB.
- Oczekiwany rezultat: punkty naliczone poprawnie, postęp zapisany.
- Edge cases: brak połączenia przy zapisywaniu wyniku, równoległe sesje.

### E. System poziomów i punktów (logika biznesowa)
- Cel: weryfikacja algorytmu awansów i progów.
- Testy: jednostkowe kombinacje wartości punktów i integracyjne: zapis wyniku -> oczekiwana zmiana poziomu.

### F. Migracje DB i RLS
- Cel: poprawność migracji SQL i działanie reguł RLS.
- Kroki: uruchom migracje na Testcontainers PostgreSQL (używając skryptów migracyjnych projektu), następnie zweryfikuj:
  - czy tabele i indeksy zostały utworzone poprawnie,
  - czy seed-data (jeśli wymagane) jest załadowane,
  - czy reguły RLS działają zgodnie z oczekiwaniami (testy integracyjne powinny symulować różne użytkowniki/role i sprawdzać dostęp do rekordów).
- Edge cases: niekompletne migracje, rollback, różnice w konfiguracji między Supabase a czystym PostgreSQL (jeżeli projekt używa Supabase-specific features trzeba to odzwierciedlić w migracjach testowych).

### G. Dostępność i responsywność
- Cel: poprawność UI na różnych rozdzielczościach i zgodność z podstawowymi zasadami dostępności.
- Testy: Lighthouse, axe-core, manualne testy klawiatury i aria attributes.

### H. Obsługa błędów i komunikaty użytkownika
- Cel: spójne i przyjazne komunikaty, logowanie błędów po stronie backendu.
- Testy: scenariusze 4xx/5xx; UI pokazuje przyjazny komunikat; logi nie wyciekają wrażliwych danych.

## 5. Środowisko testowe
- Lokalne:
  - Frontend: Node.js (wersja z `package.json`), vite dev server, Vitest/Playwright.
  - Backend: JDK 17, Gradle wrapper; profil `test`.
  - Baza: Testcontainers PostgreSQL (integration tests use the singleton container). Do lokalnego debugowania można też użyć lokalnej bazy/Postgres lub emulatora Supabase, ale integracyjne testy w repo korzystają z Testcontainers dla izolacji i deterministycznych środowisk CI.
  - Mocki openrouter: WireMock / lokalny stub.
- CI (GitHub Actions): budowanie backendu, uruchamianie testów jednostkowych i integracyjnych (Testcontainers), uruchamianie testów frontendowych, E2E na środowisku tymczasowym (ephemeral).
- Staging: kopia konfiguracji produkcyjnej (separacja kluczy), realna instancja Supabase dev/stage.
- Test data: seedery i rollback/transakcje, izolacja danych między testami.

## 6. Narzędzia do testowania
- Backend: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL), REST-assured.
- Frontend: Vitest, React Testing Library, MSW (Mock Service Worker), Playwright, Lighthouse.
- Security & deps: OWASP ZAP, Snyk/Dependabot.
- Wydajność: k6 lub JMeter.
- CI: GitHub Actions; raporty: JUnit XML / Allure.
- Zarządzanie błędami: GitHub Issues / JIRA; szablony zgłoszeń.

## 7. Harmonogram testów (proponowany)
- Faza 0 — Przygotowanie (1 tydzień): konfiguracja CI, Testcontainers, MSW, szablony issue.
- Faza 1 — Unit & Static (ciągłe, inicjalnie 2 tygodnie): napisać testy jednostkowe dla krytycznych modułów; ustawić progi pokrycia.
- Faza 2 — Integracyjne (2 tygodnie): testy integracyjne backend z Testcontainers; integracje openrouter (mocki).
- Faza 3 — E2E i dostępność (2 tygodnie): Playwright, accessibility checks.
- Faza 4 — Wydajność i bezpieczeństwo (1–2 tygodnie): baseline Lighthouse, k6/JMeter, OWASP ZAP.
- Faza 5 — Stabilizacja i regresja (ciągłe): smoke tests w CI, pełna regresja przed releasem.

## 8. Kryteria akceptacji testów
- Krytyczne:
  - Brak krytycznych błędów blokujących użycie.
  - Smoke tests CI przechodzą.
- Progi jakości:
  - Pokrycie unit tests: backend >= 75% (krytyczne moduły >= 90%), frontend >= 70% (komponenty interaktywne >= 80%).
  - E2E: krytyczne user flows zielone w staging.
  - Wydajność: Lighthouse >= 90 (Desktop) / >= 80 (Mobile) lub uzgodnione KPI; API P95 < 500ms (przykładowo — do ustalenia).
  - Bezpieczeństwo: brak krytycznych luk OWASP ZAP; brak krytycznych CVE w zależnościach.
- Akceptowalność release:
  - Wszystkie krytyczne i wysokie błędy zamknięte lub istnieje plan naprawy z SLA.

## 9. Role i odpowiedzialności
- QA Lead: nadzór nad strategią testów, priorytetyzacją i metrykami.
- Inżynier QA / Tester: tworzenie i wykonywanie testów, raportowanie bugów, utrzymanie E2E.
- Backend Developer: unit/integration tests, wsparcie Testcontainers i testowej DB.
- Frontend Developer: komponentowe testy, dostępność, utrzymanie testów Vitest/Playwright.
- DevOps/CI Engineer: konfiguracja GitHub Actions, infra testowa, Testcontainers.
- Product Owner / PM: priorytetyzacja i akceptacja kryteriów.
- Security Engineer (opcjonalnie): skany bezpieczeństwa i analiza ZAP/Snyk.

## 10. Procedury raportowania błędów
- Narzędzie: GitHub Issues (lub JIRA).
- Szablon zgłoszenia (wymagane pola):
  - Tytuł: [Priorytet] Krótki opis
  - Środowisko: lokal/CI/staging/production (wersje frontend/backend)
  - Kroki do reprodukcji
  - Oczekiwany vs rzeczywisty rezultat
  - Logi / odpowiedzi API / zrzuty ekranu / nagranie
  - Podejrzany obszar kodu oraz przypisanie
  - Priorytet i wpływ biznesowy (Blocker/Critical/High/Medium/Low)
- SLA i workflow:
  - Blocker/Critical: natychmiastowy triage; plan naprawy w 24h.
  - High: poprawka w najbliższym sprincie.
  - Medium/Low: do backlogu.
- Automatyzacja powiadomień: CI failures i E2E failures -> powiadomienia (Slack/Teams), link do jobu.
- Reprodukcja: każdy bug powinien zawierać minimalny test case (np. Playwright/Vitest snippet) ułatwiający reprodukcję.
- Metryki: liczba otwartych/zamkniętych bugów, średni czas zamknięcia, flaky tests rate, pokrycie testów.

---

## Dodatkowe rekomendacje i uwagi techniczne
- Krytyczne ryzyka:
  - Integracja z openrouter.ai: zmienność formatu odpowiedzi → wymaga mocków, walidacji formatu i fallbacków.
  - Reguły RLS w Supabase: mogą powodować niespodziewane błędy dostępu → testy RLS obowiązkowe; jeśli projekt używa Supabase-specific features należy odzwierciedlić je w migracjach uruchamianych na Testcontainers PostgreSQL.
  - Flakiness E2E: izolacja DB per job, seedery, retryy dla znanych flaków i separacja testów stateful.
- Zalecane artefakty:
  - `tests/README.md` — instrukcja uruchamiania testów lokalnie i w CI.
  - Szablon GitHub Issue `/.github/ISSUE_TEMPLATE/bug_report.md`.
  - Przykładowe seed SQL i skrypty Testcontainers w `backend/src/test/resources`.
  - Przykładowy test Playwright sprawdzający login -> play (jako wzorzec dla przyszłych E2E).

---

_Plik wygenerowano automatycznie na podstawie analizy repozytorium i stosu technologicznego. Dopasuj metryki (progi pokrycia, KPI wydajności) do wymagań biznesowych projektu przed zatwierdzeniem planu._
