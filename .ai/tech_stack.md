# 🧩 Analiza Tech Stack

### **Frontend**
- **Astro:** Framework nowej generacji do tworzenia szybkich stron webowych, oparty na koncepcji *Islands Architecture*. Umożliwia generowanie statycznych treści (SSG) oraz renderowanie po stronie serwera (SSR), co przekłada się na wysoką wydajność i krótki czas ładowania aplikacji.  
- **React 19 (zintegrowany z Astro):** Odpowiada za dynamiczne komponenty interfejsu – quizy, przyciski interaktywne i panel użytkownika. Połączenie Astro i Reacta zapewnia balans między szybkością a interaktywnością.  
- **Tailwind CSS:** Ułatwia budowanie nowoczesnego i responsywnego interfejsu użytkownika poprzez gotowe klasy CSS.  

### **Backend**
- **Java + Spring Boot:** Zapewnia solidne API do obsługi logiki biznesowej, uwierzytelniania, zarządzania użytkownikami oraz komunikacji z bazą danych.  
- **Komunikacja AI (openrouter.ai):** Integracja z modelami sztucznej inteligencji generującymi zadania matematyczne o różnym poziomie trudności.  

### **Baza danych**
- **Supabase (PostgreSQL):** Zarządzana baza danych z obsługą uwierzytelniania użytkowników, przechowywania postępów nauki i punktów.  
- Supabase udostępnia REST API i wsparcie dla WebSocketów, co pozwala na łatwą integrację z backendem w Spring Boot.  

---

## ⚙️ Skalowalność
- **Frontend (Astro + React):** Architektura komponentowa i statyczne generowanie treści umożliwiają szybkie ładowanie oraz łatwe skalowanie interfejsu.  
- **Backend (Spring Boot):** Stabilny i elastyczny, wspiera rozwój mikroserwisów i integrację z wieloma źródłami danych.  
- **Baza danych (Supabase / PostgreSQL):** Skalowalna, bezpieczna i zoptymalizowana pod dużą liczbę zapytań.  
- **AI (openrouter.ai):** Pozwala łatwo rozszerzać funkcje generowania zadań i dopasowywać je do poziomu ucznia.  

---

## 💰 Koszt i złożoność rozwiązania
- Technologie **Astro, React, Spring Boot i Supabase** są open source i dobrze udokumentowane, co ogranicza koszty licencyjne.  
- Złożoność może wzrosnąć z powodu integracji wielu środowisk (frontend, backend, AI, baza danych).  
- Dla MVP możliwe jest wdrożenie prostszej, monolitycznej wersji aplikacji (np. Spring Boot + Thymeleaf lub Astro jako samodzielny frontend).  

---

## 🔒 Bezpieczeństwo
- **Spring Boot + Spring Security:** Zapewniają kontrolę dostępu, uwierzytelnianie JWT oraz szyfrowanie danych.  
- **Supabase:** Wbudowane mechanizmy bezpieczeństwa i role użytkowników (RLS).  
- **Astro + React:** Przy poprawnej konfiguracji CORS i HTTPS zapewniają bezpieczną komunikację z backendem.  

---

## 🔁 CI/CD i Hosting
- **GitHub Actions:** Automatyzuje procesy build, test i deployment zarówno dla frontendu (Astro + React), jak i backendu (Spring Boot).  
- **DigitalOcean:** Hosting aplikacji backendowej i bazy danych z możliwością skalowania.

---

## 🔬 Testowanie
- Testy jednostkowe:
  - Backend: JUnit 5 + Mockito, Spring Boot Test; użycie Testcontainers (PostgreSQL) dla testów zależnych od bazy danych.
  - Frontend: Vitest + React Testing Library; MSW (Mock Service Worker) do mockowania API w testach jednostkowych i integracyjnych.
  - Rekomendacja: izolować logikę biznesową w serwisach i testować DTO jako immutable `record`y (backend).
- Testy E2E:
  - Playwright (zalecany) — automatyzacja scenariuszy użytkownika (rejestracja → logowanie → rozgrywka → zapis postępu); integracja z `axe-core` dla kontroli dostępności.
  - Alternatywa: Cypress.
- Narzędzia wspierające:
  - REST-assured lub Postman/Newman do testów kontraktowych endpointów backendu.
  - WireMock do stubowania zewnętrznych serwisów (np. openrouter.ai) w testach integracyjnych.
  - k6 / Lighthouse do testów wydajnościowych i generowania metryk.
