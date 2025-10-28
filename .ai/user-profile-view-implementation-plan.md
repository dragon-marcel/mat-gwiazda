# Plan implementacji widoku Profil użytkownika (User Profile)

## 1. Przegląd
Widok Profil użytkownika pozwala użytkownikowi przeglądać i edytować swoje dane (GET `/api/v1/users/me`, PATCH `/api/v1/users/me`) oraz dezaktywować swoje konto (DELETE `/api/v1/users/me`). Widok powinien umożliwiać aktualizację `userName` i `password` zgodnie z `UserUpdateCommand` oraz prezentować statystyki (poziom, punkty, gwiazdki).

## Tech stack i UI (Tailwind + shadcn/ui)
- Rekomendacja: React + TypeScript, Tailwind CSS i shadcn/ui jako biblioteka komponentów (formularze, przyciski, modale).
- Szybka instalacja:

```bash
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
npm install axios react-router-dom@6
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
npm install @shadcn/ui
```

- Bezpieczeństwo:
  - Preferuj użycie HttpOnly cookie dla refreshToken jeżeli backend wspiera tę opcję.
  - Nie przechowuj haseł w localStorage ani nie przesyłaj ich w URL.

## 2. Routing widoku
- `/profile` — strona profilu zalogowanego użytkownika

## 3. Struktura komponentów
- ProfileView (page)
  - ProfileForm (pola: email (readonly), userName, password (change))
  - StatsPanel (points, stars, level)
  - DangerZone (Delete account / Deactivate)
  - ConfirmDeleteDialog

## 4. Szczegóły komponentów

### ProfileView
- Opis: pobiera `UserDto` przez GET `/api/v1/users/me` (useAuth może udostępniać) i pozwala edytować pola.
- Elementy: formularz, przycisk zapisz, sekcja statystyk
- Zdarzenia: onSave (PATCH `/api/v1/users/me`), onDelete (DELETE `/api/v1/users/me`)

### ProfileForm
- Pola: email (readonly), userName (text), newPassword (password, optional)
- Walidacja:
  - userName: min 2, max 100
  - password: min 6 jeśli podawane
- Typy:
  - UserUpdateCommand (TS): { userName?: string; password?: string }

### DangerZone
- Opis: przycisk usunięcia/dezaktywacji konta z potwierdzeniem
- Akcja: DELETE `/api/v1/users/me` -> 204 -> logout i redirect do /goodbye lub /register
# Plan implementacji widoku Admin — Zarządzanie użytkownikami
## 5. Typy
- UserDto (TS) — (id, email, userName, role, currentLevel, points, stars, isActive, createdAt...)
- UserUpdateCommand (TS) — { userName?: string; password?: string }

## 6. Zarządzanie stanem
- `useAuth` zawiera `user` i funkcję `refreshUser()` lub bezpośrednio GET /users/me
- ProfileView lokalny stan formularza i walidacja

## 7. Integracja API
- GET `/api/v1/users/me` — requires Authorization Bearer or @AuthenticationPrincipal
- PATCH `/api/v1/users/me` — body: UserUpdateCommand -> returns updated UserDto
- DELETE `/api/v1/users/me` — deactivate account -> 204 No Content

## 8. Interakcje użytkownika
1) Użytkownik otwiera `/profile` -> widzi swoje dane i statystyki
2) Użytkownik zmienia `userName` i/lub `password` -> klika Save -> PATCH -> update lokalny i w `useAuth`
3) Użytkownik klika Deactivate -> potwierdza -> DELETE -> aplikacja wylogowuje użytkownika i pokazuje komunikat

## 9. Warunki i walidacja
- userName validation: 2..100
- password validation: 6..255 (jeżeli podano)
- przed wywołaniem DELETE pokaż potwierdzenie

## 10. Obsługa błędów
- 400: field-level validation errors -> przypisać do pól
- 401: redirect do login
- 409/422: business errors -> toast

