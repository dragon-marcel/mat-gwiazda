# Dokument wymagań produktu (PRD) - MatGwiazda

## 1. Przegląd produktu
Produkt MatGwiazda to interaktywna aplikacja webowa przeznaczona do nauki matematyki dla uczniów klas 1–8.
Aplikacja wykorzystuje algorytm AI do generowania zadań matematycznych, dostosowanych do poziomu ucznia, i wprowadza elementy grywalizacji, poprzez system punktów i poziomów, aby zwiększyć zaangażowanie użytkowników.
- Poziom 1: dodawanie i odejmowanie w zakresie 100, porównywanie liczb, proste zadania tekstowe.
- Poziom 2: mnożenie i dzielenie w zakresie 100, proste ułamki.
- Poziom 3: działania do 1000, tabliczka mnożenia, dzielenie z resztą, ułamki zwykłe, jednostki miary (długość, masa, czas).
- Poziom 4: liczby wielocyfrowe, ułamki i ich porównywanie.
- Poziom 5: ułamki dziesiętne, procenty, wyrażenia algebraiczne.
- Poziom 6: działania na ułamkach, proporcje, średnia arytmetyczna.
- Poziom 7: potęgi i pierwiastki, równania i nierówności, obliczenia procentowe.
- Poziom 8: funkcje liniowe, układy równań, twierdzenie Pitagorasa, statystyka i prawdopodobieństwo.
## 2. Problem użytkownika
- Tradycyjne metody nauki matematyki są mało angażujące i monotonne, co skutkuje spadkiem zainteresowania nauką.
- Przygotowywanie zadań edukacyjnych wymaga dużego nakładu pracy i wiedzy dydaktycznej od rodziców oraz nauczycieli.
- Brak narzędzi umożliwiających indywidualne dostosowanie poziomu trudności zadań.
- Konieczność zapewnienia natychmiastowej informacji zwrotnej oraz monitorowania postępów uczniów.

## 3. Wymagania funkcjonalne
- Automatyczne generowanie zadań matematycznych przez AI, dostosowanych do poziomu(1–8).
- Każde zadanie zawiera cztery odpowiedzi, z których tylko jedna jest poprawna, oraz krótkie wyjaśnienie.
- Rejestracja i logowanie użytkowników poprzez e-mail oraz hasło.
- System zbierania gwiazdek: za każdą poprawną odpowiedź użytkownik otrzymuje punkty, a po zdobyciu 50 punktów uzyskuje gwiazdkę, co jest równoznaczne z awansem na kolejny poziom.
- Wyświetlanie postępów użytkownika w czasie rzeczywistym, w tym liczba zdobytych punktów, gwiazdek.
- Zbieranie danych dotyczących ukończonych zadań, zdobytych punktów oraz czasu spędzonego w aplikacji.

### Zmiana w flow (aktualizacja)
W tej wersji flow operacyjny został ujednolicony tak, aby zachować spójność pomiędzy endpointem generowania zadania i jego późniejszym zatwierdzeniem (submit). Kluczowe decyzje:
- Endpoint generujący zadanie (`POST /api/v1/tasks/generate`) tworzy w tej samej transakcji:
  - rekord w tabeli `tasks` (treść zadania, opcje, poprawna odpowiedź ukryta dla klienta),
  - rekord w tabeli `progress` odpowiadający tej instancji zadania, z flagą `finalized = false` (status assigned),
  - aktualizuje `users.active_progress_id` = nowo utworzony `progress.id` — w ten sposób backend pamięta aktualnie przypisaną użytkownikowi próbę nawet przy odświeżeniu strony.
- Endpoint zatwierdzający odpowiedź (`POST /api/v1/progress/submit`) przyjmuje `progressId` oraz `selectedOptionIndex`, weryfikuje użytkownika, ocenia odpowiedź porównując z poprawną opcją po stronie serwera, aktualizuje rekord `progress` (selected_option_index, is_correct, points_awarded, finalized = true, time_taken_ms) oraz w tej samej transakcji aktualizuje profil użytkownika (punkty/gwiazdy/level) i czyści `users.active_progress_id` (= NULL) po zakończeniu próby.

Dlaczego tak?
- Zachowujemy historię próby (`progress`) od momentu wygenerowania zadania — to ułatwia analizę porzuceń, czasu pomiędzy przydziałem a rozwiązaniem, oraz debugging.
- Przypisanie `active_progress_id` do użytkownika umożliwia prosty mechanizm przywrócenia aktualnej próby na frontendzie po odświeżeniu.
- Transakcyjne tworzenie `task + progress + update user` lub `progress submit + update user` zapobiega race conditions i utracie spójności danych.

