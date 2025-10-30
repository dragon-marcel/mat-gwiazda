# Plan implementacji usługi OpenRouter dla MatGwiazda

## Krótkie podsumowanie
Celem tego dokumentu jest dostarczenie kompletnego, krok po kroku planu implementacji usługi integrującej OpenRouter API z backendem Spring Boot (Java 17) projektu MatGwiazda, tak aby model LLM generował zadania matematyczne dopasowane do poziomu nauczania przechowywanego w `LearningLevelRepository`.

---

1. Opis usługi
2. Opis konstruktora
3. Publiczne metody i pola
4. Prywatne metody i pola
5. Obsługa błędów
6. Kwestie bezpieczeństwa
7. Plan wdrożenia krok po kroku

---

## 1) Opis usługi
Usługa `OpenRouterService` (nazwa proponowana) odpowiada za:
- Pobranie szablonu promptu i parametrów dla danego poziomu nauczania z `LearningLevelRepository`.
- Zbudowanie kompletnej wiadomości (system + user) i ewentualnych dodatkowych kontekstów (np. poprzednie odpowiedzi, preferencje ucznia).
- Wywołanie OpenRouter API z odpowiednimi nagłówkami (API key), modelem i parametrami.
- Wymuszenie odpowiedzi w ustrukturyzowanym formacie (`response_format`) — JSON zgodny ze schematem.
- Parsowanie i walidacja odpowiedzi (z użyciem Jackson lub Zod-analogów w Javie), mapowanie na wewnętrzne DTO.
- Obsługę retry/backoff, ograniczeń szybkości (rate limits), monitoringu i metryk.

Sukces: metoda `generateTask(short level, UUID actor, Map<String,Object> context)` zwraca obiekt DTO z gotowym zadaniem (w strukturze oczekiwanej przez frontend) lub rzuca kontrolowany wyjątek opisany w sekcji obsługi błędów.

---

## <implementation_breakdown>

1) Kluczowe komponenty usługi OpenRouter (numerowane):

1. OpenRouterClient (HTTP client)
2. OpenRouterProperties / Konfiguracja
3. OpenRouterService (biznesowa warstwa integracji)
4. PromptTemplateProvider (adapter do LearningLevelRepository)
5. ResponseValidator & ResponseMapper
6. Retry/Resilience (circuit breaker, retry, rate limit handler)
7. Audit & Logging
8. HealthCheck / Metrics

Dla każdego komponentu krótkie omówienie i wyzwania:

1. OpenRouterClient
   a. Funkcjonalność:
      - Wysyłanie requestów HTTP(S) do OpenRouter API, ustawianie nagłówków (Authorization: Bearer <KEY>), serializacja request/response.
      - Obsługa timeoutów i deserializacji do predefiniowanych typów.
   b. Wyzwania:
      1) Bezpieczne przechowywanie i rotacja kluczy API.
      2) Odporność na błędy sieci (timeouty, 5xx, 429).
      3) Deserializacja nietypowych lub częściowych odpowiedzi.
   c. Rozwiązania:
      1) Przechowywać klucz w `application.properties` jako placeholder wskazujący na zmienną środowiskową lub w Vault; nie logować wartości klucza.
      2) Użyć `WebClient` (Spring WebFlux) z konfiguracją timeoutów i `Resilience4j` (retry + circuit breaker) oraz odpowiednimi backoffami.
      3) Ustawić schemat `response_format` i walidować odpowiedź po stronie serwera (Jackson + ręczne walidacje / JSON Schema validator).

2. OpenRouterProperties / Konfiguracja
   a. Funkcjonalność: centralizacja wartości konfiguracyjnych: baseUrl, apiKey, defaultModel, timeout, maxRetries, backoff.
   b. Wyzwania:
      1) Różne środowiska (dev/stage/prod) z odmiennymi wartościami.
      2) Bezpieczna rotacja i brak wycieków do logów.
   c. Rozwiązania:
      1) Użyć `@ConfigurationProperties("openrouter")` i profile Spring (`application-dev.properties` etc.).
      2) Użyć mechanizmu tajemnic (Vault, K8s Secrets, environment variables) i nie logować pełnej wartości klucza.