## 11. Kroki implementacji
1. Dodaj `src/pages/Profile/ProfileView.tsx` i `ProfileForm.tsx`.
2. Upewnij się, że `useAuth` zwraca `user` i `refreshUser()` (po PATCH odśwież user)
3. Implementuj PATCH i DELETE z odpowiednią obsługą odpowiedzi i redirectów.
4. Testy manualne scenariuszy: update name, update password, delete account.

---

Uwaga: endpoint `DELETE /api/v1/users/me` oznacza dezaktywację konta — po wykonaniu frontend powinien wylogować użytkownika i usunąć lokalne dane sesyjne.

## 1. Przegląd
Widok Admin pozwala administratorom na przegląd listy użytkowników systemu, podgląd szczegółów konta, dezaktywację/reaktywację konta oraz potencjalne zarządzanie rolami. Głównym endpointem obecnie udokumentowanym jest GET `/api/v1/admin/users` zwracający listę wszystkich użytkowników (`List<UserDto>`). W planie zakładamy także istnienie endpointów do modyfikacji użytkownika (PATCH/DELETE) — jeśli ich nie ma, frontend zaimplementuje UI, który korzysta z dostępnych endpointów i poprosi backend o brakujące operacje.

Wymagania bezpieczeństwa: dostęp wyłącznie dla użytkowników z rolą ADMIN (frontend sprawdza `user.role` i ukrywa/chroni UI; autoryzacja wymuszana przez backend).

## 2. Routing widoku
- `/admin/users` — strona główna zarządzania użytkownikami (lista)
- `/admin/users/:id` — (opcjonalnie) podstrona lub modal dla szczegółów pojedynczego użytkownika

## 3. Struktura komponentów
- AdminUsersView (page)
  - AdminTopBar (filtry, search)
  - UsersTable / UsersList
    - UserRow (action buttons: view, edit, deactivate)
  - UserDetailsModal
  - ConfirmDialog (deactivate/reactivate)
  - PaginationControls (jeśli backend zwraca stronicowanie)

## 4. Szczegóły komponentów

### AdminUsersView
- Opis: ładuje listę użytkowników, przechowuje filtry i paginację, renderuje tabelę i modale.
- Główne elementy: search input, role filter dropdown, active/inactive switch, tabela użytkowników, pagination
- Obsługiwane zdarzenia: changeFilter, gotoPage, openUserDetails(id), deactivateUser(id), editUser(id)
- Walidacja: filtry poprawnych typów (role ∈ dostępne role), page/size ∈ liczby
- Typy: UserDto[], ApiError
- Prospy: brak (page)

### UsersTable
- Opis: tabela z kolumnami: email, userName, role, level, points, stars, lastActiveAt, actions
- Elementy: table, rows, action buttons
- Obsługiwane zdarzenia: onView(userId), onEdit(userId), onDeactivate(userId)
- Walidacja: poprawne mapowanie pól; layout responsywny
- Typy: UserDto

### UserDetailsModal
- Opis: modal pokazujący pełne dane `UserDto` i przyciski akcji (deactivate, edit role)
- Elementy: details list, action buttons
- Prospy: user: UserDto, onClose, onDeactivate, onRoleChange

### ConfirmDialog
- Opis: uniwersalny komponent potwierdzenia (delete/deactivate)
- Prospy: message, onConfirm, onCancel

## 5. Typy
- UserDto (TS) — zgodne z backendem (id, email, userName, role, currentLevel, points, stars, isActive, createdAt, updatedAt, lastActiveAt)
- AdminUsersListViewModel
  - users: UserDto[]
  - filters: { query?: string; role?: string; isActive?: boolean }
  - pagination: { page: number; size: number; total?: number }
- ApiError: { status: number; message?: string }

## 6. Zarządzanie stanem
- Lokalny stan na poziomie `AdminUsersView` do filterów/paginacji i listy użytkowników.
- Hook `useAdminUsers`:
  - funkcje: fetchUsers(filters, page, size), deactivateUser(userId), reactivateUser(userId), changeRole(userId, role)
  - stan: { data, isLoading, error, pagination }
