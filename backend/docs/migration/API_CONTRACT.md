# TRON AI — FastAPI API Contract

**Source:** `backend-old/backend_openapi.json` (OpenAPI 3.1.0)
**Exported:** 2026-10-15
**FastAPI version:** 0.1.0
**Base URL:** `http://127.0.0.1:8000` (local only)

---

## Authentication

All protected endpoints require the header:

```
X-API-Key: <configured_api_key>
```

**Security Scheme:** `APIKeyHeader` (type: `apiKey`, in: `header`, name: `X-API-Key`)

Demo endpoints and `/health`, `/models` do NOT require the key (no `security` annotation).  
`/api/v1/*` endpoints allow loopback callers (`127.0.0.1`, `::1`) without a key.

---

## Error Schema

All 4xx/5xx responses return:

```json
{ "detail": "<error message>" }
```

FastAPI validation errors (422) return:

```json
{
  "detail": [
    {
      "loc": ["body", "field_name"],
      "msg": "error description",
      "type": "error_type"
    }
  ]
}
```

---

## Endpoints

---

### GET /health

**Tags:** health  
**Auth:** None required  
**Content-Type:** —

**Response 200:**
```json
{
  "status": "ok",
  "ollama": "reachable | unreachable"
}
```

---

### GET /models

**Tags:** health  
**Auth:** None required  
**Content-Type:** —

**Response 200:**
```json
{
  "configured_model": "qwen3:8b",
  "available": true,
  "models": ["qwen3:8b", "..."]
}
```

---

### POST /api/cv/upload

**Tags:** cv  
**Auth:** X-API-Key required  
**Content-Type:** `multipart/form-data`

**Request fields:**
| Field | Type   | Required | Notes                   |
|-------|--------|----------|-------------------------|
| file  | binary | Yes      | PDF only. Max size set by `max_pdf_bytes` |

**Response 200:**
```json
{
  "filename": "resume.pdf",
  "page_count": 2,
  "text_characters": 3412,
  "education": [
    { "school": "Tokyo University", "period": "2018-2022", "detail": "CS" }
  ],
  "skills": ["Python", "FastAPI"],
  "work_experience": [
    { "organization": "Acme Corp", "role": "Engineer", "period": "2022-2024", "detail": "..." }
  ],
  "extraction_notes": ["OCR used for page 2"]
}
```

**Error 415:** Only PDF files are accepted  
**Error 422:** Validation error

---

### POST /api/company/research

**Tags:** company  
**Auth:** X-API-Key required  
**Content-Type:** `application/json`

**Request body:**
```json
{
  "company_name": "Toyota Motor",
  "target_role": "Software Engineer",
  "website_url": "https://toyota.com"
}
```

| Field        | Type   | Required | Constraints              |
|--------------|--------|----------|--------------------------|
| company_name | string | Yes      | min 1, max 120 chars     |
| target_role  | string | Yes      | min 1, max 120 chars     |
| website_url  | string | No       | max 500 chars, or null   |

**Response 200:**
```json
{
  "company_name": "Toyota Motor",
  "target_role": "Software Engineer",
  "search": { "available": true, "detail": "..." },
  "sources": [
    { "url": "https://...", "retrieved": true, "status_code": 200, "note": "..." }
  ],
  "report_ja": "トヨタ自動車は...",
  "verified_facts": ["Founded 1937", "..."],
  "missing_information": ["Recent layoffs data unavailable"]
}
```

---

### POST /api/interview/start

**Tags:** interview  
**Auth:** X-API-Key required  
**Content-Type:** `application/json`

**Request body:**
```json
{
  "candidate_name": "田中 太郎",
  "target_company": "Toyota Motor",
  "target_role": "Software Engineer",
  "cv_summary": "5 years experience...",
  "company_context": "Company overview..."
}
```

| Field           | Type   | Required | Constraints          |
|-----------------|--------|----------|----------------------|
| candidate_name  | string | Yes      | min 1, max 80        |
| target_company  | string | Yes      | min 1, max 120       |
| target_role     | string | Yes      | min 1, max 120       |
| cv_summary      | string | No       | max 8000 or null     |
| company_context | string | No       | max 8000 or null     |

**Response 200:**
```json
{
  "session_id": "550e8400-e29b-41d4-a716-446655440000",
  "question": "自己紹介をお願いします。"
}
```