3. OpenRouterService
   a. Funkcjonalność: orchestracja całego przepływu: załadowanie promptu, zbudowanie wiadomości, wywołanie klienta, parsowanie wyniku, mapowanie na DTO i zapis audytu.
   b. Wyzwania:
      1) Utrzymanie separacji odpowiedzialności (SRP) i testowalności.
      2) Utrzymanie stabilnego schematu odpowiedzi niezależnie od zmian w modelu.
   c. Rozwiązania:
      1) Wstrzykiwać zależności przez konstruktor (LearningLevelRepository, OpenRouterClient, ObjectMapper, AuditService).
      2) Wymuszać `response_format` po stronie requestu i dodatkowo walidować odpowiedź.

4. PromptTemplateProvider (adapter do LearningLevelRepository)
   a. Funkcjonalność: pobranie z bazy szablonu promptu dla poziomu `level`, ewentualnie zastąpienie placeholderów (np. {studentName}, {maxDifficulty}).
   b. Wyzwania:
      1) Nieprawidłowe/niekompletne szablony w DB.
      2) Wersjonowanie promptów (history, A/B testing).
   c. Rozwiązania:
      1) Walidować szablony przy zapisie (backend admin) i przy pobraniu -> guard clause.
      2) Dodać pole wersji i tabelę auditów promptów; obsługa fallbacku na wersję domyślną.

5. ResponseValidator & ResponseMapper
   a. Funkcjonalność: sprawdzenie, czy odpowiedź od modelu spełnia JSON Schema, sparsowanie i mapowanie do wewnętrznego DTO.
   b. Wyzwania:
      1) Model zwraca format inny niż oczekiwany.
      2) Długie/niekompletne pola.
   c. Rozwiązania:
      1) Stosować JSON Schema validation (np. networknt JSON Schema Validator) albo ręczne DTO + Jackson + dodatkowe walidacje.
      2) Ustawić limity tokenów i długości; stosować sanitizację i fallbacky (np. generuj minimalne zadanie lub odrzuć z błędem).

6. Retry/Resilience
   a. Funkcjonalność: retry z backoffem, circuit breaker, breaker open fallback.
   b. Wyzwania:
      1) Przeciwdziałanie przekroczeniu limitów API przy równoległych żądaniach.
      2) Jak zachować UX gdy OpenRouter jest niedostępny.
   c. Rozwiązania:
      1) Resilience4j + ograniczanie współbieżności (semaphores) + globalny rate limiter.
      2) Fallback: generowanie prostego, predefiniowanego zadania lub komunikat o błędzie z alternatywną ścieżką.

7. HealthCheck / Metrics
   a. Funkcjonalność: endpoint health check (np. /actuator/health/openrouter) i metryki (latency, success rate).
   b. Wyzwania:
      1) Nie wolno wysłać sensownych promptów do testu (koszt).
      2) Rozróżnienie problemów sieciowych od problemów autoryzacji.
   c. Rozwiązania:
      1) Implementować lekki „ping” endpoint (GET) jeśli OpenRouter wspiera lub wykonać request z małym kosztem i wykonać sudo-check bez generowania dużej odpowiedzi.
      2) Rozróżnić statusy HTTP i mapować do odpowiedniego stanu health.


3) Włączenie elementów wymaganych przez OpenRouter API (system message / user message / response_format / model name / model params)

Poniżej konkretne propozycje i przykłady implementacji w kodzie (fragmenty logiczne i wzorce):

- Komunikat systemowy (system message):
  1) Cel: ustawić reguły stylu, format odpowiedzi i oczekiwania (np. poziom trudności, język, długość).
  2) Przykład:
     - "You are an assistant that generates single-step math problems for primary school students. Always output the task as a JSON object following the provided schema. Keep the text concise and age-appropriate. Provide 1 correct answer and 3 distractors."
  3) Implementacja: traktować jako stały string lub jako pole w DB (dla łatwej edycji przez adminów). W `buildMessages()` dodać jako pierwszą wiadomość (role: system).

