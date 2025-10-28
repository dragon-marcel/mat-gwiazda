# Plan implementacji widoku Przegląd zadań (Tasks Overview)

## 1. Przegląd
Widok „Przegląd zadań” umożliwia listowanie zadań w systemie, filtrowanie po poziomie, statusie (aktywny/nieaktywny), autorze oraz paginację i sortowanie. Pozwala na podgląd szczegółów zadania oraz wygenerowanie nowego zadania (POST `/api/v1/tasks/generate`) i ewentualną edycję (PATCH) jeśli backend to obsługuje.

Główne endpointy:
- GET `/api/v1/tasks` — query params: `level`, `isActive`, `createdById`, pagination (page/size/sort)
- GET `/api/v1/tasks/{taskId}` — pobranie pojedynczego zadania
- POST `/api/v1/tasks/generate` — generowanie zadania (admin/creator)

## Tech stack i UI (Tailwind + shadcn/ui)
- Proponowany stos: React + TypeScript, Tailwind CSS oraz shadcn/ui jako biblioteka komponentów.
- Szybkie instrukcje instalacji:

```bash
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
npm install axios react-router-dom@6
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
npm install @shadcn/ui
# opcjonalnie
npm install @tanstack/react-query
```

- Wskazówki:
  - Użyj shadcn/ui do komponentów tabel, drawerów i form generowania.
  - Tailwind przyspieszy tworzenie responsywnej tabeli i layoutu filtrów.

## 2. Routing widoku
- `/tasks` — lista zadań (filters + table)
- `/tasks/:taskId` — szczegóły zadania (modal lub podstrona)

## 3. Struktura komponentów
- TasksView (page)
  - TasksFilterBar (LevelSelector, ActiveSwitch, CreatedBy filter, Search)
  - TasksTable / TasksGrid
    - TaskRow / TaskCard
  - PaginationControls
  - TaskDetailsDrawer / Modal
  - TaskGenerateDialog (opcjonalny)

## 4. Szczegóły komponentów

### TasksView
- Opis: zarządza pobieraniem zadań, filtrami i paginacją; renderuje TasksTable i kontrolki
- Elementy: filter bar, table, pagination, buttons (generate, export)
- Zdarzenia: changeFilter, gotoPage, openTask(taskId), openGenerateDialog
- Walidacja: level musi być number 1..8

### TasksFilterBar
- Opis: zestaw kontrolek filtrujących: level select, isActive checkbox, createdBy select/search, free-text search
- Zdarzenia: onFilterChange(filter)

### TasksTable
- Opis: tabela z kolumnami: id (link), level, prompt (trunc), optionsCount, active, createdBy, createdAt, actions
- Akcje: view, edit, toggleActive

### TaskDetailsDrawer
- Opis: rozszerzony widok TaskDto: prompt full, options list, explanation (jeśli dostępne), metadata
- Propsy: task: TaskDto, onClose

### TaskGenerateDialog
- Opis: formularz do wygenerowania nowego zadania: level (1..8), createdById (opcjonalnie)
- Po submit: POST `/api/v1/tasks/generate` z `TaskGenerateCommand`
- Po sukcesie: zamknij dialog i odśwież listę

## 5. Typy
- TaskDto (TS): id, level, prompt, options: string[], correctOptionIndex?: number | null, explanation?: string | null, createdById?: string | null, isActive: boolean, createdAt?: string, updatedAt?: string
- TaskGenerateCommand (TS): { level: number; createdById?: string }
- TasksListViewModel: { tasks: TaskDto[]; filters; pagination }

## 6. Zarządzanie stanem
- Hook `useTasks`:
  - fetchTasks(filters, page, size, sort)
  - fetchTaskById(taskId)
  - generateTask(cmd)
  - toggleActive(taskId)
  - state: { data, isLoading, error, pagination }
- Rekomendacja: użyć react-query dla cache/paginacji i łatwego refetch po mutacjach

## 7. Integracja API
- GET `/api/v1/tasks` -> returns Page<TaskDto>
  - Do obsługi paginacji z backendu: parsuj odpowiedź (data.content, data.totalElements itd.) jeśli backend zwraca Spring Page structure; jeśli backend zwraca prostą listę, adaptuj frontend.
- GET `/api/v1/tasks/{taskId}` -> TaskDto
- POST `/api/v1/tasks/generate` -> TaskDto (201)
- PATCH `/api/v1/tasks/{taskId}` -> częściowa aktualizacja (jeżeli istnieje)

Nagłówki: Authorization Bearer jeśli wymagane.

## 8. Interakcje użytkownika
1) Użytkownik otwiera `/tasks` -> lista ładowana (paginacja domyślna)
2) Użytkownik filtruje po poziomie -> lista odświeża się (debounce dla search)
3) Użytkownik klika task -> otwiera `TaskDetailsDrawer` z pełną treścią
4) Admin/creator klika „Generate” -> otwiera `TaskGenerateDialog` -> po generacji nowy wiersz pojawia się w liście (refetch)
5) Admin klika toggle active -> wysyła PATCH -> odświeżenie pojedynczego wiersza

## 9. Warunki i walidacja
- Level input: 1..8 (client-side) zgodnie z `@Min`/`@Max` na backendzie
- Generate: disable submit gdy brak poziomu
- Jeśli options array pusta lub null -> pokaz błędu

## 10. Obsługa błędów
- 400: pokaz błędy walidacji
- 401/403: redirect do login lub show unauthorized
- 404 dla GET /tasks/{id}: pokaz "Task nie znaleziono"
- Network: toast i retry

## 11. Kroki implementacji
1. Stwórz `src/pages/Tasks/TasksView.tsx` i komponenty w `src/pages/Tasks/components`.
2. Stwórz lub użyj istniejącego `apiClient` (axios) i typów w `src/types/api.ts`.
3. Zaimplementuj `useTasks` hook (react-query adapter recommended).
4. Zaimplementuj `TasksFilterBar`, `TasksTable`, `TaskDetailsDrawer`, `TaskGenerateDialog`.
5. Przetestuj paginację, filtry i generowanie zadania.
6. Dokumentuj ewentualne braki API (np. PATCH toggle/edytuj) i skoordynuj z backendem.

## Dostęp i ochrona trasy
- Widok `Przegląd zadań` (`/tasks`) powinien być dostępny tylko dla zalogowanych użytkowników.
- Implementacja frontendowa: zaimplementuj `ProtectedRoute` lub guard w routerze, który przed renderowaniem sprawdza `useAuth().isAuthenticated`.
- Zachowanie przy braku autoryzacji:
  - Spróbuj automatycznie odświeżyć access token przy użyciu `useAuth().refresh()` (axios interceptor). Jeśli odświeżenie powiedzie się, kontynuuj.
  - Jeśli odświeżenie się nie powiedzie, przekieruj na `/login` i zachowaj powrót do zapamiętanego filtra/strony (np. query params), aby po logowaniu użytkownik wrócił do miejsca, w którym był.
- Dodatkowo: jeśli frontend wywołuje admin-only operacje (np. edycja/usuwanie zadań), sprawdź po stronie klienta rolę `useAuth().user.role === 'ADMIN'` i ukryj lub zablokuj przyciski akcji; zaakceptuj, że ostateczna weryfikacja odbywa się po stronie backendu (403).

---

Uwaga: format odpowiedzi z backendu `GET /api/v1/tasks` to Spring `Page<TaskDto>` (zgodnie z `api-plan.md`) — upewnij się, że frontend parsuje strukturę page (content, totalElements, number, size). Jeśli backend został skonfigurowany inaczej, dostosuj mapping.
