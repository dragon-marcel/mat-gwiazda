# Plan implementacji widoku Admin — Zarządzanie użytkownikami

## 1. Przegląd
Widok Admin pozwala administratorom na przegląd listy użytkowników systemu, podgląd szczegółów konta, dezaktywację/reaktywację konta oraz potencjalne zarządzanie rolami. Głównym endpointem obecnie udokumentowanym jest GET `/api/v1/admin/users` zwracający listę wszystkich użytkowników (`List<UserDto>`). W planie zakładamy także istnienie endpointów do modyfikacji użytkownika (PATCH/DELETE) — jeśli ich nie ma, frontend zaimplementuje UI, który korzysta z dostępnych endpointów i poprosi backend o brakujące operacje.

Wymagania bezpieczeństwa: dostęp wyłącznie dla użytkowników z rolą ADMIN (frontend sprawdza `user.role` i ukrywa/chroni UI; autoryzacja wymuszana przez backend).

## Tech stack i UI (Tailwind + shadcn/ui)
- Stack: React + TypeScript; UI stylizowany Tailwind CSS; komponenty interfejsu oparte o shadcn/ui.
- Zalecane pakiety i instalacja (frontend):

```bash
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
npm install axios react-router-dom@6
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
npm install @shadcn/ui
# opcjonalnie react-query
npm install @tanstack/react-query
```

- Dobre praktyki:
  - Użyj shadcn/ui do szybkiego stworzenia tabel, modalów i dialogów potwierdzających.
  - Tailwind ułatwia dostosowanie wyglądu tabel i przycisków. Trzymać komponenty w `src/components/ui`.

## 2. Routing widoku
Widok administracyjny dostępny pod `/admin` (przykład: `http://localhost:3000/admin`).

- `/admin/users` — zarządzanie użytkownikami (widok tabeli, akcje: edytuj, usuń, dodaj).
- `/admin/learning-levels` — zarządzanie poziomami nauczania (widok tabeli, akcje: edytuj, usuń, dodaj).

## 3. Plan implementacji kontrolera Admin — `AdminLearningLevelsController`
Poniższa sekcja uzupełnia plan implementacji widoku administracyjnego o szczegółowy plan backendowy obsługi poziomów nauczania (`learning_levels`) — CRUD dostępny tylko dla roli ADMIN.
Endpointy i szczegóły implementacyjne
- GET /api/v1/admin/learning-levels
- GET /api/v1/admin/learning-levels/{level}
- POST /api/v1/admin/learning-levels
- PUT /api/v1/admin/learning-levels/{level}
- DELETE /api/v1/admin/learning-levels/{level}