- Komunikat użytkownika (user message):
  1) Cel: zawierać specyficzne parametry zadania: poziom, temat, ograniczenia (czas, zakres), kontekst ucznia.
  2) Przykład:
     - "Create a multiplication problem for level 3. Use numbers between 2 and 12. Include the problem statement in Polish. Include an explanation of solution steps in Polish in the "explanation" field."
  3) Implementacja: zbudować poprzez wczytanie szablonu z `LearningLevelRepository` i wstawienie parametrów kontekstowych.

- Ustrukturyzowane odpowiedzi poprzez `response_format` (JSON Schema):
  1) Cel: wymusić na modelu odpowiedź w przewidywalnym JSONie, co upraszcza parsowanie i walidację.
  2) Wzorzec poprawnego `response_format` (użyj dokładnie tego formatu):
     { type: 'json_schema', json_schema: { name: [schema-name], strict: true, schema: [schema-obj] } }
  3) Przykład schematu (numerowany):
     1) Nazwa schematu: `math_prompt_v1`.
     2) Schema obiektu JSON (skrót):
      ```json
     {
      "prompt": {
      "type": "string",
      "minLength": 10,
      "maxLength": 500
      },
      "choices": {
      "type": "array",
      "minItems": 4,
      "maxItems": 4,
      "items": {
      "type": "string",
      "minLength": 1
      }
      },
      "correctIndex": {
      "type": "integer",
      "minimum": 0,
      "maximum": 3
      },
      "explanation": {
      "type": "string",
      "maxLength": 1000
      }
     }
     ```   
     3) Implementacja: wygenerować obiekt `response_format` zgodnie z wzorcem i przesłać go w body requestu do OpenRouter. Po otrzymaniu odpowiedzi wykonać JSON Schema validation (lub deserializację do record DTO i dodatkowe walidacje strict=true). Jeśli `strict=true` i model nie zwróci odpowiednich pól -> odrzucić i retry lub fallback.

- Nazwa modelu:
  1) Cel: umożliwić łatwą zmianę modelu (A/B testy, fallbacky).
  2) Przykłady:
     1) `google/gemini-2.0-flash-exp:free` 
     2) `qwen/qwen3-coder:free` 
  3) Implementacja: trzymać domyślny model w `OpenRouterProperties.defaultModel`.

- Parametry modelu:
  1) Typowe parametry: `temperature`, `top_p`, `max_tokens`, `stop`, `presence_penalty`, `frequency_penalty`.
  2) Przykłady:
     1) Dla prostych, deterministycznych zadań: `{ "temperature": 0.0, "top_p": 0.8, "max_tokens": 400 }`.
     2) Dla kreatywnych opisów: `{ "temperature": 0.7, "top_p": 0.9 }`.
  3) Implementacja: parametry globalne z `OpenRouterProperties` z możliwością nadpisu per-level w DB.


4) Obsługa błędów (scenariusze, numerowane):

1. Brak rekordu `LearningLevel` dla poziomu -> zwrócić 404 (ResponseStatusException) przed wywołaniem API.
2. Błąd autoryzacji (401) od OpenRouter -> log, raise custom `OpenRouterAuthorizationException` -> operator alert + krótkotrwały fallback.
3. Limity lub throttling (429) -> Retry z exponencjalnym backoff + logging; po N niepowodzeniach -> circuit-breaker i fallback.
4. Timeout sieciowy / 5xx -> retry z backoff; jeśli ciągle: fallback lub error 503 do klienta.
5. Niepoprawny format odpowiedzi (niezgodny ze schematem) -> oznaczyć jako `MalformedResponseException`, zapisać surową odpowiedź do audytu (bez PII), spróbować ponownie N razy, potem fallback.
6. Zbyt długi wynik (przekroczenie limitu długości) -> truncate lub odrzucić i retry z niższym `max_tokens`.
7. Błędy serwera DB podczas pobierania promptu -> zwrócić 500/try again; użyć circuit-breaker i retry lokalny.
8. Brak połączenia do OpenRouter (DNS) -> wykryć, health check -> operator alert.

