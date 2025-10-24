# Architektura UI dla MatGwiazda

## 1. Przegląd struktury UI

Interfejs użytkownika MatGwiazda jest zaprojektowana jako responsywna aplikacja webowa oparta na React + Tailwind + shadcn/ui a pozniej jako mobilna. Kluczowe założenia:
- Globalny layout z top barem (shadcn/ui) i warunkowym admin sidebarem.
- Zarządzanie stanem sesji: React Context (access token w pamięci) + TanStack Query dla server-state (fetch/mutation/cache/invalidacja).
- Bezpieczny refresh: refresh token przechowywany w httpOnly Secure SameSite cookie; interceptor HTTP automatycznie wywołuje POST /api/v1/auth/refresh przy wygasłym access tokenie.
- UX: natychmiastowa informacja zwrotna (toasty, modal), animacje zdobywania punktów/gwiazdki/awansu (honorujące prefers-reduced-motion), cooldown na generowanie zadań.
- Dostępność: WCAG AA (ARIA, keyboard nav, focus management, kontrast, skalowalna typografia).
- Odporność offline: lokalny cache ostatnich zadań/postępu, optymistyczne aktualizacje z rollbackiem, retry/backoff.

## 2. Lista widoków

Dla każdego widoku podano: cel, ścieżka, kluczowe informacje, komponenty oraz uwagi dotyczące UX, dostępności i bezpieczeństwa.

1) Auth (Login / Register)
- Ścieżka: /auth
- Główny cel: umożliwić rejestrację i logowanie użytkownika, walidacja pól oraz ustawienie sesji (access token w pamięci; refresh cookie ustawiane przez backend).
- Kluczowe informacje: email, password, userName (rejestracja), walidacja inline (mapowanie schema błędów {status,code,message,details}).
- Kluczowe komponenty: AuthForm (LoginForm, RegisterForm), Field, FormError, SubmitButton, OAuthPlaceholder (future).
- UX/A11y/Security:
  - Pola z labelami i aria-describedby do błędów.
  - Przyciski dostępne klawiaturowo; fokus widoczny.
  - Nie zapisywać hasła ani access tokena w localStorage; access token w pamięci tylko w Context.
  - Obsługa statusów 400/409/429 z przyjaznym komunikatem.

2) Dashboard / Profile
- Ścieżka: / lub /users/me
- Główny cel: prezentacja aktualnego poziomu, punktów, gwiazdek, paska postępu do następnej gwiazdki; szybkie akcje (Wygeneruj zadanie, Kontynuuj ostatnie).
- Kluczowe informacje: currentLevel, points (0..49), stars, progress bar, ostatnie próby (skrót), CTA "Wygeneruj zadanie".
- Kluczowe komponenty: LevelProgressBar, StarBadge, PointsTicker, RecentAttemptsList, GenerateButton (z cooldown), ProfileSummaryCard.
- UX/A11y/Security:
  - Live region (aria-live) dla dynamicznych zmian punktów/gwiazd.
  - Animacje honorujące prefers-reduced-motion.
  - Po zmianach (submit) TanStack Query automatycznie refetchuje /users/me w tle.

3) Generate Task
- Ścieżka: /tasks/generate (alternatywnie akcja z topbaru lub dashboardu)
- Główny cel: uruchomić POST /api/v1/tasks/generate, obsłużyć rate-limit/cooldown i przeprowadzić usera do rozwiązywania zadania.
- Kluczowe informacje: poziom (możliwość ustawienia), wskazówka o cooldownie, podgląd kosztów/warning jeśli serwer zwraca ograniczenie.
- Kluczowe komponenty: GeneratePanel, CooldownTimer, ServerRateHint, SkeletonTaskPreview.
- UX/A11y/Security:
  - Przycisk wyłączony podczas cooldownu z odliczaniem oraz informacją tekstową i aria-live.
  - Obsługa 429: pokazanie dedykowanego bannera z możliwością retry i ewentualnym serwerowym resetTimestamp.
  - Po wygenerowaniu: przekierowanie do /tasks/:id lub otwarcie modalnego TaskView.

4) Task View / Solve Task
- Ścieżka: /tasks/:id
- Główny cel: wyświetlenie treści zadania, umożliwienie kilkukrotnych prób aż do poprawnej odpowiedzi, wysłanie odpowiedzi do POST /api/v1/progress/submit i reakcja na flagi (pointsAwarded, leveledUp, newLevel, explanation).
- Kluczowe informacje: prompt, 4 options array, timer (opcjonalnie), informacje o próbach, progressId (jeśli zwrócony przy generate).
- Kluczowe komponenty: TaskCard, OptionButtonGroup (keyboard navigable, role=listbox/option), SubmitButton, AttemptCounter, ExplanationPanel, LevelUpModal/AnimatedToast.
- UX/A11y/Security:
  - Opcje jako semanticzne buttony z aria-pressed lub radio group; obsługa klawiatury (arrow keys, Enter), duże targety dotykowe.
  - Po submicie: jeśli isCorrect=false -> inline feedback i pozwolenie na retry; jeśli isCorrect=true -> otwarcie LevelUpModal/animowanego toasta używając danych z odpowiedzi (pointsAwarded, leveledUp, newLevel, explanation).
  - TanStack Query mutation z optimistic UI minimalnym (np. local attempt state) i finalnym uaktualnieniem po odpowiedzi servera; po sukcesie invalidacja ['me'] i ['progress'].
  - Wszystkie animacje respektują prefers-reduced-motion.

