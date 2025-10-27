# Plan implementacji widoku Task Play

## 1. Przegląd
Widok "Task Play" to strona, na której zalogowany użytkownik generuje i rozwiązuje pojedyncze zadania (single-choice). Umożliwia wybór poziomu trudności, wyświetlenie treści zadania i opcji odpowiedzi, mierzy czas rozwiązania, przesyła odpowiedź do backendu i natychmiast pokazuje feedback (czy odpowiedź była poprawna, ile punktów przyznano, czy nastąpił awans poziomu oraz krótkie wyjaśnienie).

Widok korzysta z endpointów backendu: POST /api/v1/tasks/generate oraz POST /api/v1/progress/submit i (opcjonalnie) GET /api/v1/users/me do odświeżenia statystyk użytkownika.

Założenia techniczne (jawne): frontend: React + TypeScript, routing: react-router v6, http client: axios; opcjonalnie TanStack Query (react-query) do fetch/caching.

## Tech stack i UI (Tailwind + shadcn/ui)
- UI będzie implementowany przy użyciu React + TypeScript, stylowanie oparte na Tailwind CSS oraz komponentach z pakietu shadcn/ui (primitives + konfigurowalne komponenty). To zapewni spójną bibliotekę komponentów i szybkie prototypowanie.
- Zalecane kroki instalacji wewnątrz katalogu frontend:

```bash
# utworzenie projektu (Vite + React + TS)
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install

# instalacja Tailwind CSS
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p

# zainstaluj axios i react-router
npm install axios react-router-dom@6

# (opcjonalnie) tanstack/react-query
npm install @tanstack/react-query

# instalacja shadcn/ui (przykładowo)
# uwaga: shadcn/ui wymaga konfiguracji i zależności; użyj oficjalnego przewodnika shadcn/ui
npm install @shadcn/ui
```

- Konfiguracja Tailwind: w `tailwind.config.cjs` dodaj ścieżki do plików `src/**/*.{js,jsx,ts,tsx}` i włącz tryb JIT. Dodaj base CSS do `index.css`:

```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

- shadcn/ui: użyj gotowych komponentów (Buttons, Dialogs, Form Controls) i nadpisuj poprzez Tailwind classes. Dokumentacja shadcn zawiera generator komponentów i konwencję folderu `components/ui`.

- Uwagi:
  - Stosuj klasy utility-first Tailwind dla szybkich iteracji UI.
  - Komponenty będą dostępne jako TypeScript React components z dobrze zdefiniowanymi propsami.
  - Jeśli wymagane, przygotuję szablon projektu (scaffold) z powyższą konfiguracją.

## 2. Routing widoku
- Proponowana ścieżka strony: `/play` (sesja ćwiczeniowa) lub alternatywnie `/tasks/play`.
- Dodatkowa opcja: `/tasks/:taskId/play` — pozwala załadować konkretne zadanie po id (GET /api/v1/tasks/{taskId}).

W praktyce: dodajemy route w aplikacji:
- `<Route path="/play" element={<TaskPlayView />} />`
- (opcjonalnie) `<Route path="/tasks/:taskId/play" element={<TaskPlayView />} />`

## 3. Struktura komponentów (hierarchia)
- TaskPlayView (page)
  - TopBar / StatsBanner
  - ControlPanel
    - LevelSelector
    - GenerateButton
  - TaskArea
    - TaskCard
      - OptionList
        - OptionButton (xN)
      - SubmitBar (SubmitButton + Timer)
      - ExplanationPanel / ResultPanel (po submit)
  - FeedbackModal (po rozstrzygnięciu, opcjonalny)
  - ConfirmLeaveDialog (opcjonalny)

## 4. Szczegóły komponentów

### TaskPlayView
- Opis: Strona zarządzająca całą sesją rozwiązywania zadań. Odpowiada za integrację z API (generowanie zadania, submit), utrzymanie stanu (aktualne zadanie, wybór, wynik) i przekazanie danych do komponentów prezentacyjnych.
- Główne elementy:
  - `TopBar` / `StatsBanner` pokazujący `UserDto` (punkty / gwiazdki / poziom)
  - `ControlPanel` z `LevelSelector` i `GenerateButton`
  - `TaskArea` renderujący `TaskCard` lub komunikaty (loading / no task / error)
- Obsługiwane zdarzenia:
  - generate(level: number)
  - selectOption(index: number)
  - submitAnswer()
  - clearTask()
- Warunki walidacji:
  - `level` musi być integer z zakresu 1..8
  - przed wywołaniem submit: `selectedOptionIndex` !== null
- Typy wymagane:
  - TaskDto (z backendu)
  - ProgressSubmitResponseDto
  - ViewModel: TaskPlayState (opis w sekcji Typy)
- Prospy: (nie przyjmuje propsów, działa jako samodzielna strona; opcjonalnie przyjmuje `initialTaskId?: string` gdy używamy `/tasks/:taskId/play`)

### LevelSelector
- Opis: Dropdown / control do wyboru poziomu (1..8).
- Główne elementy: select/input z wartościami 1..8, validation message
- Obsługiwane zdarzenia: onChange(level)
- Walidacja: enforce 1..8
- Typy: number
- Prospy: value, onChange

### GenerateButton
- Opis: przycisk inicjujący POST /api/v1/tasks/generate z wybranym `level`.
- Elementy: button z spinnerem
- Zdarzenia: onClick -> generate(level)
- Walidacja: disabled gdy `level` nieprawidłowy lub loading
- Prospy: onGenerate

### TaskCard
- Opis: wizualna reprezentacja `TaskDto`: prompt, opcje, meta (level, createdAt)
- Główne elementy HTML / children:
  - h2 / header: prompt
  - ul / div: lista opcji -> `OptionButton` dla każdej
  - footer: meta + SubmitBar
  - ExplanationPanel (po submit) — pokazuje `explanation` z `TaskDto` lub z ProgressSubmitResponseDto
- Obsługiwane zdarzenia:
  - onSelectOption(index)
  - onSubmit()
- Walidacja:
  - `options` musi być tablicą długości >= 2 (po stronie serwera zwykle tak jest); guard i wyświetlenie błędu jeśli brak.
- Typy:
  - input: `task: TaskDto`, `selectedIndex?: number`, `isSubmitting: boolean`, `submissionResult?: ProgressSubmitResponseDto`
- Prospy: task, selectedIndex, onSelect, onSubmit, isSubmitting, submissionResult

### OptionButton
- Opis: pojedynczy przycisk opcji
- Elementy: button z tekstem opcji, ikoną stanu
- Props: { index, text, selected: boolean, status: 'idle'|'correct'|'incorrect', onClick }
- Obsługiwane zdarzenia: onClick(index)
- Walidacja: none (sapce-safe); a11y: role="radio" / ARIA attributes

### SubmitBar
- Opis: zawiera przycisk submit i timer (czas od wygenerowania zadania)
- Elementy: button (Submit), timer (mm:ss / ms), ewentualny przycisk "Poddaj" lub "Generuj nowe"
- Obsługiwane zdarzenia: onSubmit
- Walidacja: disable submit gdy no selection lub isSubmitting

### ExplanationPanel / ResultPanel
- Opis: po submit prezentuje ProgressSubmitResponseDto: czy poprawnie, ile punktów, gwiazdki, informacja o awansie, explanation tekstowe
- Elementy: tytuł (Poprawne / Niepoprawne), body z explanation, statystyki
- Prospy: ProgressSubmitResponseDto
- Interakcje: przycisk "Dalej" / "Wygeneruj kolejne" albo zamknij modal

### FeedbackModal
- Opis: opcjonalny modal wyświetlany po submicie z krótkim podsumowaniem (points, leveled up)
- Elementy: modal z CTA

## 5. Typy (szczegółowy opis typów TS wymaganych do implementacji)
Wszystkie typy TS mapujemy na podstawie backendowych DTO.

- TaskDto (TS)
  - id: string (UUID)
  - level: number (short)
  - prompt: string
  - options: string[]
  - correctOptionIndex?: number | null // admin-only; frontend nie polega na tym polu
  - explanation?: string | null
  - createdById?: string | null
  - isActive: boolean
  - createdAt?: string (ISO timestamp)
  - updatedAt?: string (ISO timestamp)

- TaskGenerateCommand (TS)
  - level: number
  - createdById?: string

- ProgressSubmitCommand (TS)
  - taskId: string
  - selectedOptionIndex: number
  - timeTakenMs?: number

- ProgressSubmitResponseDto (TS)
  - progressId: string
  - isCorrect: boolean
  - pointsAwarded: number
  - userPoints: number
  - starsAwarded: number
  - leveledUp: boolean
  - newLevel: number
  - explanation?: string | null

- UserDto (TS)
  - id: string
  - email: string
  - userName: string
  - role: string
  - currentLevel: number
  - points: number
  - stars: number
  - isActive: boolean
  - createdAt?: string
  - updatedAt?: string
  - lastActiveAt?: string

- ViewModel: TaskPlayState (TS)
  - task?: TaskDto | null
  - selectedOptionIndex?: number | null
  - isLoading: boolean
  - isSubmitting: boolean
  - timeStartMs?: number | null
  - timeTakenMs?: number | null
  - submissionResult?: ProgressSubmitResponseDto | null
  - error?: ApiError | null

- ApiError (TS) — suggested uniwersalny typ błędów
  - status: number
  - message?: string
  - details?: any

## 6. Zarządzanie stanem
Rekomendacja: lokalny stan na poziomie `TaskPlayView` zarządzany za pomocą React + custom hook `useTaskPlay`. Alternatywnie można użyć react-query dla fetch/submit i lokalnego stanu dla selekcji i timera.

- useTaskPlay hook — odpowiedzialności:
  - generate(level: number): wywołuje POST /api/v1/tasks/generate z {level} -> ustawia `task` oraz `timeStartMs = Date.now()`; ustawia isLoading
  - loadTaskById(taskId: string): (opcjonalnie) GET /api/v1/tasks/{taskId}
  - setSelected(index: number)
  - submit(): wykonuje obliczenie timeTakenMs = Date.now() - timeStartMs, buduje ProgressSubmitCommand i POSTuje do /api/v1/progress/submit z nagłówkiem `X-User-Id`; ustawia isSubmitting, przyjmuje ProgressSubmitResponseDto i zapisuje `submissionResult`
  - reset() / newRound(): czyści `task`, `selectedOptionIndex`, `submissionResult`, `timeStartMs`
  - expose: { state, generate, loadTaskById, select, submit, reset }

- useTimer (opcjonalny): hook pomocniczy, który zwraca formatowany czas oraz funkcje pause/resume; implementować pomiar bazujący na Date.now() dla odporności na backgrounding.

- useAuth: globalny hook/kontekst autoryzacji, który przechowuje access tokeny, `userId` oraz mechanizmy refresh. Ważne: `userId` musi być dostępny, bo backend wymaga `X-User-Id` przy submicie progress.
  - Po loginie: wykonać GET /api/v1/users/me, zapisać `user.id` w kontekście auth.

## 7. Integracja API (szczegóły techniczne)
- Konfiguracja axios:
  - baseURL: `/api/v1` (lub pełny URL jeżeli potrzeba)
  - interceptors: wysyłanie Authorization: `Bearer ${accessToken}` jeśli dostępny
  - Dodawać `X-User-Id` header dla requestów wymagających tego nagłówka (submit progress, list progress)
  - Globalny parser błędów: mapować 400/401/403/404 na ApiError

- Wywołania wymagane w widoku:
  1) POST `/api/v1/tasks/generate` (Content-Type: application/json)
     - Request body: TaskGenerateCommand -> { level: number }
     - Response: TaskDto (201)
     - Frontend: ustaw task i uruchom timer
  2) (opcjonalnie) GET `/api/v1/tasks/{taskId}`
     - Response: TaskDto
  3) POST `/api/v1/progress/submit`
     - Headers: `X-User-Id: <userId>` (UUID)
     - Body: ProgressSubmitCommand { taskId: string, selectedOptionIndex: number, timeTakenMs: number }
     - Response: ProgressSubmitResponseDto (201)
     - Frontend: przy odbiorze: zaktualizować lokalny stan (submissionResult), zaktualizować `StatsBanner` albo wykonać GET /users/me
  4) GET `/api/v1/users/me` (opcjonalnie)
     - Response: UserDto — użyjemy do inicjalizacji `StatsBanner` i do zapisania userId

- Typy request/response muszą być odwzorowane w TS (patrz sekcja Typy).

## 8. Interakcje użytkownika i oczekiwane wyniki
1) Start sesji
  - Użytkownik wybiera poziom w `LevelSelector` (1..8) i klika `Generate`.
  - Oczekiwane: widok pokazuje spinner przez czas oczekiwania, po otrzymaniu TaskDto pokazuje `TaskCard` i rozpoczyna pomiar czasu.

2) Wybór opcji
  - Użytkownik klika jedną z `OptionButton`.
  - Oczekiwane: zaznaczenie opcji (stan wizualny), możliwe keyboard navigation.

3) Submit odpowiedzi
  - Użytkownik klika `Submit`.
  - Warunki: selectedOptionIndex !== null, jeśli brak -> inline walidacja i komunikat.
  - Oczekiwane: przycisk submit disabled po kliknięciu do czasu odpowiedzi serwera; wywoływany POST /progress/submit z timeTakenMs (Date.now() - timeStartMs).
  - Po otrzymaniu odpowiedzi: pokazanie `ResultPanel` / `FeedbackModal` z informacjami: poprawne/niepoprawne, pointsAwarded, userPoints, starsAwarded, leveledUp/newLevel, explanation; opcja „Wygeneruj kolejne” lub „Kontynuuj”.

4) Błąd sieci lub walidacja
  - Oczekiwane: komunikat o błędzie z CTA (retry, go back, login), przy błędzie auth -> przekierowanie do logowania.

5) Odświeżenie statystyk
  - Po submit backend zwraca nowe `userPoints` i `newLevel`; frontend powinien zaktualizować `StatsBanner` na podstawie tych pól lub wykonać GET /users/me.

## 9. Warunki i walidacja (komponenty i API)
- LevelSelector: enforce 1..8 (client-side + disabled Generate jeśli poza zakresem)
- TaskCard: verify `task.options` jest tablicą i ma co najmniej 2 elementy; inaczej pokaż komunikat "Nieprawidłowe zadanie" i CTA "Wygeneruj ponownie".
- SubmitBar / Submit: disable jeśli selectedOptionIndex === null. Przy próbie submit bez wyboru pokazać walidację.
- Przy POST /progress/submit: zadbać o wysyłanie poprawnego typu `selectedOptionIndex` (number/short). Zapewnić `timeTakenMs >= 0` i typ integer.
- Obsługa kodów odpowiedzi: 400 -> wyświetlić komunikat z treścią zwróconą przez API; 401/403 -> przekieruj do loginu lub wyświetl modale wymagający re-autoryzacji; 404 -> informacja, że zadanie nie istnieje (CTA: generuj nowe)

## 10. Obsługa błędów (sytuacje i strategie)
- Brak/nieprawidłowy `X-User-Id`:
  - Objawy: 401/403 lub błędy backendu. Strategia: upewnić się, że `userId` jest zapisany w kontekście auth zaraz po logowaniu; jeżeli brak, wykonać GET /users/me lub wymusić login.
- Network error / timeout:
  - Pokaż toast/alert z możliwością ponowienia akcji.
  - Przy submicie: jeśli nie wiadomo, czy operacja wykonała się po stronie serwera, zablokuj wielokrotne wysłanie; opcja „Sprawdź status” lub „Spróbuj ponownie”.
- 400 (walidacja): odczytać message z body i zmapować do UI (jeśli dotyczy pola, np. level); jeśli brak szczegółów, pokazać ogólny komunikat.
- 404 (task not found): pozwolić wygenerować nowe zadanie; jeśli występuje na GET /tasks/{id}, zaproponować powrót do /play.
- Długie czasy odpowiedzi: pokaż spinner i feedback dla użytkownika; timeouti ustawione w axios (np. 10s) i fallbacky.
- Race conditions (np. podwójny submit): po submicie natychmiast ustawić isSubmitting=true i disable buttons, ignorować kolejne kliknięcia.

## 11. Kroki implementacji (krok po kroku)
Poniżej sekwencja implementacji, która ułatwi review i testy.

1) Przygotowanie środowiska i zależności
   - Upewnij się, że projekt frontendowy używa React + TypeScript.
   - Zainstaluj (jeżeli brak): axios, react-router-dom@6, (opcjonalnie) @tanstack/react-query, clsx (opcjonalnie), react-toastify lub inna biblioteka do toastów.

2) Utworzenie struktur plików
   - `src/pages/TaskPlay/TaskPlayView.tsx`
   - `src/pages/TaskPlay/components/TaskCard.tsx`
   - `src/pages/TaskPlay/components/OptionButton.tsx`
   - `src/pages/TaskPlay/components/LevelSelector.tsx`
   - `src/pages/TaskPlay/hooks/useTaskPlay.ts` (or .ts/ .tsx)
   - `src/api/apiClient.ts` (axios instance)
   - `src/types/api.ts` (wszystkie typy TS mapujące DTO)
   - `src/components/StatsBanner.tsx` (jeśli nie istnieje)

3) Zaimplementuj `apiClient`
   - baseURL `/api/v1` lub skonfigurowalny
   - interceptor auth: dodać Authorization Bearer jeżeli accessToken w kontekście
   - helper: `withUserId(headers)` lub middleware do dodawania `X-User-Id` gdy wymagane

4) Zaimplementuj typy w `src/types/api.ts`
   - przepisz TaskDto, ProgressSubmitCommand, ProgressSubmitResponseDto, UserDto

5) Zaimplementuj `useAuth` (jeśli jeszcze nie istnieje)
   - odpowiedzialny za przechowywanie accessToken/refreshToken i `user` (UserDto)
   - po loginie wykonać GET /users/me i zapisać user.id
   - expose: { user, accessToken, login, logout, refresh }

6) Zaimplementuj `useTaskPlay` hook
   - functions: generate(level), loadTaskById(taskId), select(index), submit(), reset()
   - integrate with apiClient
   - measure time via Date.now() (timeStartMs) — set on successful task load
   - handle isLoading / isSubmitting / error states

7) Zbuduj komponent `TaskPlayView`
   - użyj `useTaskPlay` i `useAuth`
   - renderuj `StatsBanner`, `LevelSelector`, `GenerateButton`, `TaskCard`
   - handle route param `taskId` (optional) to pre-load specific task

8) Zbuduj `TaskCard`, `OptionButton`, `SubmitBar`, `ExplanationPanel`
   - option buttons support keyboard nav (arrow keys) i ARIA
   - after submit, visually mark selected answer and correct one (only if backend provides info — if backend nie zwraca correct index, użyć `isCorrect` i explanation; nie ujawniaj correctOptionIndex jeśli jest admin-only)

9) Implementuj feedback logic
   - na `submit()` pokaż `ResultPanel` i zaktualizuj `StatsBanner` na podstawie `ProgressSubmitResponseDto` (userPoints, newLevel)
   - opcjonalnie call GET /users/me dla pełnego odświeżenia profilu

10) Testy i walidacja manualna
   - testy scenariuszy: generate -> select -> submit (poprawna/niepoprawna), error handling (400/401/timeout), race condition (double-click submit)
   - sprawdzić, że `X-User-Id` jest wysyłany i ma wartość user.id (po loginie wykonanego GET /users/me)

11) Drobne usprawnienia i accessibility
   - dodać focus management i a11y labels
   - animacje przy przyznawaniu punktów
   - jednostkowe testy komponentów (Jest + React Testing Library) — happy path + brak wyboru + network error

12) Dokumentacja i PR
   - dodać README fragment opisujący endpointy używane przez widok oraz przykładowe JSONy request/response
   - w opisie PR wypisać jak odtworzyć (krok po kroku), wymagania do autoryzacji i mock danych jeśli brak backendu

---

Uwagi końcowe:
- Kluczowe ryzyko: brak jawnego userId w AuthResponseDto; konieczne jest wywołanie GET /users/me po loginie i zapis user.id dla nagłówka X-User-Id.
- Jeśli projekt frontend używa innego stacku (np. Vue/Angular), struktura komponentów i hooków należy przenieść koncepcyjnie (komponenty -> komponenty, hook -> composables / services).

Plik ten opisuje kompletny plan implementacji widoku "Task Play" i powinien umożliwić frontendowemu programiście implementację krok po kroku.

## Dostęp i ochrona trasy
- Ten widok jest chroniony: dostęp mają wyłącznie użytkownicy zalogowani.
- Implementacja po stronie frontend: użyj `ProtectedRoute` lub mechanizmu route guard, który sprawdza `useAuth().isAuthenticated` przed renderowaniem `TaskPlayView`.
- Zachowanie przy braku autoryzacji:
  - Spróbuj automatycznie odświeżyć token przez `useAuth().refresh()` (interceptor axios). Jeśli odświeżenie się powiedzie — kontynuuj render.
  - Jeżeli odświeżenie nie powiedzie się, przekieruj użytkownika na `/login` lub `/auth` i zapamiętaj aktualną ścieżkę, aby po zalogowaniu wrócić.
- Dodatkowo: przed wywołaniem POST `/api/v1/progress/submit` upewnij się, że `user.id` (z `useAuth().user`) jest dostępny i ustaw `X-User-Id` w nagłówkach żądania.