</implementation_breakdown>

---

## 2) Opis konstruktora

Klasa: `OpenRouterService` (przykład)

Konstruktor powinien przyjmować następujące zależności (wyłącznie przez konstruktor, bez `@Autowired`):

- `LearningLevelRepository learningLevelRepository` - do pobrania szablonu promptu i ewentualnych dodatkowych parametrów per-level.
- `OpenRouterClient openRouterClient` - HTTP client (adapter) do komunikacji z OpenRouter.
- `OpenRouterProperties properties` - konfig (baseUrl, defaultModel, apiKeyRef, timeout, retry settings).
- `ObjectMapper objectMapper` - do serializacji/deserialyzacji JSON.
- `AuditService auditService` - opcjonalny, do zapisu promptów i wyników (wraz z metadanymi), używany do debugu i A/B testingu.
- `MeterRegistry meterRegistry` - opcjonalnie do rejestracji metryk (latency, successRate).
- `Logger logger` - SLF4J logger.

Konstruktor powinien:
- Zainicjować pola klasy.
- Weryfikować prekonfigurację (np. czy properties.apiKey jest ustawiony w produkcji) i rzucić wczesny błąd przy braku krytycznej konfiguracji (guard clause).

---

## 3) Publiczne metody i pola

Pola publiczne (w sensie konfigurowalnych wartości):
- `defaultModel` - String (z `OpenRouterProperties`).
- `defaultModelParams` - obiekt konfiguracji parametrów modelu.

Publiczne metody (sygnatury przykładowe):

1. `public MathPromptDto generateTask(short level, UUID actor)`
   - Wejście: poziom i ID aktora (do audytu i kontekstów).
   - Wyjście: `MathPromptDto` (record) zawierający wygenerowane pole `prompt`, `choices`, `correctIndex`, `explanation`.
   - Zabezpieczenia: walidacja poziomu, guard clauses.

2. `public MathPromptDto generateTask(short level, UUID actor, Map<String,Object> overrides)`
   - Jak wyżej, z możliwością nadpisania parametrów (np. model, temperature, tokens, custom constraints).

3. `public HealthDto health()`
   - Sprawdzenie dostępności OpenRouter (lekki request lub stan circuit-breakera).

4. `public void setModelForLevel(short level, String model)`
   - Admin API do nadpisania modelu dla poziomu (zabezpieczone rolami).

5. `public List<String> supportedModels()`
   - Zwraca listę modeli dostępnych/fallbacków skonfigurowanych w properties.

Każda metoda powinna być drobiazgowo testowana (unit + integration).

---

## 4) Prywatne metody i pola

Pola prywatne (przykładowe):
- `LearningLevelRepository learningLevelRepository`
- `OpenRouterClient openRouterClient`
- `OpenRouterProperties properties`
- `ObjectMapper objectMapper`
- `AuditService auditService`
- `Semaphore requestSemaphore` lub `RateLimiter` (opcjonalnie)

Prywatne metody (odpowiedzialności):

1. `private PromptTemplate fetchPromptTemplate(short level)`
   - Pobiera encję `LearningLevel` z repozytorium, waliduje treść i zwraca DTO z szablonem i parametrami.

2. `private List<Message> buildMessages(PromptTemplate template, Map<String,Object> overrides, UUID actor)`
   - Buduje listę wiadomości: system + user + (opcjonalne) assistant (kontext historyczny). Zastępuje placeholdery.

3. `private OpenRouterRequest buildRequest(List<Message> messages, String model, Map<String,Object> modelParams, ResponseFormat responseFormat)`
   - Składa gotowy body request do OpenRouter, dołącza `response_format` zgodny ze wzorcem.

4. `private MathProblemDto callOpenRouterAndParse(OpenRouterRequest req)`
   - Wywołuje klienta, obsługuje retry/backoff, deskryptuje odpowiedź, waliduje JSON Schema i mapuje na `MathProblemDto`.