---

### POST /api/interview/answer

**Tags:** interview  
**Auth:** X-API-Key required  
**Content-Type:** `application/json`

**Request body:**
```json
{
  "session_id": "550e8400-e29b-41d4-a716-446655440000",
  "answer": "私は5年間、ソフトウェアエンジニアとして..."
}
```

| Field      | Type   | Required | Constraints     |
|------------|--------|----------|-----------------|
| session_id | UUID   | Yes      | —               |
| answer     | string | Yes      | min 1, max 4000 |

**Response 200:**
```json
{
  "session_id": "550e8400-e29b-41d4-a716-446655440000",
  "question": "それでは次の質問です。あなたの強みを教えてください。"
}
```

**Error 404:** Session not found  
**Error 409:** Interview already ended

---

### POST /api/interview/end

**Tags:** interview  
**Auth:** X-API-Key required  
**Content-Type:** `application/json`

**Request body:**
```json
{ "session_id": "550e8400-e29b-41d4-a716-446655440000" }
```

**Response 200:**
```json
{
  "session_id": "550e8400-e29b-41d4-a716-446655440000",
  "feedback": "全体的に良い回答でした。..."
}
```

---

### GET /api/interview/{session_id}

**Tags:** interview  
**Auth:** X-API-Key required  
**Path param:** `session_id` (UUID)

**Response 200:**
```json
{
  "session_id": "550e8400-...",
  "candidate_name": "田中 太郎",
  "target_company": "Toyota Motor",
  "target_role": "Software Engineer",
  "status": "active | completed",
  "history": [
    { "role": "interviewer | candidate", "content": "..." }
  ],
  "feedback": null
}
```

---

### POST /api/voice/transcribe

**Tags:** voice  
**Auth:** X-API-Key required  
**Content-Type:** `multipart/form-data`

**Request fields:**
| Field | Type   | Required |
|-------|--------|----------|
| file  | binary | Yes      |

Accepted audio: WAV, WebM, etc. Max size: `max_audio_bytes`

**Response 200:**
```json
{
  "text": "よろしくお願いします",
  "language": "ja",
  "duration_seconds": 2.4
}
```

---

### POST /api/voice/speak

**Tags:** voice  
**Auth:** X-API-Key required  
**Content-Type:** `application/json`

**Request body:**
```json
{ "text": "こんにちは" }
```

| Field | Type   | Required | Constraints     |
|-------|--------|----------|-----------------|
| text  | string | Yes      | min 1, max 400  |

**Response 200:** Binary audio stream  
**Content-Type:** `audio/wav`  
**Headers:** `Content-Disposition: inline; filename=speech.wav`

---

### POST /api/voice/interview-turn

**Tags:** voice  
**Auth:** X-API-Key required  
**Content-Type:** `multipart/form-data`

**Request fields:**
| Field      | Type   | Required |
|------------|--------|----------|
| session_id | UUID   | Yes (form field) |
| file       | binary | Yes      |

**Response 200:**
```json
{
  "session_id": "550e8400-...",
  "transcript": "私は...",
  "question": "それでは次に...",
  "audio_url": "/api/voice/audio/clip_abc123",
  "latency_ms": { "stt": 820, "llm": 1200, "tts": 430, "total": 2450 }
}
```

**Error 409:** Interview already ended

---

### GET /api/voice/audio/{clip_id}

**Tags:** voice  
**Auth:** X-API-Key required  
**Path param:** `clip_id` (string)

**Response 200:** Binary audio stream  
**Content-Type:** `audio/wav`  
**Headers:** `Content-Disposition: inline; filename=question.wav`

---

### POST /api/v1/chat

**Tags:** v1  
**Auth:** X-API-Key required (loopback exempt)  
**Content-Type:** `application/json`

**Request body:**
```json
{
  "sessionId": "abc123",
  "messages": [
    { "role": "user", "content": "Hello", "id": null, "createdAt": null }
  ],
  "language": "ja",
  "searchMode": "auto"
}
```

| Field      | Type                  | Required | Notes              |
|------------|-----------------------|----------|--------------------|
| sessionId  | string                | No       | max 80             |
| messages   | array of ChatMessageIn| Yes      | 1–40 items         |
| language   | string or null        | No       | —                  |
| searchMode | "off"\|"auto"\|"on"   | No       | default: "auto"    |

