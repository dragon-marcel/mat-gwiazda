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
- Jeśli chcesz, mogę przygotować przykładowy workflow GitHub Actions, który uruchamia testy jednostkowe i E2E.

