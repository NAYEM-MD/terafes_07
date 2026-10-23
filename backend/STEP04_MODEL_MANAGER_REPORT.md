# TRON AI — STEP 4: MODEL MANAGER FOUNDATION REPORT

**Generated:** 2026-10-16  
**Canonical Workspace:** `C:\Users\engrm\OneDrive\Desktop\Group7`  
**Spring Boot Backend:** `C:\Users\engrm\OneDrive\Desktop\Group7\backend\backend-spring`  
**Current Active Provider:** Ollama (`http://127.0.0.1:11434`)  
**Current Active Model:** `qwen3:8b` (Logical ID: `qwen3-8b`)

---

## 1. Executive Summary

In Step 4, we designed and implemented a clean **Model Manager** abstraction layer for TRON AI. Prior to this step, `ChatService` and `OllamaClient` implicitly hardcoded the Ollama model name `qwen3:8b`. 

With this implementation:
1. `ChatService` is fully decoupled from provider-specific model strings and provider routing.
2. Models are defined as typed `ModelProfile` instances registered in `ModelRegistry`.
3. `ModelManager` orchestrates model profile resolution, validation, provider mapping, and availability probing.
4. A read-only REST API endpoint `GET /api/v1/models` exposes safe configured model metadata with dynamic provider availability status.
5. `GET /health` continues to return existing fields (`status`, `service`, `database`, `ollama`) and now also safely reports `model: "qwen3-8b"`.
6. Full regression testing verified identical chat behavior for all supported languages (English, Japanese, Sinhala, Bengali) and code generation using `qwen3:8b`.
7. All 31 automated tests pass cleanly with 0 failures and 0 errors.

---

## 2. Target Architecture

```
                       ┌──────────────────────┐
                       │    Frontend/Client   │
                       └──────────┬───────────┘
                                  │
                   ┌──────────────┴──────────────┐
                   │ POST /api/v1/chat           │ GET /api/v1/models
                   │ GET  /health                │
                   ▼                             ▼
       ┌───────────────────────┐    ┌─────────────────────────┐
       │     ChatController    │    │     ModelController     │
       └───────────┬───────────┘    └────────────┬────────────┘
                   │                             │
                   ▼                             │
       ┌───────────────────────┐                 │
       │      ChatService      │                 │
       └───────────┬───────────┘                 │
                   │ resolveModel(id)            │ getModelsResponse()
                   ▼                             ▼
       ┌──────────────────────────────────────────────────────┐
       │                     ModelManager                     │
       └───────────┬─────────────────────────────┬────────────┘
                   │                             │
                   ▼                             ▼
       ┌───────────────────────┐    ┌─────────────────────────┐
       │     ModelRegistry     │    │     ModelProperties     │
       │ (qwen3-8b ModelProfile│    │ (tron.default-model)    │
       └───────────────────────┘    └─────────────────────────┘
                   │
                   │ resolves AiProvider by ModelProviderType
                   ▼
       ┌───────────────────────┐
       │       AiProvider      │ (Interface)
       └───────────┬───────────┘
                   │
                   ▼
       ┌───────────────────────┐
       │    OllamaAiProvider   │ (OLLAMA)
       └───────────┬───────────┘
                   │
                   ▼
       ┌───────────────────────┐
       │      OllamaClient     │
       └───────────┬───────────┘
                   │ HTTP POST /api/chat
                   ▼
       ┌───────────────────────┐
       │  Local Ollama Server  │ (qwen3:8b on RTX 4060)
       └───────────────────────┘
```

---

## 3. New Classes & Components

All components are located in the package `com.tronai.ai.model` and its subpackages:

| Class / Enum | Role & Description |
|---|---|
| [`ModelProviderType`](file:///C:/Users/engrm/OneDrive/Desktop/Group7/backend/backend-spring/src/main/java/com/tronai/ai/model/ModelProviderType.java) | Enum representing provider runtimes: `OLLAMA`, `VLLM`. |
| [`ModelCapability`](file:///C:/Users/engrm/OneDrive/Desktop/Group7/backend/backend-spring/src/main/java/com/tronai/ai/model/ModelCapability.java) | Enum representing model capabilities: `CHAT`, `CODING`, `INTERVIEW`, `MULTILINGUAL`. |
| [`ModelProfile`](file:///C:/Users/engrm/OneDrive/Desktop/Group7/backend/backend-spring/src/main/java/com/tronai/ai/model/ModelProfile.java) | POJO domain entity storing model metadata: `id`, `displayName`, `provider`, `providerModelName`, `capabilities`, `enabled`, `contextWindow`, `maxOutputTokens`. |
| [`ModelProperties`](file:///C:/Users/engrm/OneDrive/Desktop/Group7/backend/backend-spring/src/main/java/com/tronai/ai/model/ModelProperties.java) | `@ConfigurationProperties(prefix = "tron")` binding `tron.default-model` (default: `"qwen3-8b"`). |
| [`ModelRegistry`](file:///C:/Users/engrm/OneDrive/Desktop/Group7/backend/backend-spring/src/main/java/com/tronai/ai/model/ModelRegistry.java) | Spring component maintaining registered `ModelProfile` instances. Initializes the single active model `qwen3-8b`. |
| [`ModelNotFoundException`](file:///C:/Users/engrm/OneDrive/Desktop/Group7/backend/backend-spring/src/main/java/com/tronai/ai/model/ModelNotFoundException.java) | Domain runtime exception thrown when an unrecognized model ID is requested. |
| [`ModelDisabledException`](file:///C:/Users/engrm/OneDrive/Desktop/Group7/backend/backend-spring/src/main/java/com/tronai/ai/model/ModelDisabledException.java) | Domain runtime exception thrown when a disabled model ID is requested. |
| [`ModelItemDto`](file:///C:/Users/engrm/OneDrive/Desktop/Group7/backend/backend-spring/src/main/java/com/tronai/ai/model/dto/ModelItemDto.java) | DTO for serialized model items in the read-only models API. |
| [`ModelResponse`](file:///C:/Users/engrm/OneDrive/Desktop/Group7/backend/backend-spring/src/main/java/com/tronai/ai/model/dto/ModelResponse.java) | Response envelope containing `defaultModel` and `models` list. |
| [`ModelManager`](file:///C:/Users/engrm/OneDrive/Desktop/Group7/backend/backend-spring/src/main/java/com/tronai/ai/model/ModelManager.java) | Central service orchestrating default resolution, model lookup, provider dispatch, and model discovery. |
| [`ModelController`](file:///C:/Users/engrm/OneDrive/Desktop/Group7/backend/backend-spring/src/main/java/com/tronai/ai/model/ModelController.java) | REST controller exposing `GET /api/v1/models`. |

---

## 4. Model Profile Structure & Default Model

### Registered Active Model Profile
```java
ModelProfile.builder()
    .id("qwen3-8b")
    .displayName("Qwen3 8B")
    .provider(ModelProviderType.OLLAMA)
    .providerModelName("qwen3:8b")
    .capability(ModelCapability.CHAT)
    .capability(ModelCapability.CODING)
    .capability(ModelCapability.INTERVIEW)
    .capability(ModelCapability.MULTILINGUAL)
    .enabled(true)
    .contextWindow(4096)
    .maxOutputTokens(1536)
    .build();
```

- **Default Logical ID:** `qwen3-8b`
- **Config Key:** `tron.default-model: ${TRON_DEFAULT_MODEL:qwen3-8b}`
- **Provider Model Name:** `qwen3:8b` (sent over HTTP to Ollama)
- Only models registered in `ModelRegistry` can be resolved; arbitrary model strings passed by clients are rejected with HTTP 422 (`{"detail": "Model not found: <id>"}`).

---

## 5. Provider Mapping Workflow

1. Client sends `POST /api/v1/chat` with optional `model` property:
   ```json
   {
     "model": "qwen3-8b",
     "messages": [{"role": "user", "content": "..."}]
   }
   ```
2. `ChatService` invokes:
   ```java
   ModelProfile profile = modelManager.resolveModel(request.getModel());
   AiProvider provider = modelManager.getProvider(profile.getProvider());
   ```
   - If `request.getModel()` is null or blank, `modelManager.getDefaultModel()` returns `qwen3-8b`.
   - If `request.getModel()` is unrecognized, `ModelNotFoundException` is raised -> HTTP 422.
   - If disabled, `ModelDisabledException` is raised -> HTTP 422.
3. `ChatService` maps `profile.getProviderModelName()` ("qwen3:8b") into `ChatGenerationRequest`.
4. `OllamaAiProvider` receives `ChatGenerationRequest` and delegates to `OllamaClient.chat(request)`.
5. `OllamaClient` places `request.getModel()` into the Ollama payload `{"model": "qwen3:8b", ...}`.

---

## 6. Live API Verification

### 6.1 `GET /api/v1/models`
**Request:**
```http
GET http://127.0.0.1:8080/api/v1/models
```
**Response:** HTTP 200 OK
```json
{
  "defaultModel": "qwen3-8b",
  "models": [
    {
      "id": "qwen3-8b",
      "displayName": "Qwen3 8B",
      "provider": "OLLAMA",
      "capabilities": [
        "CHAT",
        "CODING",
        "INTERVIEW",
        "MULTILINGUAL"
      ],
      "enabled": true,
      "available": true
    }
  ]
}
```
*Note: `available` is dynamically probed against `AiProvider.isAvailable()`. Because Ollama is running and responding on port 11434, `available` evaluates to `true`.*

---

### 6.2 `GET /health`
**Request:**
```http
GET http://127.0.0.1:8080/health
```
**Response:** HTTP 200 OK
```json
{
  "status": "ok",
  "service": "tron-ai-spring",
  "database": "reachable",
  "ollama": "reachable",
  "model": "qwen3-8b"
}
```
*All 4 original fields (`status`, `service`, `database`, `ollama`) remain intact; `model: "qwen3-8b"` is safely included.*

---

### 6.3 Chat Live Verification Prompt
**Prompt:** `"Say only: TRON model manager works."`  
**Request:**
```http
POST http://127.0.0.1:8080/api/v1/chat
Content-Type: application/json

{
  "messages": [
    { "role": "user", "content": "Say only: TRON model manager works." }
  ]
}
```
**Response:** HTTP 200 OK
```json
{
  "sessionId": "86545093-a6ca-4ab4-9c0d-7d7ac467ebc8",
  "reply": "TRON model manager works.",
  "content": "TRON model manager works.",
  "language": "en",
  "truncated": false,
  "finishReason": "stop",
  "evalCount": 7,
  "promptEvalCount": 173,
  "search": {
    "mode": "auto",
    "performed": false,
    "status": "unavailable",
    "query": null,
    "detail": "Search provider not configured",
    "results": []
  }
}
```

---

### 6.4 Chat Multilingual Regression Live Test Results

| Language / Test | Input Prompt | HTTP Status | Response Reply | Finish Reason |
|---|---|:---:|---|:---:|
| **English** | "What is 2+2? Answer in one word." | **200** | `'Four.'` | `stop` |
| **Japanese** | "こんにちは！一言で挨拶を返してください。" | **200** | `'こんにちは！'` | `stop` |
| **Sinhala** | "සුභ දවසක්! කෙටියෙන් පිළිතුරු දෙන්න." | **200** | `'සුභ දවස! 🌟'` | `stop` |
| **Bengali** | "নমস্কার! সংক্ষেপে উত্তর দিন।" | **200** | `'নমস্কার! আমি TRON AI, একজন সচেতন সহায়ক।'` | `stop` |
| **Code** | "Write a 1-line Python function that returns 42." | **200** | `'```python\ndef return_42(): return 42\n```'` | `stop` |
| **Explicit Model** | Model: `"qwen3-8b"`, Prompt: "Confirm you are active in one word." | **200** | `'Active'` | `stop` |
| **Unknown Model** | Model: `"unregistered-model"`, Prompt: "Hello" | **422** | `{"detail":"Model not found: unregistered-model"}` | N/A |

### 6.5 Live Server Log Verification
Inspection of Spring Boot server log confirmed Ollama was dispatched with `qwen3:8b`:
```log
DEBUG com.tronai.chat.ChatService      : Sending chat generation request with model qwen3:8b (2 messages) in language ja
DEBUG o.s.web.client.DefaultRestClient : Writing [{model=qwen3:8b, messages=[...], think=false, stream=false, ...}]
DEBUG com.tronai.chat.ChatService      : Sending chat generation request with model qwen3:8b (2 messages) in language si
DEBUG o.s.web.client.DefaultRestClient : Writing [{model=qwen3:8b, messages=[...], think=false, stream=false, ...}]
DEBUG com.tronai.chat.ChatService      : Sending chat generation request with model qwen3:8b (2 messages) in language bn
DEBUG o.s.web.client.DefaultRestClient : Writing [{model=qwen3:8b, messages=[...], think=false, stream=false, ...}]
DEBUG com.tronai.chat.ChatService      : Sending chat generation request with model qwen3:8b (2 messages) in language en
DEBUG o.s.web.client.DefaultRestClient : Writing [{model=qwen3:8b, messages=[...], think=false, stream=false, ...}]
```

---

## 7. Automated Test Suite Results

Command:
```powershell
.\mvnw.cmd test
```

Result:
```
[INFO] Results:
[INFO] 
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  20.997 s
```

### Breakdown of Test Suites
1. **[`ModelManagerTest`](file:///C:/Users/engrm/OneDrive/Desktop/Group7/backend/backend-spring/src/test/java/com/tronai/ai/model/ModelManagerTest.java) (9 tests):**
   - ModelRegistry initializes with active `qwen3-8b` profile.
   - Default model resolves correctly to `qwen3-8b`.
   - `resolveModel(null)` and `resolveModel("   ")` return default model.
   - `resolveModel("qwen3-8b")` returns exact profile.
   - Unknown model fails safely with `ModelNotFoundException`.
   - Disabled model cannot be selected and throws `ModelDisabledException`.
   - `ModelManager` chooses Ollama provider based on provider type `ModelProviderType.OLLAMA`.
   - Unconfigured provider throws `AiProviderUnavailableException`.
   - `getModelsResponse` returns safe configured metadata with availability flag.
   - `getModelsResponse` reports `available: false` when provider health check fails.

2. **[`ModelEndpointTest`](file:///C:/Users/engrm/OneDrive/Desktop/Group7/backend/backend-spring/src/test/java/com/tronai/ai/model/ModelEndpointTest.java) (2 tests):**
   - `GET /api/v1/models` returns HTTP 200 with registered model metadata and availability true.
   - `GET /api/v1/models` returns `available: false` when Ollama is unreachable.

3. **[`ChatEndpointTest`](file:///C:/Users/engrm/OneDrive/Desktop/Group7/backend/backend-spring/src/test/java/com/tronai/chat/ChatEndpointTest.java) (15 tests):**
   - Standard 200 chat response contract validation.
   - Language resolution & system instruction injection for English, Japanese, Sinhala, Bengali.
   - SearchMode handling (`auto`, `off`, `on`).
   - Validation & error handling (empty messages, latest message role, invalid language, malformed JSON).
   - Provider failure handling (HTTP 503 unavailable, HTTP 504 timeout).
   - Default model resolution: Chat resolves `qwen3-8b` and passes `qwen3:8b` to provider.
   - Explicit valid model: Chat accepts `qwen3-8b` and dispatches `qwen3:8b`.
   - Unknown model: Chat rejects unknown model with HTTP 422 `{ "detail": "Model not found: ..." }`.

4. **[`HealthEndpointTest`](file:///C:/Users/engrm/OneDrive/Desktop/Group7/backend/backend-spring/src/test/java/com/tronai/HealthEndpointTest.java) (2 tests):**
   - HTTP 200 with `status`, `service`, `database`, `ollama`, and `model`.
   - HTTP 503 degraded when Ollama is unreachable.

5. **[`UserPersistenceTest`](file:///C:/Users/engrm/OneDrive/Desktop/Group7/backend/backend-spring/src/test/java/com/tronai/user/UserPersistenceTest.java) (3 tests):**
   - MySQL 8 schema validation and JPA entity persistence across users and user settings.

---

## 8. Files Created & Modified

### Created Files
- `src/main/java/com/tronai/ai/model/ModelProviderType.java`
- `src/main/java/com/tronai/ai/model/ModelCapability.java`
- `src/main/java/com/tronai/ai/model/ModelProfile.java`
- `src/main/java/com/tronai/ai/model/ModelProperties.java`
- `src/main/java/com/tronai/ai/model/ModelRegistry.java`
- `src/main/java/com/tronai/ai/model/ModelNotFoundException.java`
- `src/main/java/com/tronai/ai/model/ModelDisabledException.java`
- `src/main/java/com/tronai/ai/model/dto/ModelItemDto.java`
- `src/main/java/com/tronai/ai/model/dto/ModelResponse.java`
- `src/main/java/com/tronai/ai/model/ModelManager.java`
- `src/main/java/com/tronai/ai/model/ModelController.java`
- `src/test/java/com/tronai/ai/model/ModelManagerTest.java`
- `src/test/java/com/tronai/ai/model/ModelEndpointTest.java`

### Modified Files
- `src/main/java/com/tronai/ai/ChatGenerationRequest.java` (added `model` field & builder)
- `src/main/java/com/tronai/ai/OllamaClient.java` (dispatches `request.getModel()` with fallback)
- `src/main/java/com/tronai/chat/ChatService.java` (injects `ModelManager`, resolves model and provider)
- `src/main/java/com/tronai/chat/dto/ChatRequest.java` (added optional `model` field)
- `src/main/java/com/tronai/error/GlobalExceptionHandler.java` (added handlers for `ModelNotFoundException` and `ModelDisabledException`)
- `src/main/java/com/tronai/system/HealthController.java` (added `model` to health response)
- `src/main/resources/application.yml` (added `tron.default-model: ${TRON_DEFAULT_MODEL:qwen3-8b}`)
- `src/test/resources/application-test.yml` (added `tron.default-model: qwen3-8b`)
- `.env.example` (documented `TRON_DEFAULT_MODEL=qwen3-8b`)
- `src/test/java/com/tronai/HealthEndpointTest.java` (added `model` assertion)
- `src/test/java/com/tronai/chat/ChatEndpointTest.java` (added model verification and error handling tests)

---

## 9. Known Limitations & Future Considerations

1. **Read-Only Architecture:** Step 4 implements read-only model querying (`GET /api/v1/models`). There is no dynamic registration or model switching POST endpoint yet (as instructed).
2. **Single Provider Runtime:** Currently, `OllamaAiProvider` is the sole implemented runtime. Future providers like `vLLM` can implement `AiProvider` with `getProviderName() = "vllm"` without modifying `ChatService`.
3. **Static Registry Initialization:** `ModelRegistry` registers `qwen3-8b` programmatically. In future steps, model profiles could optionally be loaded dynamically from configuration files or database tables.

---

## 10. Scope Confirmation

- **Frontend untouched:** Confirmed (zero changes in `frontend/`).
- **Voice untouched:** Confirmed (no Voice endpoints migrated).
- **Interview untouched:** Confirmed (no Interview endpoints migrated).
- **CV untouched:** Confirmed (no CV endpoints migrated).
- **No vLLM added:** Confirmed (only enum placeholder defined in `ModelProviderType`).
- **No new models downloaded:** Confirmed (only existing local `qwen3:8b` utilized).
- **qwen3:8b remains active:** Confirmed.
- **No FastAPI changes:** Confirmed (`backend-old/` untouched).
- **No commit / push:** Confirmed (all changes uncommitted in local workspace).