`ChatMessageIn` fields: `role` (user/assistant/system), `content` (min 1, max 12000), `id?`, `createdAt?`

**Response 200:**
```json
{
  "sessionId": "abc123",
  "reply": "こんにちは！",
  "content": "full content",
  "language": "ja",
  "truncated": false,
  "finishReason": "stop",
  "evalCount": 42,
  "promptEvalCount": 18,
  "search": {
    "mode": "auto",
    "performed": false,
    "status": "skipped",
    "query": "",
    "detail": "",
    "results": []
  }
}
```

---

### POST /api/v1/interviews/start

**Tags:** v1  
**Auth:** X-API-Key required (loopback exempt)  
**Content-Type:** `application/json`

**Request body:**
```json
{
  "companyName": "Toyota Motor",
  "companyNameJa": "トヨタ自動車",
  "jobPosition": "Software Engineer",
  "jobPositionJa": "ソフトウェアエンジニア",
  "jobDescription": "...",
  "industry": "Automotive",
  "language": "japanese",
  "type": "general",
  "difficulty": "intermediate",
  "mode": "manual",
  "questionCount": 5,
  "cvFileName": null
}
```

| Field          | Type    | Required | Notes              |
|----------------|---------|----------|--------------------|
| companyName    | string  | No       | default "未設定"   |
| companyNameJa  | string  | No       | overrides companyName |
| jobPosition    | string  | No       | default "未設定"   |
| jobPositionJa  | string  | No       | overrides jobPosition |
| jobDescription | string  | No       | max 2000           |
| industry       | string  | No       | —                  |
| language       | string  | No       | default "japanese" |
| type           | string  | No       | default "general"  |
| difficulty     | string  | No       | default "intermediate" |
| mode           | string  | No       | default "manual"   |
| questionCount  | integer | No       | 1–8, default 5     |
| cvFileName     | string  | No       | —                  |

**Response 200:**
```json
{
  "interviewId": "550e8400-...",
  "config": { ...V1InterviewStartRequest... },
  "questions": [
    {
      "id": "q-1",
      "questionNumber": 1,
      "totalQuestions": 5,
      "category": "Self-Introduction",
      "categoryJa": "自己紹介",
      "textJa": "自己紹介をお願いします。",
      "textEn": "",
      "expectedKeypoints": [],
      "difficulty": "Intermediate"
    }
  ],
  "currentQuestionIndex": 0,
  "transcript": [
    { "id": "ai-...", "sender": "ai", "text": "...", "timestamp": "14:30", "isFollowUp": false }
  ]
}
```

---

### POST /api/v1/interviews/{interview_id}/answers

**Tags:** v1  
**Auth:** X-API-Key required (loopback exempt)  
**Path param:** `interview_id` (UUID string)  
**Content-Type:** `application/json`

**Request body:**
```json
{
  "questionIndex": 0,
  "answerText": "私は5年間..."
}
```

**Response 200:**
```json
{
  "userMessage": { "id": "user-...", "sender": "user", "text": "...", "timestamp": "14:31", "isFollowUp": false },
  "aiFollowUpMessage": { "id": "ai-...", "sender": "ai", "text": "...", "timestamp": "14:31", "isFollowUp": true },
  "nextQuestionIndex": 1,
  "isFinished": false
}
```

---

### GET /api/v1/interviews/{interview_id}/analysis

**Tags:** v1  
**Auth:** X-API-Key required (loopback exempt)  
**Path param:** `interview_id` (UUID string)

**Response 200:** (inline dict)
```json
{
  "interviewId": "...",
  "companyName": "Toyota Motor",
  "jobPosition": "Software Engineer",
  "interviewType": "general",
  "difficulty": "intermediate",
  "overallScore": 0,
  "communicationScore": 0,
  "japaneseScore": 0,
  "technicalScore": 0,
  "confidenceScore": 0,
  "radarData": [
    { "metric": "Japanese Fluency", "metricJa": "日本語", "score": 0, "benchmark": 0 }
  ],
  "strongPoints": [],
  "weakPoints": [],
  "aiOverallFeedback": "...",
  "starSuggestion": { "situation": "", "task": "", "action": "", "result": "" },
  "durationMinutes": 5,
  "completionDate": "2026/10/15",
  "transcript": [
    { "id": "turn-0", "sender": "ai", "text": "...", "textEn": "", "timestamp": "14:30", "isFollowUp": false }
  ]
}
```

