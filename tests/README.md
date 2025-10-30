# Uruchamianie testów — MatGwiazda

Ten dokument zawiera szybkie instrukcje uruchamiania testów jednostkowych (frontend i backend) oraz E2E lokalnie i wskazówki CI.

Wymagania:
- Java 17 (dla backendu)
- Gradle wrapper (w katalogu `backend`)
- Node.js 18+ i npm (dla frontendu)
- (Opcjonalnie) Docker jeżeli chcesz użyć Testcontainers z natywnymi zależnościami

Frontend — szybkie komendy (cmd.exe):

```cmd
cd frontend
npm install
npx playwright install
npm run test:unit       :: uruchamia Vitest w trybie interaktywnym
npm run test:unit:ci    :: uruchamia Vitest w trybie CI (jednorazowo)
npm run test:coverage   :: uruchamia Vitest i generuje coverage
npm run test:e2e        :: uruchamia Playwright (upewnij się, że frontend jest dostępny pod baseURL z configu)
```

Uwaga: Playwright wymaga zainstalowania przeglądarek (`npx playwright install`). W CI możesz dodać krok instalujący przeglądarki.

Backend — szybkie komendy (cmd.exe):

```cmd
cd backend
gradlew.bat test
```

To uruchomi testy JUnit (w tym Testcontainers, jeśli zostaną użyte w testach). Jeżeli w testach używasz Testcontainers i Docker nie jest zainstalowany, rozważ konfigurację Testcontainers z `ryuk.disabled=true` lub uruchamianie kontenerów na CI, które wspiera Docker.

---

# Testy integracyjne (backend) — Testcontainers

Poniżej znajdziesz dokładne wskazówki uruchamiania testów integracyjnych, które w repozytorium korzystają z Testcontainers (statyczny singleton kontenera PostgreSQL skonfigurowany w `backend/src/test/java/pl/matgwiazda/integration/IntegrationTestBase.java`). Instrukcje są podane dla Windows `cmd.exe`.

Wymagania specyficzne dla testów integracyjnych:
- Docker uruchomiony i dostępny lokalnie (Testcontainers uruchomi kontener PostgreSQL).
- Zalecane: wystarczająca przestrzeń dyskowa i dostęp do internetu (pierwsze uruchomienie pobierze obrazy Docker).

Uruchomienie wszystkich testów (unit + integration):

```cmd
cd backend
gradlew.bat test
```

Uruchomienie tylko testów integracyjnych (filtr po pakiecie/konwencji nazewnictwa):

Opcja A — filtr wg pakietu (wszystkie testy w pakiecie `pl.matgwiazda.integration`):
```cmd
cd backend
gradlew.bat test --tests "pl.matgwiazda.integration.*"
```

Opcja B — filtr wg nazwy klasy/testu (np. wszystkie klasy kończące się na `IntegrationTest`):
```cmd
cd backend
gradlew.bat test --tests "*IntegrationTest"
```

Uruchomienie pojedynczej klasy integracyjnej (przykład):

```cmd
cd backend
gradlew.bat test --tests "pl.matgwiazda.integration.TestPostgresContainerTest"
```

(Uwaga: zamień nazwę klasy na rzeczywistą klasę testową, którą chcesz uruchomić.)

Dodatkowe wskazówki i troubleshooting
- Docker nie jest dostępny / nie chcesz używać Testcontainers:
  - Możesz uruchomić lokalny PostgreSQL i ustawić zmienne środowiskowe przed uruchomieniem testów, aby Spring użył tej bazy zamiast kontenera. Przykład (cmd.exe):

```cmd
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/testdb
set SPRING_DATASOURCE_USERNAME=test
set SPRING_DATASOURCE_PASSWORD=test
cd backend
gradlew.bat test
```

  - Uwaga: w takim przypadku upewnij się, że schemat i migracje są zastosowane (np. uruchom skrypty migracyjne lub Liquibase/Flyway przed testami).

- Jeśli testy integracyjne wymagają migracji (projekt ma migracje SQL/seed): uruchom najpierw migracje na środowisku testowym lub pozwól testom wykonać migracje automatycznie (w repo migracje są udostępnione w `db/supabase/migrations` lub w katalogu migracji projektu).

- Logi Testcontainers: aby zobaczyć więcej informacji, możesz uruchomić Gradle z debugiem:

```cmd
cd backend
gradlew.bat test --info
```

albo zwiększyć poziom logowania dla Testcontainers (np. ustawiając `-Dorg.slf4j.simpleLogger.defaultLogLevel=debug`).

- Problemy z portami/zasobami: Testcontainers sam wybiera porty, ale jeżeli masz zasoby blokujące (np. lokalny DB na tych samych portach), zatrzymaj lokalne usługi lub skonfiguruj testy tak, by używały innego URI.

- RLS / Supabase-specific features: jeśli aplikacja używa Supabase-specific features (np. funkcje/konfiguracje RLS), upewnij się, że migracje odzwierciedlają potrzebne reguły — Testcontainers uruchamia czysty Postgres, więc trzeba uruchomić migracje tworzące reguły RLS, jeśli chcesz je testować.

CI / GitHub Actions (krótkie wskazówki):
- W CI upewnij się, że runner ma dostęp do Dockera (np. ubuntu-latest hostuje Docker). Testcontainers pobierze obrazy; możesz przyspieszyć pipeline przy pomocy cache dla obrazów lub pre-pulled images.
- Dodaj krok `gradlew.bat test` w jobie backendowym; możesz użyć filtrów `--tests` dla grup testów.

---

Uruchamianie E2E (lokalnie):
1. Uruchom backend i frontend w trybie deweloperskim (lub zbudowane wersje i preview):
   - Backend: w `backend` uruchom `gradlew.bat bootRun`.
   - Frontend: w `frontend` uruchom `npm run dev`.
2. W osobnym terminalu uruchom `npm run test:e2e` w katalogu `frontend`.

Integracja z CI (sugerowane kroki):
- Krok 1: Zbuduj backend i frontend.
- Krok 2: Uruchom testy jednostkowe backendu (`./gradlew test`) i frontend (Vitest) — raporty JUnit XML.
- Krok 3: Uruchom testy integracyjne z Testcontainers (jeśli potrzebne).
- Krok 4: Zainstaluj Playwright (przeglądarki) i uruchom E2E na ephemeral environment lub wystawionym stagingu.

Dodatkowe uwagi:
- Pliki testów frontendowych znajdują się w `frontend/src` (unit) oraz `frontend/tests/e2e` (E2E Playwright).
- MSW (Mock Service Worker) jest skonfigurowany do użytku w testach jednostkowych (setup w `src/setupTests.ts`).
- Jeśli chcesz, mogę przygotować przykładowy workflow GitHub Actions, który uruchamia testy jednostkowe, integracyjne i E2E.
