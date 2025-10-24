# Plan implementacji widoku Autoryzacja (Logowanie & Rejestracja)

## 1. Przegląd
Widok autoryzacji obejmuje formularze rejestracji i logowania oraz mechanizm odświeżania tokenów. Celem jest umożliwienie użytkownikowi utworzenia konta (POST /api/v1/auth/register), zalogowania się (POST /api/v1/auth/login) oraz odświeżenia tokenów (POST /api/v1/auth/refresh). Frontend musi zapisać access + refresh token, bezpiecznie je przechowywać, oraz pobrać i przechować `UserDto` (GET /api/v1/users/me) aby uzyskać `userId` wymagany przez inne endpointy.

Założony stack: React + TypeScript, axios, react-router v6, kontekst/auth hook (`useAuth`).

## Tech stack i UI (Tailwind + shadcn/ui)
- UI i aplikacja będą oparte na React + TypeScript z Tailwind CSS i komponentami shadcn/ui.
- Zalecane zależności i instalacja w katalogu frontend:

```bash
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
npm install axios react-router-dom@6
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
# opcjonalnie
npm install @tanstack/react-query
npm install @shadcn/ui
```

- Konfiguracja Tailwind: dodaj `./src/**/*.{js,jsx,ts,tsx}` do `content` w `tailwind.config.cjs` i dołącz bazowe style w `src/index.css`:

```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

- shadcn/ui: korzystaj z komponentów formularzy, przycisków i dialogów, dostosowując style Tailwind. Można wygenerować komponenty wg oficjalnego przewodnika shadcn.

- Uwaga bezpieczeństwa: rozważ użycie HttpOnly cookies dla refreshToken jeśli backend to wspiera; minimalizuj przechowywanie tokenów w localStorage.

## 2. Routing widoku
- `/login` — strona logowania
- `/register` — strona rejestracji
- (opcjonalnie) `/auth/refresh` — endpoint wywoływany automatycznie przez `useAuth` przy wygasłym access tokenie

## 3. Struktura komponentów
- AuthPage (wrapper z routami lokalnymi)
  - LoginForm
  - RegisterForm
  - OAuthButtons (opcjonalne)
  - ErrorBanner / FormField

## 4. Szczegóły komponentów

### LoginForm
- Opis: formularz przyjmujący email i password, wywołuje POST `/api/v1/auth/login`.
- Główne elementy: pola email (type=email), password (type=password), przycisk Submit, link do /register
- Obsługiwane zdarzenia: onSubmit
- Walidacja:
  - email: format @ (client-side, also backend @Email)
  - password: not empty
- Typy:
  - AuthLoginCommand { email: string; password: string }
  - AuthResponseDto { accessToken, expiresIn, refreshToken }
- Prospy: onSuccess callback (np. redirect)
- Dodatki:
  - Po otrzymaniu AuthResponseDto zapisać tokeny (preferuj HttpOnly cookie jeśli backend wspiera; inaczej secure storage) i wykonać GET `/api/v1/users/me` w celu pobrania `UserDto` (przechowujemy user.id)

### RegisterForm
- Opis: formularz rejestracji użytkownika, wywołuje POST `/api/v1/auth/register`.
- Elementy: email, password, userName, submit
- Walidacja:
  - email: valid format
  - password: min length 6
  - userName: min length 2, max 100
- Typy:
  - AuthRegisterCommand { email, password, userName }
  - AuthResponseDto
- Prospy: onSuccess callback (np. auto-login + redirect)
- Dodatki:
  - Po rejestracji otrzymujemy AuthResponseDto; wykonać tożsame kroki jak przy logowaniu (zapisać tokeny i pobrać /users/me)

### ErrorBanner / FormField
- Opis: komponent do wyświetlania błędów walidacji i błędów serwera
- Elementy: miejsce na field-level errors i global error
- Prospy: message, type

## 5. Typy
- AuthLoginCommand (TS): { email: string; password: string }
- AuthRegisterCommand (TS): { email: string; password: string; userName: string }
- AuthResponseDto (TS): { accessToken: string; expiresIn: number; refreshToken: string }
- UserDto (TS): jak w `.ai/task-play-view-implementation-plan.md` (id, email, userName, currentLevel, points, stars...)
- ApiError (TS): { status: number; message?: string; details?: any }

## 6. Zarządzanie stanem
- Globalny kontekst/auth hook `useAuth`:
  - Stan: { user?: UserDto, accessToken?: string, refreshToken?: string, isAuthenticated: boolean }
  - Funkcje: login(credentials) => stores tokens and fetchUser(), register(credentials) => same, logout(), refresh(), getAuthHeaders()
  - Automatyczne odświeżanie tokenów: interceptor axios odpytuje POST /api/v1/auth/refresh gdy 401 i refreshToken jest dostępny.
  - Po loginie i rejestracji wywołać GET /api/v1/users/me i zapisać `user` oraz `user.id` (do X-User-Id)
  - Trwałość: localStorage dla refreshToken/accessToken (jeśli HttpOnly cookie nie jest dostępne); zalecane: refreshToken w HttpOnly cookie (backend must support)

## 7. Integracja API
- POST `/api/v1/auth/login` — body: AuthLoginCommand -> returns AuthResponseDto
- POST `/api/v1/auth/register` — body: AuthRegisterCommand -> returns AuthResponseDto (201)
- POST `/api/v1/auth/refresh` — body: AuthRefreshCommand { refreshToken } -> returns AuthResponseDto
- GET `/api/v1/users/me` — headers: Authorization Bearer -> returns UserDto

Szczegóły integracji:
- axios instance z interceptorem: dodawać Authorization Bearer gdy accessToken dostępny
- obsłużyć 401 -> spróbuj refresh token -> powtórz żądanie
- w żądaniach wymagających `X-User-Id` (np. progress submit), `useAuth` powinien zwracać user.id

## 8. Interakcje użytkownika
- Rejestracja: wypełnienie formularza -> submit -> przy udanym zwróceniu tokenów skutkuje przekierowaniem do głównego widoku (np. `/play`) i pobraniem `UserDto`.
- Logowanie: analogicznie; błędne dane -> pokaż komunikat zamknięty (400/401)
- Token refresh: w tle przy wygaśnięciu access tokena interceptorem -> użytkownik powinien pozostać zalogowany bez interakcji jeśli refresh się powiódł

## 9. Warunki i walidacja
- Client-side validate: email format, password length, username length
- Backend annotations zapewniają walidacje -> frontend wyświetla błędy zwrócone z 400
- Przy błędzie 401 po refresh token fail -> logout i redirect na /login

## 10. Obsługa błędów
- 400: pokaż konkretne błędy (jeśli backend zwraca mapę field->msg, zmapować na field errors)
- 401/403: jeśli refresh token nie działa -> wyloguj użytkownika i przekieruj do `/login` z info o konieczności ponownego logowania
- Sieć: toast i retry
- Bezpieczeństwo: nie logować tokenów w konsoli; minimalizować przechowywanie w localStorage (pref. HttpOnly cookies)

## 11. Kroki implementacji
1. Utwórz pliki: `src/pages/Auth/LoginForm.tsx`, `src/pages/Auth/RegisterForm.tsx`, `src/contexts/AuthContext.tsx`, `src/api/apiClient.ts`, `src/types/api.ts`.
2. Zaimplementuj `apiClient` (axios) z interceptorami autorozszerzenia.
3. Zaimplementuj `useAuth` / `AuthContext`:
   - login/register -> zapisz tokens -> fetch /users/me
   - refresh flow
4. Implementuj `LoginForm` i `RegisterForm` z walidacją (React Hook Form lub ręczna walidacja).
5. Dodaj routing `/login` i `/register`.
6. Testy manualne: login, register, token refresh, zachowanie przy braku sieci.
7. Dodaj e2e tests lub unit tests dla `useAuth` (opcjonalnie).

---

Uwagi:
- Jeśli preferujesz, mogę skapować te komponenty kodem (React+TS) bez stylów lub z prostymi stylami i testami jednostkowymi. Powiedz, czy chcesz, bym od razu wygenerował implementację kodową.