---

### POST /api/v1/interviews/{interview_id}/voice-answer

**Tags:** v1  
**Auth:** X-API-Key required (loopback exempt)  
**Path param:** `interview_id` (UUID string)  
**Content-Type:** `multipart/form-data`

**Request fields:**
| Field          | Type    | Required |
|----------------|---------|----------|
| file           | binary  | Yes      |
| question_index | integer | No       |

**Response 200:**
```json
{
  "interviewId": "...",
  "transcript": "私は...",
  "nextQuestionText": "次の質問は...",
  "nextQuestionIndex": 1,
  "isFinished": false,
  "audio_url": "/api/v1/voice/audio/clip_abc",
  "audioContentType": "audio/wav",
  "latency_ms": { "stt": 820, "llm": 1200, "tts": 430, "total": 2450 }
}
```

---

### POST /api/v1/voice/turn

**Tags:** v1  
**Auth:** X-API-Key required (loopback exempt)  
**Content-Type:** `multipart/form-data`

**Request fields:**
| Field      | Type   | Required | Notes           |
|------------|--------|----------|-----------------|
| file       | binary | Yes      | Audio file      |
| session_id | string | No       | default ""      |
| language   | string | No       | default ""      |

**Response 200:**
```json
{
  "sessionId": "abc123",
  "transcript": "こんにちは",
  "reply": "こんにちは！どうぞよろしく。",
  "content": "full content",
  "language": "ja",
  "audio_url": "/api/v1/voice/audio/clip_xyz",
  "audioContentType": "audio/wav",
  "speechSupported": true,
  "speechDetail": "Japanese speech from local VOICEVOX",
  "latency_ms": { "stt": 820, "llm": 1200, "tts": 430, "total": 2450 }
}
```

---

### GET /api/v1/voice/audio/{clip_id}

**Tags:** v1  
**Auth:** X-API-Key required (loopback exempt)  
**Path param:** `clip_id` (string)

**Response 200:** Binary audio stream  
**Content-Type:** `audio/wav`  
**Headers:** `Content-Disposition: inline; filename=reply.wav`, `Cache-Control: no-store`

---

### GET /demo

**Tags:** demo  
**Auth:** None  

**Response 200:** HTML demo page (or JSON redirect)

---

### POST /demo/cv/upload

**Tags:** demo  
**Auth:** None  
**Content-Type:** `multipart/form-data`

**Request fields:**
| Field | Type   | Required |
|-------|--------|----------|
| file  | binary | Yes      |

**Response 200:** JSON (same shape as `/api/cv/upload` response)

---

### POST /demo/interview/start

**Tags:** demo  
**Auth:** None  
**Content-Type:** `application/json`

Same request body as `/api/interview/start`.  
**Response 200:** JSON

---

### POST /demo/voice/speak

**Tags:** demo  
**Auth:** None  
**Content-Type:** `application/json`

Same request body as `/api/voice/speak`.  
**Response 200:** JSON (not binary in demo mode)

---

### POST /demo/interview/end

**Tags:** demo  
**Auth:** None  
**Content-Type:** `application/json`

Same request body as `/api/interview/end`.  
**Response 200:** JSON

---

### POST /demo/voice/interview-turn

**Tags:** demo  
**Auth:** None  
**Content-Type:** `multipart/form-data`

**Request fields:**
| Field      | Type   | Required |
|------------|--------|----------|
| session_id | UUID   | Yes      |
| file       | binary | Yes      |

**Response 200:** Same shape as `/api/voice/interview-turn`

---

### GET /demo/voice/audio/{clip_id}

**Tags:** demo  
**Auth:** None  
**Path param:** `clip_id` (string)

**Response 200:** Binary audio or JSON in demo mode

---

## Session Behavior

- Sessions are stored **in-memory** (Python dict). They do NOT persist across FastAPI restarts.
- `session_id` is a UUID v4 generated by the server on `/api/interview/start`.
- Concurrent voice operations are serialized via `voice_runtime` semaphore.
- Audio clips are stored in-memory via `audio_store` with a short TTL.

---

## CORS Policy

```
Allow-Origins: http://localhost:5174, http://127.0.0.1:5174
Allow-Methods: GET, POST, OPTIONS
Allow-Headers: Content-Type, X-API-Key
```