## 4. Granice produktu
- Brak zaawansowanych raportów oraz analityki wyników.
- Brak integracji z platformami szkolnymi oraz e-dziennikami.
- Brak możliwości tworzenia własnych zadań przez nauczyciela w MVP.
- Aplikacja dostępna wyłącznie w wersji webowej; aplikacja mobilna nie wchodzi w zakres MVP.
- Implementacja trybu wieloosobowego i rywalizacji między uczniami może być rozważona w kolejnych wersjach, ale nie jest priorytetem w MVP.

## 5. Historyjki użytkowników

### US-001
- Tytuł: Rejestracja i logowanie użytkownika
- Opis: Użytkownik powinien mieć możliwość utworzenia konta oraz zalogowania się do aplikacji w celu zabezpieczenia danych i postępów.
- Kryteria akceptacji:
    - Użytkownik może zarejestrować konto poprzez podanie e-mail oraz hasła.
    - Po zalogowaniu, użytkownik widzi swój profil z zapisanymi postępami.

### US-002
- Tytuł: Automatyczne generowanie zadań matematycznych
- Opis: System powinien na podstawie poziomu użytkownika generować zadania matematyczne z czterema odpowiedziami, w tym jedną poprawną. Generowanie tworzy także record `progress` przypisany do użytkownika, aby przy odświeżeniu frontendu móc przywrócić bieżącą próbę.
- Kryteria akceptacji:
    - Algorytm AI generuje zadania dostosowane do określonego poziomu.
    - Każde zadanie zawiera cztery opcje odpowiedzi oraz krótkie wyjaśnienie.

### US-003
- Tytuł: System punktów i gwiazdek
- Opis: Użytkownik zdobywa punkty za poprawne odpowiedzi. Zdobycie 50 punktów skutkuje zdobyciem gwaizdki i awansem na kolejny poziom.
- Kryteria akceptacji:
    - Każda poprawna odpowiedź przyznaje jeden punkt.
    - Po zdobyciu 50 punktów, użytkownik otrzymuje informację o uzyskaniu gwaizdki o awansu na kolejny poziom.
    - Postępy są wyświetlane w czasie rzeczywistym na interfejsie użytkownika.

### US-004
- Tytuł: Rozwiązywanie zadań matematycznych
- Opis: Użytkownik powinien mieć możliwość podejmowania próby rozwiązania zadania matematycznego, zaznaczania wybranej odpowiedzi oraz otrzymywania natychmiastowej informacji zwrotnej.
- Kryteria akceptacji:
    - Użytkownik otrzymuje natychmiastową informację zwrotną po wybraniu odpowiedzi.
    - W przypadku błędnej odpowiedzi, użytkownik otrzymuje dodatkowe wyjaśnienie dotyczące zadania.
    - System zapisuje dane o ukończonych zadaniach oraz czasie trwania sesji.
    - W przypadku porzucenia zadania (np. użytkownik zamyka kartę bez submita) rekord `progress` pozostaje w historii i można raportować porzucone próby.

### US-005
- Tytuł: Przegląd postępów użytkownika
- Opis: Użytkownik powinien mieć możliwość przeglądania szczegółowych informacji o postępach, w tym liczby ukończonych zadań, zdobytych gwiazdek i poziomów.
- Kryteria akceptacji:
    - Profil użytkownika wyświetla liczbę zdobytych punktów i gwiazdek.
    - System generuje statystyki dotyczące czasu spędzonego w aplikacji oraz ukończonych zadań.
    - Dane są aktualizowane w czasie rzeczywistym.

## 6. Metryki sukcesu
- 80% zadań generowanych przez AI musi być uznawanych za odpowiednie przez rodziców lub nauczycieli.
- Średni czas spędzony w aplikacji przez dziecko wynosi minimum 10 minut dziennie.
- Co najmniej 70% użytkowników odwiedza aplikację minimum 3 razy w tygodniu.
- Co najmniej 60% użytkowników osiąga kolejny poziom poprzez zdobycie 50 punktów.


---

Dokument zaktualizowany: flow generowania zadań tworzy teraz również rekord `progress` i przypisuje go do użytkownika (users.active_progress_id). Następne kroki: przygotować migrację SQL, zaktualizować `db-plan.md`, a następnie zaimplementować backend i frontend zgodnie z kontraktami API powyżej.