5) Progress / Historia prób
- Ścieżka: /progress
- Główny cel: przegląd paginowanej listy prób użytkownika z filtrami (isCorrect, from/to), dostęp do pojedynczych prób i link do zadania.
- Kluczowe informacje: lista attemptów (isCorrect, pointsAwarded, timeTakenMs, createdAt), paginacja, filtry, search.
- Kluczowe komponenty: ProgressTable/List, FilterBar, PaginationControls, AttemptRow, EmptyState, RetrySkeletons.
- UX/A11y/Security:
  - Paginacja przyjazna klawiaturze i aria-labels.
  - Mapowanie błędów API do globalnego bannera lub inline (np. filter validation).
  - Dane chronione: tylko właściciel lub admin.

6) Admin Area
- Ścieżka: /admin/* (np. /admin/users, /admin/tasks)
- Główny cel: narzędzia do zarządzania użytkownikami i zadaniami (MVP: listowanie, filtrowanie, podstawowa edycja/aktywacja zadań).
- Kluczowe informacje: lista użytkowników, lista zadań, filtry po level/isActive, możliwość dezaktywacji/usunięcia.
- Kluczowe komponenty: AdminSidebar, AdminTable, UserCard, TaskEditorModal (prostota w MVP).
- UX/A11y/Security:
  - Ochrona trasy (role check) i wyraźne komunikaty przy błędach uprawnień (403).
  - Potwierdzenia dla operacji destrukcyjnych.

7) Globalne komponenty / Overlays
- TopBar (Logo, Home, Generate CTA, Points/Stars quick view, UserMenu)
- AdminSidebar (warunkowy)
- NotificationCenter / Toasts (aria-live polite/assertive), LevelUpModal (focus trap)
- GlobalErrorBanner (dla 500, 429, krytycznych błędów)
- OfflineIndicator
- LoadingSkeletons i Placeholders

## 3. Mapa podróży użytkownika

Główny scenariusz (nowy użytkownik -> rozwiązanie zadania -> level-up):
1. Rejestracja: /auth (POST /api/v1/auth/register). Po udanej rejestracji: automatyczne login / przekierowanie do /users/me.
2. Dashboard: użytkownik widzi currentLevel, points, stars, GenerateButton.
3. Generowanie: klik "Wygeneruj zadanie" -> klient wywołuje POST /api/v1/tasks/generate (GenerateButton disabled przez cooldown). Po odpowiedzi: redirect /tasks/:id.
4. Rozwiązywanie: na /tasks/:id user wybiera opcję -> POST /api/v1/progress/submit.
   - Jeśli isCorrect=false: UI pokazuje inline feedback, explanation (jeśli dostępne), pozwala na kolejną próbę.
   - Jeśli isCorrect=true: UI pokazuje LevelUpModal/AnimatedToast bazując na pointsAwarded i leveledUp; TanStack Query invaliduje ['me'] i ['progress'] w tle; pasek postępu w topbar/dash aktualizuje się.
   Po udzieleniu poprawnej odowpiedzi widoczny przycisk do wygnerowania kolejnego pytania.
6. Historia: user może przejść do /progress, przeglądać i filtrować próby.

Dodatkowe ścieżki:
- Admin: /admin/* dostęp tylko dla roli admin.
- Sesja wygasła: interceptor wywołuje /api/v1/auth/refresh; jeśli odświeżanie nie powiodło się -> redirect do /auth z zachowanym path.
- Offline: UI wyświetla offline banner; ostatnie zadania/progress z cache dostępne; mutacje kolejkowane z retry.

## 4. Układ i struktura nawigacji

Hierarchia i elementy nawigacyjne:
- TopBar (globalny): logo (link do '/'), Generate CTA, Points/Stars quick preview, UserMenu (Profile, Logout), Mobile menu.
- Primary navigation (visible depending on viewport): Home/Dashboard, Generate, Progress, Admin (jeśli role=admin).
- AdminSidebar: kiedy trasa zaczyna się od '/admin', pokazuje pionowe menu: Dashboard, Users, Tasks, Templates.
- Route guards:
  - Public: /auth
  - Protected: wszystkie pozostałe (401 -> try refresh -> redirect to /auth)
- Breadcrumbs: Task view i Admin sections.
- Deep linking: /tasks/:id and /progress?page=.. preserved for shareable links.

UX flow rules:
- Każda akcja mutująca server-state (generate, submit) ma jasno widoczny loading state i jest zabezpieczona przed duplikacją (single-flight, disable buttons).
- Failures: inline field errors + global banner for server-level errors.

## 5. Kluczowe komponenty

Lista komponentów wielokrotnego użytku, z krótkim opisem:

- TopBar
  - Zawiera: logo, Generate CTA, Points/Stars preview, UserMenu.
  - Responsibilities: natychmiastowa informacja o stanie konta, wejście do najważniejszych akcji.

- AdminSidebar
  - Conditional vertical nav for admin routes.

- GenerateButton + CooldownTimer
  - Odpowiada za: execute POST /tasks/generate, show cooldown UX, handle 429.
  - Behaviour: client-side cooldown + server timestamp if available.

- TaskCard
  - Zawiera prompt, options, timer, attempt counter.
  - Accessibility: role semantics, keyboard navigation, focus management.

- OptionButtonGroup
  - Renderuje 4 opcje; obsługuje keyboard nav i ARIA.

- LevelProgressBar
  - Wizualizuje bieżące punkty do następnej gwiazdki (0..49) z aria-valuenow.

- LevelUpModal / AnimatedToast
  - Pokazywany kiedy leveledUp lub pointsAwarded; focus trap, accessible announcement, supports reduced motion.

- NotificationCenter / Toast
  - Centralne zarządzanie komunikatami systemowymi (info/error/success).

- ProgressList / ProgressRow
  - Paginated list of attempts, with filters and row-level actions.

- ApiErrorMapper + FormError
  - Mapuje {status,code,message,details} na field-level errors i global banner messages.

- OfflineQueue + LocalCache
  - Mechanizm kolejkowania i retry dla mutacji w przypadku offline/niestabilnej sieci; wykorzystuje IndexedDB (recommended) lub localStorage (fallback) dla retention of last tasks/progress only.

## Dopasowanie historyjek użytkownika (PRD) do architektury UI

- US-001 (Rejestracja i logowanie): zaadresowane przez `Auth` view, walidację, oraz `TopBar`/`Profile` po zalogowaniu.
- US-002 (Automatyczne generowanie zadań): `Generate Task` + `Task View` z integracją POST /tasks/generate.
- US-003 (System punktów i gwiazdek): `LevelProgressBar`, `LevelUpModal`, `ProfileSummaryCard`, powiązanie z /users/me.
- US-004 (Rozwiązywanie zadań): `Task View`, `OptionButtonGroup`, `Progress/submit` flow i explanation panel.
- US-005 (Przegląd postępów): `Progress` view z paginacją i filtrami.

## Mapowanie wymagań na elementy UI (krótkie)

- Bezpieczeństwo sesji: React Context + in-memory token + refresh cookie (backend) + interceptor -> wpływa na Auth flow i Protected Routes.
- Rate-limit dla generate: GenerateButton + CooldownTimer + ServerRateHint.
- Instant feedback i animacje: LevelUpModal + Toast + PointsTicker (driven by flags from /progress/submit).
- Accessibility: semantic HTML, aria-live, focus trap, keyboard nav for options and paginacja.
- Error handling: ApiErrorMapper -> FormError + GlobalErrorBanner.

## Potencjalne punkty bólu użytkownika i przyjęte rozwiązania

1. Długie opóźnienie generowania zadań:
   - Pokaż skeleton + komunikat o szacowanym czasie; fallback curated tasks; wyraźne CTA do retry.
2. Utrata sesji podczas rozwiązywania zadania:
   - Single-flight refresh token, kolejkuj requesty, resume po odświeżeniu lub przekierowanie do /auth z zachowanym stanem (taskId + selected option cached temporarily).
3. Niejasność o zdobyciu gwiazdki/awansie:
   - Wyraźny LevelUpModal z before/after i wskazaniem co się zmieniło.
4. Brak sieci/niestabilność:
   - Offline indicator, cache ostatnich zadań, kolejka mutacji z retry/backoff, rollback on permanent failure.
5. Błędy rate-limit:
   - Czytelny licznik i informacja kiedy można ponowić; accept server reset timestamp if provided.

---

Uwagi końcowe i otwarte pytania do backendu:
- Czy `POST /tasks/generate` zwraca `progressId`? nie, zwraca task id i user id wheaderze jest przekazywany  w wywołaniu  POST /api/v1/progress/submit .
- Czy serwer zwraca `rate-limit reset timestamp` by precyzyjnie ustawić cooldown klienta?tak
- Polityka cookie dla refresh endpoint: path, SameSite, expiry i rotacja refresh tokenów.
- Pełna lista wartości `code` w schemacie błędów API dla spójnego mapowania w UI.

Plik z architekturą UI został zapisany w `.ai/ui-plan.md`.