5. `private void auditRequestResponse(PromptTemplate tpl, OpenRouterRequest req, String rawResponse, MathProblemDto dto, UUID actor)`
   - Zapis audytu (bez wrażliwych danych), wersjonowanie użytego promptu.

6. `private ResponseFormat buildResponseFormatSchema()`
   - Zwraca obiekt `response_format` zgodny ze wzorcem wykorzystanym w OpenRouter.

7. `private MathProblemDto fallbackGenerate(short level, UUID actor)`
   - Generuje prosty, predefiniowany task (hard-coded) używany w przypadku długotrwałych niepowodzeń OpenRouter.

---

## 5) Obsługa błędów (szczegóły implementacyjne)

Zalecane wyjątki (niższa warstwa -> wyższa warstwa kontrolowana):
- `LearningLevelNotFoundException` -> mapować na 404 w kontrolerze.
- `OpenRouterAuthorizationException` -> 502 lub 500 z odpowiednim logiem i alertem.
- `OpenRouterRateLimitException` -> 429 lub 503 w zależności od polityki; log i metric.
- `OpenRouterTimeoutException` -> 504 Gateway Timeout.
- `MalformedResponseException` -> 502 / zapisać surową odpowiedź i alert.
- `OpenRouterUnavailableException` -> użyć fallback.

Praktyki:
- Używaj `@ControllerAdvice` i spójnego DTO błędu (zgodnie z repozytorium project rules). Zwracaj `error_dto` z pliku `copilot-instructions.md` (zgodnie z zasadami projektu).
- Loguj błędy używając SLF4J z odpowiednimi poziomami (WARN/ERROR) i bez PII.
- Dodaj metryki i alerty (Prometheus / Alertmanager) dla 429/5xx/timeout.
- Retry policy: maxRetries=3, początkowy backoff 500ms, expo x2, maks 5s.

---

## 6) Kwestie bezpieczeństwa

1. Przechowywanie klucza API:
   - Trzymać w environment variables lub secret managerze (Vault / Kubernetes secrets) w środowisku dev zapisać w pliku .env ktróry bedzi eodany do gitIgnore.
   - Nie hardkodować w `application.properties` w repo.

2. Ograniczenie dostępu do endpointów:
   - Admin endpoints (np. setModelForLevel) zabezpieczyć rolami (Spring Security + method security).

3. Walidacja wejścia:
   - Zanim zbudujesz prompt, waliduj wartości od użytkownika: długości, typy, zakresy.

4. PII i audyt:
   - Nie logować danych osobowych. Jeśli konieczne, hashować/anonimizować.

5. Bezpieczeństwo wywołań HTTP:
   - Wymusić TLS (https) i sprawdzić certyfikaty.

6. Ograniczenia kosztów i nadużyć:
   - Rate limiting per-user i per-system.

7. Bezpieczeństwo schematu odpowiedzi:
   - Waliduj typy i długości, filtruj HTML/JS w polach tekstowych (XSS risk przy wyświetlaniu w frontendzie).

---

## 7) Plan wdrożenia krok po kroku (szczegółowy)

Poniższe kroki są dostosowane do stacku (Spring Boot, Java 17, LearningLevelRepository).

Faza A — Przygotowanie konfiguracji:

1. Dodać `OpenRouterProperties`:
   - Klasa z `@ConfigurationProperties(prefix = "openrouter")` zawierająca: `baseUrl`, `apiKeyRef` (env var name), `defaultModel`, `timeoutMs`, `maxRetries`, `backoffBaseMs`.
   - Dodać przykładowe wartości do `*.env` (tylko placeholdery, production używa secret manager).

2. Dodać bean `OpenRouterClient`:
   - Implementacja używa `WebClient` z `baseUrl` i domyślnymi nagłówkami (Authorization). Ustawić timeouts i exchange filter do logowania (maskowanie headerów).
   - Wprowadzić testable adapter `OpenRouterClient` z `interface` i `HttpOpenRouterClient` jako implementacją.

Faza B — Implementacja serwisu:

3. Utworzyć `OpenRouterService` z konstruktorem przyjmującym zależności wymienione wcześniej.

