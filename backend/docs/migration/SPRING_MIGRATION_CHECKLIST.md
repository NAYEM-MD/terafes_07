# TRON AI — Spring Boot Migration Checklist

**Project:** TRON AI Backend Migration FastAPI → Spring Boot  
**Spring Version:** 3.3.5  
**Java Version:** 21  
**Last Updated:** 2026-10-15

---

## ✅ Step 1B — Foundation (THIS STEP — COMPLETE)

### Infrastructure
- [x] Maven installed (3.9.9 at `C:\apache-maven-3.9.9`)
- [x] Java 21 confirmed (Eclipse Temurin 21.0.7)
- [x] Spring Boot 3.3.5 project created
- [x] Base package `com.tronai` established
- [x] Project at `backend/backend-spring/`

### Source Code — Packages Created
- [x] `com.tronai` (root)
- [x] `com.tronai.common`
- [x] `com.tronai.config`
- [x] `com.tronai.system`
- [x] `com.tronai.chat`
- [x] `com.tronai.interview`
- [x] `com.tronai.voice`
- [x] `com.tronai.cv`
- [x] `com.tronai.ai`
- [x] `com.tronai.auth`
- [x] `com.tronai.career`
- [x] `com.tronai.agent`

### Configuration Files
- [x] `pom.xml` (Spring Boot 3.3.5, Java 21, all dependencies)
- [x] `application.yml` (port 8080, JPA/Flyway disabled, actuator, CORS)
- [x] `application-local.yml` (developer profile)
- [x] `.env.example` (all environment variables documented)
- [x] CORS configured (mirrors FastAPI: localhost:5174 only)

### First Endpoint
- [x] `GET /health` → HTTP 200 `{"status":"ok","service":"tron-ai-spring"}`
- [x] `HealthController.java` in `com.tronai.system`

### Tests
- [x] `HealthEndpointTest.java` (MockMvc integration test)
- [x] `./mvnw test` passes

### Documentation — Contract Freeze
- [x] `docs/migration/fastapi-openapi.json` (exported from `backend-old/`)
- [x] `docs/migration/API_CONTRACT.md` (26 endpoints documented)
- [x] `docs/migration/ENDPOINT_MATRIX.md` (priority matrix)
- [x] `docs/migration/SPRING_MIGRATION_CHECKLIST.md` (this file)

### Safety
- [x] FastAPI `backend-old/` NOT deleted — preserved as-is
- [x] No git commit/push performed

---

## ⬜ Step 2 — Chat Migration (PENDING ARCHITECT APPROVAL)

> **STOP. Do not begin until architect signs off on Step 1B.**

### Pre-requisites
- [ ] Architect approval received for Step 1B
- [ ] MySQL server running locally
- [ ] Database schema designed
- [ ] Flyway migration scripts ready

### Tasks
- [ ] Enable JPA + Flyway in `application.yml` (remove exclusions)
- [ ] Configure datasource env vars
- [ ] Create `com.tronai.ai.OllamaClient` (REST client for qwen3:8b)
- [ ] Migrate `POST /api/v1/chat` → `com.tronai.chat.ChatController`
- [ ] Implement `ChatRequest` / `ChatResponse` DTOs
- [ ] Implement `ChatService` with Ollama call
- [ ] Port session management (in-memory → consider Redis or DB)
- [ ] Update `GET /health` to include Ollama connectivity check
- [ ] Write tests for `/api/v1/chat`

---

## ⬜ Step 3 — Interview Migration (PENDING)

- [ ] Migrate `POST /api/v1/interviews/start`
- [ ] Migrate `POST /api/v1/interviews/{id}/answers`
- [ ] Migrate `GET  /api/v1/interviews/{id}/analysis`
- [ ] Persist sessions to MySQL (not in-memory)
- [ ] Write tests

---

## ⬜ Step 4 — Voice Migration (PENDING)

- [ ] Integrate VOICEVOX TTS (local HTTP call)
- [ ] Integrate Whisper STT (local HTTP or subprocess)
- [ ] Migrate `POST /api/v1/interviews/{id}/voice-answer`
- [ ] Migrate `POST /api/v1/voice/turn`
- [ ] Migrate `GET  /api/v1/voice/audio/{clip_id}`
- [ ] Port audio clip store (in-memory → temp files or Redis bytes)

---

## ⬜ Step 5 — CV Migration (PENDING)

- [ ] Integrate PDF text extraction (Apache PDFBox or Tika)
- [ ] Migrate `POST /api/cv/upload`
- [ ] CV structured extraction via Ollama

---

## ⬜ Step 6 — Company Research + Auth (PENDING)

- [ ] Migrate `POST /api/company/research`
- [ ] Implement proper `X-API-Key` auth via Spring Security
- [ ] Replace loopback-exempt logic with proper JWT or API key auth

---

## ⬜ Step 7 — Legacy Endpoints Compatibility (PENDING)

- [ ] Migrate `/api/interview/*` (legacy path group)
- [ ] Migrate `/api/voice/*` (legacy path group)
- [ ] Migrate `/models` endpoint

---

## ⬜ Step 8 — Demo Endpoints (PENDING / Low Priority)

- [ ] Decide: migrate or remove demo endpoints
- [ ] If migrating: port `/demo/*` group

---

## ⬜ Step 9 — Cut-over (PENDING)

- [ ] Switch frontend from port 8000 (FastAPI) to port 8080 (Spring)
- [ ] Smoke test all P1/P2 endpoints
- [ ] FastAPI retained as legacy reference (archive)
- [ ] Git tag: `spring-foundation-stable`

---

## Notes

### Port Assignment
| Service         | Port  | Status         |
|-----------------|-------|----------------|
| FastAPI (legacy)| 8000  | Reference only |
| Spring Boot     | 8080  | ACTIVE (Step 1B+) |
| MySQL           | 3306  | TBD Step 2     |
| VOICEVOX TTS    | 50021 | TBD Step 4     |

### Model Lock
- LLM: `qwen3:8b` via Ollama — **DO NOT CHANGE**
- Ollama default URL: `http://127.0.0.1:11434`

### Key Differences from FastAPI

| Concern          | FastAPI                | Spring Boot Plan         |
|------------------|------------------------|--------------------------|
| Session storage  | In-memory Python dict  | Step 2: MySQL + cache    |
| Audio clips      | In-memory byte store   | Step 4: temp files/Redis |
| Validation       | Pydantic               | Jakarta Validation       |
| Auth             | APIKeyHeader + loopback| Step 6: Spring Security  |
| Serialization    | Pydantic JSON          | Jackson                  |
| Async I/O        | FastAPI async          | Spring WebFlux (if needed)|
