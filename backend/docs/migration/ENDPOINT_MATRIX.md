# TRON AI — Endpoint Matrix

**FastAPI → Spring Boot Migration**  
**Total Endpoints: 22**  
Extracted from `backend-old/backend_openapi.json` + source code on 2026-10-15.

---

## Matrix Legend

| Column         | Meaning                                         |
|----------------|-------------------------------------------------|
| Method         | HTTP verb                                       |
| Endpoint       | URL path                                        |
| Auth           | X-API-Key required? (Loopback = no key needed)  |
| Content-Type   | Request body type                               |
| Response       | Response content type                           |
| Priority       | Migration priority (1=highest)                  |
| Spring Status  | Current Spring Boot implementation status       |

---

## Full Endpoint Matrix

| # | Method | Endpoint                                        | Auth       | Request Content-Type    | Response Content-Type | Priority | Spring Status |
|---|--------|-------------------------------------------------|------------|-------------------------|-----------------------|----------|---------------|
| 1 | GET    | `/health`                                       | None       | —                       | application/json      | **P0**   | ✅ DONE        |
| 2 | GET    | `/models`                                       | None       | —                       | application/json      | P2       | ⬜ TODO        |
| 3 | POST   | `/api/cv/upload`                                | Required   | multipart/form-data     | application/json      | P3       | ⬜ TODO        |
| 4 | POST   | `/api/company/research`                         | Required   | application/json        | application/json      | P4       | ⬜ TODO        |
| 5 | POST   | `/api/interview/start`                          | Required   | application/json        | application/json      | P2       | ⬜ TODO        |
| 6 | POST   | `/api/interview/answer`                         | Required   | application/json        | application/json      | P2       | ⬜ TODO        |
| 7 | POST   | `/api/interview/end`                            | Required   | application/json        | application/json      | P2       | ⬜ TODO        |
| 8 | GET    | `/api/interview/{session_id}`                   | Required   | —                       | application/json      | P2       | ⬜ TODO        |
| 9 | POST   | `/api/voice/transcribe`                         | Required   | multipart/form-data     | application/json      | P3       | ⬜ TODO        |
|10 | POST   | `/api/voice/speak`                              | Required   | application/json        | audio/wav (binary)    | P3       | ⬜ TODO        |
|11 | POST   | `/api/voice/interview-turn`                     | Required   | multipart/form-data     | application/json      | P3       | ⬜ TODO        |
|12 | GET    | `/api/voice/audio/{clip_id}`                    | Required   | —                       | audio/wav (binary)    | P3       | ⬜ TODO        |
|13 | POST   | `/api/v1/chat`                                  | Loopback   | application/json        | application/json      | **P1**   | ⬜ TODO        |
|14 | POST   | `/api/v1/interviews/start`                      | Loopback   | application/json        | application/json      | **P1**   | ⬜ TODO        |
|15 | POST   | `/api/v1/interviews/{interview_id}/answers`     | Loopback   | application/json        | application/json      | **P1**   | ⬜ TODO        |
|16 | GET    | `/api/v1/interviews/{interview_id}/analysis`    | Loopback   | —                       | application/json      | **P1**   | ⬜ TODO        |
|17 | POST   | `/api/v1/interviews/{interview_id}/voice-answer`| Loopback   | multipart/form-data     | application/json      | P2       | ⬜ TODO        |
|18 | POST   | `/api/v1/voice/turn`                            | Loopback   | multipart/form-data     | application/json      | P2       | ⬜ TODO        |
|19 | GET    | `/api/v1/voice/audio/{clip_id}`                 | Loopback   | —                       | audio/wav (binary)    | P2       | ⬜ TODO        |
|20 | GET    | `/demo`                                         | None       | —                       | HTML/JSON             | P5       | ⬜ TODO        |
|21 | POST   | `/demo/cv/upload`                               | None       | multipart/form-data     | application/json      | P5       | ⬜ TODO        |
|22 | POST   | `/demo/interview/start`                         | None       | application/json        | application/json      | P5       | ⬜ TODO        |
|23 | POST   | `/demo/voice/speak`                             | None       | application/json        | application/json      | P5       | ⬜ TODO        |
|24 | POST   | `/demo/interview/end`                           | None       | application/json        | application/json      | P5       | ⬜ TODO        |
|25 | POST   | `/demo/voice/interview-turn`                    | None       | multipart/form-data     | application/json      | P5       | ⬜ TODO        |
|26 | GET    | `/demo/voice/audio/{clip_id}`                   | None       | —                       | audio/wav (binary)    | P5       | ⬜ TODO        |

---

## Priority Groups

### P0 — Foundation (Step 1B — THIS STEP)
- `GET /health` ✅

### P1 — Frontend Critical (v1 routes used by React frontend)
- `POST /api/v1/chat`
- `POST /api/v1/interviews/start`
- `POST /api/v1/interviews/{id}/answers`
- `GET  /api/v1/interviews/{id}/analysis`

### P2 — Core Interview (Legacy API)
- `/api/interview/*` (start, answer, end, get)
- `/api/v1/interviews/{id}/voice-answer`
- `/api/v1/voice/turn`
- `/api/v1/voice/audio/{clip_id}`
- `GET /models`

### P3 — Voice + CV
- `/api/voice/*` (transcribe, speak, interview-turn, audio)
- `/api/cv/upload`

### P4 — Company Research
- `/api/company/research`

### P5 — Demo (Low priority — not used in production frontend)
- All `/demo/*` endpoints

---

## Response Type Summary

| Response Type       | Endpoints                                          |
|---------------------|----------------------------------------------------|
| `application/json`  | All non-audio endpoints                            |
| `audio/wav` binary  | `/api/voice/speak`, `/api/voice/audio/*`, `/api/v1/voice/audio/*`, `/demo/voice/audio/*` |
| HTML (static)       | `GET /demo`                                        |

---

## Fields Summary by Endpoint Group

### Interview Session Object Fields
`session_id` (UUID), `candidate_name`, `target_company`, `target_role`, `status` (active|completed), `history[]`, `feedback?`

### Voice Turn Latency Object
`stt` (ms), `llm` (ms), `tts` (ms), `total` (ms) — all integers

### CV Upload Response Object
`filename`, `page_count`, `text_characters`, `education[]`, `skills[]`, `work_experience[]`, `extraction_notes[]`