4. Implementować `generateTask(short level, UUID userId)`:
   - Fetch prompt template z `LearningLevelRepository.findById(level)`. Jeśli brak -> throw `LearningLevelNotFoundException`.
   - Zbuduj messages = [systemMessage, userMessage] gdzie `userMessage` to wypełniony szablon/polecenie.
   - Stwórz `response_format` JSON Schema zgodnie z sekcją powyżej.
   - Zbuduj request body i użyj `openRouterClient.send(request)`.
   - Waliduj odpowiedź: JSON Schema validation / try deserializacji do `MathPromptDto`.
   - Jeśli OK: audit + return DTO.
   - Jeśli nie: retry (zgodnie z polityką retry) lub fallback.

5. Parsowanie i walidacja:
   - Zaimplementuj `ResponseValidator` korzystający z `networknt` JSON Schema validator (lub hand-made przy użyciu Jackson + Bean Validation) dla porównania struktury.

Faza C — Resilience, testy i obsługa błędów:

6. Dodać Resilience4j configuration:
   - Retry (max 3), CircuitBreaker (slidingWindow=100, failureRateThreshold=50), RateLimiter (limit per second).
   - Podpiąć do `OpenRouterClient` lub do `OpenRouterService`
---

## Przykładowe payloady i schematy

1) Przykładowy `response_format` (w ciele requestu):

```json
{
  "type": "json_schema",
  "json_schema": {
    "name": "math_prompt_v1",
    "strict": true,
    "schema": {
      "type": "object",
      "properties": {
        "problemId": {
          "type": "string"
        },
        "language": {
          "type": "string",
          "enum": [
            "pl"
          ]
        },
        "level": {
          "type": "integer",
          "minimum": 1,
          "maximum": 8
        },
        "prompt": {
          "type": "string",
          "minLength": 10,
          "maxLength": 500
        },
        "choices": {
          "type": "array",
          "minItems": 4,
          "maxItems": 4,
          "items": {
            "type": "string",
            "minLength": 1
          }
        },
        "correctIndex": {
          "type": "integer",
          "minimum": 0,
          "maximum": 3
        },
        "explanation": {
          "type": "string",
          "maxLength": 1000
        }
      },
      "required": [
        "problemId",
        "language",
        "level",
        "prompt",
        "choices",
        "correctIndex"
      ],
      "additionalProperties": false
    }
  }
}
```

2) Przykładowy system message:

```
You are an assistant that must always output a single JSON object according to the given schema. Generate a single math problem appropriate for the requested level. Keep the language in Polish. Provide exactly 4 choices, with one correct answer. Ensure correctIndex is 0-based.
```

3) Przykładowy user message (po wypełnieniu szablonu z DB):

```
Create a single multiplication problem for level 3. Use numbers between 2 and 12. Provide statement in Polish and a short step-by-step explanation in the "explanation" field.
```

4) Przykładowe parametry modelu (body):

```json
{
  "model": "gpt-4o-mini",
  "temperature": 0.0,
  "top_p": 0.8,
  "max_tokens": 400,
  "response_format": { /* as above */ }
}
```

---

## Wymagane elementy testów

- Unit tests: `OpenRouterService` (mocky repo i client), `ResponseValidator`.
- Integration tests: WireMock nagrywający requesty i serwujący gotowe odpowiedzi, weryfikacja, że `response_format` było obecne.
- E2E (opcjonalne): ręczny test integracyjny z prawdziwym OpenRouter (w staging, z ograniczonym ruchem).

---

## Podsumowanie
Dokument ten precyzuje architekturę i szczegóły implementacji usługi OpenRouter w backendzie Spring Boot projektu MatGwiazda. Kluczowe punkty do wykonania: bezpieczne przechowywanie klucza, wymuszanie `response_format` z walidacją strict=true, zastosowanie Resilience4j dla retry/circuit breaker oraz testy integracyjne z symulowanym API.


---

*Uwaga:* plik zawiera konkretne wytyczne i przykłady schematów, które developer może bezpośrednio przekuć na implementację. 


