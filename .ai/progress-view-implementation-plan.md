# Plan implementacji widoku Przegląd postępów (Progress Overview)

## 1. Przegląd
Widok „Przegląd postępów” pokazuje historię prób użytkownika (Progress entries). Główny endpoint: GET `/api/v1/progress/all` — zwraca `List<ProgressDto>` i wymaga nagłówka `X-User-Id`. Widok umożliwi przegląd, filtrowanie i analizę postępów (np. filtrowanie po zadaniu, dacie, poprawności).

## Tech stack i UI (Tailwind + shadcn/ui)
- Stos: React + TypeScript, Tailwind CSS oraz shadcn/ui dla komponentów UI.
- Instalacja (szybki przepis):

```bash
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
npm install axios react-router-dom@6
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
npm install @shadcn/ui
```

- Uwagi:
  - Dla dużych list rozważ użycie `react-window` oraz komponentów shadcn dla listy wirtualizowanej.

## 2. Routing widoku
- `/progress` — strona z listą postępów dla zalogowanego użytkownika
- (opcjonalnie) `/progress/:progressId` — szczegóły pojedynczego wpisu

## 3. Struktura komponentów
- ProgressView (page)
  - ProgressFilterBar (task select, date range, correctness filter)
  - ProgressList (list of ProgressItem)
    - ProgressItem (summary row)
  - ProgressDetailsModal
  - ExportButtons (CSV/JSON) (opcjonalne)

## 4. Szczegóły komponentów

### ProgressView
- Opis: ładuje wszystkie Postępy użytkownika przy pomocy GET `/api/v1/progress/all` (nagłówek `X-User-Id`) i pozwala użytkownikowi filtrować i przeglądać wpisy.
- Elementy: filter bar, list, pagination (jeśli lista długa — frontend może paginować lokalnie)
- Zdarzenia: changeFilter, openDetails(id), export
- Walidacja: date range correctness (start ≤ end)

### ProgressItem
- Opis: jednostkowy wiersz zawierający: data, taskId (link), attemptNumber, selectedOptionIndex, isCorrect, pointsAwarded, timeTakenMs
- Elementy: tekst, badge correctness, link do zadania
- Propsy: ProgressDto

### ProgressDetailsModal
- Opis: rozszerzona informacja o próbie: explanation (jeśli jest), progressId, timestamp, userPoints at the moment

## 5. Typy
- ProgressDto (TS): id: string, userId: string, taskId: string, attemptNumber: number, selectedOptionIndex?: number, isCorrect: boolean, pointsAwarded: number, timeTakenMs?: number, createdAt?: string, updatedAt?: string
- ProgressFilter: { taskId?: string; dateFrom?: string; dateTo?: string; isCorrect?: boolean }
- ViewModel: { items: ProgressDto[], filtered: ProgressDto[], isLoading, error }

## 6. Zarządzanie stanem
- Hook `useProgress`:
  - fetchAllProgress(userId)
  - filterProgress(filters)
  - exportProgress(format)
- `useAuth` dostarcza `user.id` potrzebny do nagłówka `X-User-Id`.

## 7. Integracja API
- GET `/api/v1/progress/all` — Headers: `X-User-Id: <uuid>` -> returns List<ProgressDto>
- (opcjonalnie) GET `/api/v1/progress/{progressId}` -> ProgressDto

Uwaga: backend zwraca wszystkie wpisy; jeśli lista może być duża, rekomendowane wprowadzenie paginacji po stronie backendu lub implementacja lazy loading/virtualized list na froncie.

## 8. Interakcje użytkownika
1) Użytkownik otwiera `/progress` -> widzi listę swoich prób posortowaną malejąco po dacie
2) Użytkownik filtruje po zadaniu -> lista się aktualizuje (client-side)
3) Użytkownik filtruje po zakresie dat -> lista aktualizuje się
4) Użytkownik klika w pozycję -> otwiera `ProgressDetailsModal` z dodatkowymi informacjami
5) Użytkownik eksportuje listę -> wyzwala pobranie CSV/JSON

## 9. Warunki i walidacja
- userId musi być dostępny (useAuth) przed fetch
- dateFrom ≤ dateTo
- filters sanity checks (taskId must be a UUID-like string)

## 10. Obsługa błędów
- 401/403: jeśli nagłówek X-User-Id nie jest poprawny lub token nieaktualny -> redirect do /login
- Network error: retry + informacja
- Large payload: informacja o limicie i prośba o paginację ze strony backendu

## 11. Kroki implementacji
1. Stwórz `src/pages/Progress/ProgressView.tsx` oraz komponenty w `src/pages/Progress/components`.
2. Implementuj `useProgress` hook używający `apiClient` i `useAuth` (jednokrotne pobranie wszystkich wpisów po mount).
3. Zaimplementuj filtry i export (CSV/JSON).
4. Dodaj `ProgressDetailsModal` i testuj przypadki edge (brak danych, duża lista).
5. Jeśli lista będzie bardzo duża, zaimplementuj paginację po stronie backendu lub wprowadź virtualized list (react-window).

## Dostęp i ochrona trasy
- Widok `Przegląd postępów` (`/progress`) powinien być dostępny tylko dla zalogowanych użytkowników.
- Frontend: użyj `ProtectedRoute` lub guard w routerze, który przed renderowaniem sprawdza `useAuth().isAuthenticated`.
- Przed wywołaniem GET `/api/v1/progress/all` upewnij się, że `useAuth().user.id` jest dostępny i dodaj `X-User-Id` do nagłówków żądania.
- Zachowanie przy braku autoryzacji:
  - Spróbuj odświeżyć access token używając `useAuth().refresh()` (axios interceptor). Jeśli odświeżenie powiedzie się — kontynuuj.
  - Jeśli odświeżenie nie powiedzie się, przekieruj na `/login` i zachowaj aktualną ścieżkę (np. query param `returnTo`) do powrotu po zalogowaniu.
- Dodatkowo: jeśli lista postępów jest duża, rozważ paginację po stronie serwera oraz ochronę tras API na backendzie (JWT + role).  

---

Uwaga: pamiętaj, że endpoint wymaga nagłówka `X-User-Id` — upewnij się, że `useAuth` ma przechowywane `user.id` i że przed pobraniem wywołano GET `/api/v1/users/me` po logowaniu.