- Opcjonalnie: globalny cache przez react-query (zalecane dla stron list z paginacją)

## 7. Integracja API
- GET `/api/v1/admin/users` — returns List<UserDto>
  - Query params: (możliwe) page/size/role/isActive/search — jeśli backend nie obsługuje, frontend filtruje lokalnie lub prosi backend o rozbudowę.
- DELETE `/api/v1/users/{id}` — (jeśli dostępne) dezaktywacja użytkownika — lub PATCH `/api/v1/users/{id}` z polem `isActive=false` — jeśli brak, użyć `/api/v1/users/me` dla własnego deaktywowania lub zgłosić potrzebę backendu.
- PATCH `/api/v1/users/{id}` — aktualizacja roli/username (jeżeli backend wspiera)
- GET `/api/v1/users/{id}` — pobór pojedynczego usera (opcjonalnie)

Nagłówki: Authorization Bearer token; backend powinien weryfikować rolę ADMIN.

## 8. Interakcje użytkownika
1) Admin otwiera `/admin/users` → lista użytkowników ładowana automatycznie.
2) Admin wyszukuje użytkownika przez email lub nazwę → pola search + debounce (300ms) → update list.
3) Admin klika „Szczegóły” → otwiera modal z pełnymi danymi i akcjami.
4) Admin klika „Dezaktywuj” → pokazuje ConfirmDialog → po potwierdzeniu wywołanie DELETE/PATCH → aktualizacja listy i notyfikacja.
5) Admin zmienia rolę (jeśli endpoint istnieje) → PATCH -> sukces -> ui update.

## 9. Warunki i walidacja
- Filter inputs: sanitize strings, max length
- Role change: dozwolone role z backendu; frontend powinien walidować przed wysłaniem
- Deactivate: potwierdzenie użytkownika

## 10. Obsługa błędów
- 401/403: usuń dostęp do strony i redirect do /login lub show unauthorized message
- 404: jeśli użytkownik nie istnieje przy GET -> show not found modal
- 409: conflict przy zmianie roli -> show message
- Network: retry/pokaż toast

## 11. Kroki implementacji
1. Utwórz `src/pages/Admin/AdminUsersView.tsx` oraz komponenty w `src/pages/Admin/components`.
2. Zaimplementuj `apiClient` z interceptorem autoryzacji (jeśli nie istnieje).
3. Zaimplementuj hook `useAdminUsers` (z react-query lub fetch wrapperem).
4. Zaimplementuj tabelę, filtry i paginację.
5. Zaimplementuj `UserDetailsModal` i `ConfirmDialog`.
6. Przetestuj scenariusze: fetch, filter, deactivate/reactivate, role change.
7. Przy braku potrzebnych endpointów, zgłoś backendowi wymagania (PATCH user, DELETE user) lub dopasuj UI do istniejących API.

---

Uwaga: plan minimalizuje zmiany po stronie backendu — jeżeli backend nie obsługuje edycji użytkownika przez admina, frontend może jedynie wyświetlać listę i prosić o implementację brakujących endpointów.

## Dostęp i ochrona trasy
- Widok `Profil` (`/profile`) jest dostępny tylko dla zalogowanych użytkowników.
- Frontendowa implementacja: użyj `ProtectedRoute` lub mechanizmu guard w `react-router` sprawdzającego `useAuth().isAuthenticated` przed renderowaniem `ProfileView`.
- Zachowanie przy braku autoryzacji:
  - Spróbuj odświeżyć access token przez `useAuth().refresh()` (axios interceptor). Jeśli odświeżenie powiedzie się, kontynuuj render.
  - Jeśli odświeżenie nie powiedzie się, przekieruj użytkownika do `/login` i zapamiętaj `returnTo` (aktualna ścieżka), aby po udanym logowaniu przywrócić kontekst.
- Dodatkowo: po wywołaniu DELETE `/api/v1/users/me` (dezaktywacja), frontend powinien wykonać `useAuth().logout()` i przekierować do strony powitalnej lub rejestracji.
