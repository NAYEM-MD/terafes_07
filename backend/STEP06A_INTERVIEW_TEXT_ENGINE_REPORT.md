# TRON AI — STEP 6A: INTERVIEW TEXT ENGINE MIGRATION REPORT

**Date**: 2026-10-16  
**Status**: COMPLETED & VERIFIED  
**Target Environment**: Spring Boot 4.1.1 (Spring 7.0.9) | Java 21 | Ollama (qwen3:8b) | MySQL 8.4  

---

## 1. Legacy Interview Engine Audit & Findings

An audit of the frozen legacy FastAPI backend (`backend-old/backend/app/v1.py`, `backend-old/backend/app/services/interview_service.py`, and `INTERVIEW_AUTO_BUG_BACKEND_REPORT.md`) identified the following key architectural behaviors and pain points:

1. **Request Mapping**:
   - `POST /api/v1/interviews/start`: Accepted `V1InterviewStartRequest` with fields: `companyName`, `companyNameJa`, `jobPosition`, `jobPositionJa`, `jobDescription`, `industry`, `language`, `type`, `difficulty`, `mode`, `questionCount`, `cvFileName`.
   - In legacy, `cvFileName`, `type`, and `difficulty` were captured in session metadata (`_bridges`) but underutilized in dynamic prompting.
2. **Opening Question & Placeholders**:
   - The opening question (Q1) focused on Self-Introduction (`自己紹介`).
   - Questions 2 through `questionCount` were returned as placeholder cards with the text:  
     `"次の質問は、回答後に下の文字起こしへ追加されます。"`  
     This prevented frontend card rendering issues while keeping the server authoritative.
3. **Turn Progression & Server Index**:
   - `POST /api/v1/interviews/{interviewId}/answers`: Handled `questionIndex` and `answerText`.
   - Legacy suffered from an index desynchronization bug (documented in `INTERVIEW_AUTO_BUG_BACKEND_REPORT.md`) where `nextQuestionIndex` was cosmetic (`body.questionIndex + 1`), allowing client/server drift and question repetition.
4. **Repeat Prevention & Fallback**:
   - Fallback topic list (`自己紹介`, `志望動機`, `自己PR`, `強み・弱み`, `学校で学んだこと`, `プロジェクト経験`, `チームワーク`, `技術スキル`, `将来のキャリア目標`, `逆質問`).
   - If an answer was vague or brief (<40 chars), clarification was requested.
5. **Shared Progression Engine (Text + Voice)**:
   - In legacy, `/voice-answer` and `/answers` shared the underlying `interview_service.answer_interview()` method.
   - For Step 6A, text answering is migrated into Spring Boot's `InterviewService.submitAnswer(...)` as the single authoritative progression engine, ready to be called by `voice-answer` once Whisper/STT is migrated.

---

## 2. Session & State-Machine Architecture

The session state machine is server-authoritative, thread-safe, and resides in memory (`ConcurrentHashMap<String, InterviewSession>`):

```
                   ┌────────────────────────────────────────┐
                   │    POST /api/v1/interviews/start       │
                   └───────────────────┬────────────────────┘
                                       │ creates session (index = 0)
                                       ▼
                   ┌────────────────────────────────────────┐
                   │           InterviewSession             │
                   │  - currentQuestionIndex = 0            │
                   │  - isFinished = false                  │
                   │  - questions [Q1 real, Q2..QN cards]   │
                   │  - transcript [Q1 opening]             │
                   └───────────────────┬────────────────────┘
                                       │
    ┌──────────────────────────────────┴──────────────────────────────────┐
    │ POST /api/v1/interviews/{id}/answers                                │
    │ Validates:                                                          │
    │  - session exists (404)                                             │
    │  - session not finished (409)                                       │
    │  - clientIndex == serverIndex (409 on stale/duplicate)              │
    │  - answerText not blank (422)                                       │
    └──────────────────────────────────┬──────────────────────────────────┘
                                       │
                 ┌─────────────────────┴─────────────────────┐
                 │                                           │
         nextIndex < questionCount             nextIndex >= questionCount
                 │                                           │
                 ▼                                           ▼
   ┌───────────────────────────┐               ┌───────────────────────────┐
   │ - Generate next question  │               │ - Mark finished = true    │
   │ - Avoid repeats & rotate  │               │ - Concluding message      │
   │ - Update card[nextIndex]  │               │ - NO dangling Q6          │
   │ - Increment server index  │               │ - isFinished = true       │
   │ - isFinished = false      │               └───────────────────────────┘
   └───────────────────────────┘
```

---

## 3. Classes Created

Package: `com.tronai.interview`

1. **`InterviewController`**:
   - `POST /api/v1/interviews/start`
   - `POST /api/v1/interviews/{interviewId}/answers`
   - `GET /api/v1/interviews/{interviewId}/analysis`
2. **`InterviewService`**: Core orchestrator managing lifecycle, server-authoritative answer progression, index synchronization, and analysis delegation.
3. **`InterviewSessionManager`**: Thread-safe in-memory session repository with `ConcurrentHashMap`.
4. **`InterviewSession`**: Encapsulates session state (UUID, config, turns, transcript, question cards, `ReentrantLock` for concurrency control).
5. **`InterviewTurn`**: Value object recording speaker role (`interviewer` / `candidate`), content, and timestamp.
6. **`InterviewQuestionGenerator`**: Generates context-aware Japanese and English interview questions via `ModelManager -> AiProvider`, with Dice bigram similarity repeat protection and structured topic fallback rotation.
7. **`InterviewAnalysisService`**: Calculates scores (0–100, clamped), radar metrics, strong/weak points, AI overall feedback, and STAR suggestion via `ModelManager -> AiProvider` with deterministic fallback.
8. **`InterviewException`**: Domain exception mapped by `GlobalExceptionHandler` to HTTP 404, 409, 422 with `{"detail": "..."}`.
9. **DTOs** (`com.tronai.interview.dto`):
   - `InterviewConfigDto`
   - `InterviewQuestionDto`
   - `TranscriptMessageDto`
   - `InterviewStartResponse`
   - `InterviewAnswerRequest`
   - `InterviewAnswerResponse`
   - `InterviewAnalysisResultDto`
   - `RadarDataDto`
   - `StarSuggestionDto`

---

## 4. API Contracts

### 1. `POST /api/v1/interviews/start`
- **Request Body**:
  ```json
  {
    "companyName": "LINE Yahoo",
    "companyNameJa": "LINEヤフー株式会社",
    "jobPosition": "Backend Engineer",
    "jobPositionJa": "バックエンドエンジニア",
    "jobDescription": "Java / Spring Boot / 高スループットAPI設計",
    "industry": "IT / Webサービス",
    "language": "japanese",
    "type": "technical",
    "difficulty": "intermediate",
    "mode": "manual",
    "questionCount": 5
  }
  ```
- **Response Body (200 OK)**:
  ```json
  {
    "interviewId": "1e6d5e32-b003-4ba5-8e20-04e78a496436",
    "config": { ... },
    "questions": [
      {
        "id": "q-1",
        "questionNumber": 1,
        "totalQuestions": 5,
        "category": "自己紹介",
        "categoryJa": "自己紹介",
        "textJa": "LINEヤフー株式会社の面接にお越しいただきありがとうございます。1分程度で自己紹介をお願いできますでしょうか？",
        "difficulty": "intermediate"
      },
      {
        "id": "q-2",
        "questionNumber": 2,
        "totalQuestions": 5,
        "category": "会話",
        "categoryJa": "会話",
        "textJa": "次の質問は、回答後に下の文字起こしへ追加されます。",
        "difficulty": "intermediate"
      }
    ],
    "currentQuestionIndex": 0,
    "transcript": [
      {
        "id": "ai-1729065000000",
        "sender": "ai",
        "text": "LINEヤフー株式会社の面接にお越しいただきありがとうございます。1分程度で自己紹介をお願いできますでしょうか？",
        "timestamp": "07:50",
        "isFollowUp": false
      }
    ]
  }
  ```

### 2. `POST /api/v1/interviews/{interviewId}/answers`
- **Request Body**:
  ```json
  {
    "questionIndex": 0,
    "answerText": "はじめまして。山田太郎と申します。大学卒業後、Web企業でJavaおよびSpring Bootを用いたAPI開発に携わってきました。"
  }
  ```
- **Response Body (200 OK, in-progress turn)**:
  ```json
  {
    "userMessage": {
      "id": "user-1729065000001",
      "sender": "user",
      "text": "はじめまして。山田太郎と申します。...",
      "timestamp": "07:50",
      "isFollowUp": false
    },
    "aiFollowUpMessage": {
      "id": "ai-1729065000002",
      "sender": "ai",
      "text": "LINEヤフー株式会社でのバックエンドエンジニアとしての業務内容について、ご存知でしょうか？",
      "timestamp": "07:50",
      "isFollowUp": true
    },
    "nextQuestionIndex": 1,
    "isFinished": false
  }
  ```
- **Final Turn Response (e.g. Turn 4 of 5)**:
  ```json
  {
    "userMessage": { ... },
    "aiFollowUpMessage": {
      "id": "ai-1729065000010",
      "sender": "ai",
      "text": "面接は以上となります。お疲れ様でした。回答の分析結果をご確認ください。",
      "timestamp": "07:51",
      "isFollowUp": true
    },
    "nextQuestionIndex": 5,
    "isFinished": true
  }
  ```

### 3. `GET /api/v1/interviews/{interviewId}/analysis`
- **Response Body (200 OK)**:
  ```json
  {
    "interviewId": "1e6d5e32-b003-4ba5-8e20-04e78a496436",
    "companyName": "LINEヤフー株式会社",
    "jobPosition": "バックエンドエンジニア",
    "interviewType": "technical",
    "difficulty": "intermediate",
    "overallScore": 85,
    "communicationScore": 82,
    "japaneseScore": 88,
    "technicalScore": 86,
    "confidenceScore": 84,
    "radarData": [
      {"metric": "Japanese Fluency", "metricJa": "日本語", "score": 88, "benchmark": 75},
      {"metric": "Technical Logic", "metricJa": "論理性", "score": 86, "benchmark": 70},
      {"metric": "Problem Solving", "metricJa": "課題解決", "score": 84, "benchmark": 70},
      {"metric": "STAR Structure", "metricJa": "回答構成", "score": 80, "benchmark": 65},
      {"metric": "Company Fit", "metricJa": "適性", "score": 85, "benchmark": 70}
    ],
    "strongPoints": [
      "技術的知識が明確で、具体的な実績を示している",
      "企業のビジョンと自分の志望に高いマッチングがある"
    ],
    "weakPoints": [
      "STAR構造での回答がやや曖昧である",
      "自己PRの具体性が不足している"
    ],
    "aiOverallFeedback": "面接全体を通して、技術的な理解力と企業への強い意欲が伝わってきました。...",
    "starSuggestion": {
      "situation": "大学卒業後のWeb企業での開発経験",
      "task": "高スループットAPIの設計・実装",
      "action": "JavaおよびSpring Bootを使用し、DBボトルネックの分析とRedisキャッシュ導入によりレスポンス速度改善",
      "result": "システム性能向上に大きく貢献した"
    },
    "durationMinutes": 1,
    "completionDate": "2026/10/16",
    "transcript": [ ... 11 messages ... ]
  }
  ```

---

## 5. Progression Logic & Safety Validations

1. **Server-Authoritative Progression**: The client passes `questionIndex`. If `questionIndex != session.getCurrentQuestionIndex()`, the request is rejected with **HTTP 409 Conflict** (`{"detail": "This interview turn was already submitted"}`). Duplicate submits or retries cannot skip or replay questions.
2. **Empty Answer Protection**: Empty, null, or whitespace-only answers are rejected with **HTTP 422 Unprocessable Entity** (`{"detail": "Answer text cannot be empty"}`).
3. **Session Termination Guard**: Submissions to an already-finished interview return **HTTP 409 Conflict** (`{"detail": "This interview has already ended"}`).
4. **Repeat Question Prevention**:
   - Every candidate answer is checked.
   - Generated questions are compared against all previously asked questions in the session using bigram Dice coefficient (threshold 0.75) and substring matching.
   - If a duplicate is detected, it is immediately discarded and rotated to the next unaddressed topic fallback.
   - Q1 is never looped.
5. **Exact Turn Completion**:
   - When turn index reaches `questionCount - 1` (e.g. 5th answer for a 5-question interview), `isFinished` is set to `true`.
   - The follow-up message is a polite closing message, not a new question.
   - No 6th question is ever generated or appended.

---

## 6. Automated Test Results

Ran:
```powershell
.\mvnw.cmd test
```

### Result:
```
[INFO] Results:
[INFO] 
[INFO] Tests run: 72, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  24.621 s
```

### New Automated Tests Added (17 tests):
- `InterviewSessionTest` (3 tests): Session creation and state, 404 on missing session, concurrent sessions isolation under 20 parallel threads.
- `InterviewQuestionGeneratorTest` (5 tests): Japanese Q1 self-introduction, English Q1, repetition rejection and topic advancement, vague answer clarification fallback, offline LLM fallback.
- `InterviewAnalysisServiceTest` (4 tests): Empty candidate answers handling, valid AI JSON score clamping and parsing, malformed AI JSON fallback, Ollama unavailable fallback.
- `InterviewEndpointTest` (5 integration tests with MockMvc):
  - `POST /api/v1/interviews/start` (Japanese).
  - `POST /api/v1/interviews/start` (English).
  - Full 5-turn interview progression (0 -> 1 -> 2 -> 3 -> 4 -> FINISHED, Q6 prevention, stale index 409, post-finish 409, and analysis fetch).
  - Empty answer rejected with HTTP 422.
  - Unknown interview ID returns HTTP 404 for answer and analysis.

All 55 existing tests (Chat, CV, ModelManager, Health, MySQL/JPA, UserPersistence) continue to pass 100%.

---

## 7. Live Text Interview Test Verification

Executed against running Spring Boot instance (`http://127.0.0.1:8080`) with active Ollama (`qwen3:8b`):

### Regression Checks:
- `GET /health` -> **HTTP 200** `{"status":"ok","service":"tron-ai-spring","database":"reachable","ollama":"reachable","model":"qwen3-8b"}`
- `GET /api/v1/models` -> **HTTP 200** `defaultModel: qwen3-8b`
- `POST /api/v1/chat` -> **HTTP 200** `reply: INTERVIEW-READY`
- `POST /demo/cv/upload` -> **HTTP 200** `filename: sample_cv.pdf, skills: [Java, Spring Boot, MySQL, Redis, Docker]`

### Live 5-Turn Progression:
- **Start**: `POST /api/v1/interviews/start` (LINEヤフー / バックエンドエンジニア, 5 questions)  
  `interviewId: 1e6d5e32-b003-4ba5-8e20-04e78a496436`  
  `Q1`: `"LINEヤフー株式会社の面接にお越しいただきありがとうございます。1分程度で自己紹介をお願いできますでしょうか？"`
- **Turn 0 (Q1 Answer)**: Candidate introduces background with Java and Spring Boot.  
  Response: `nextQuestionIndex: 1`, `isFinished: false`, Interviewer: `"LINEヤフー株式会社でのバックエンドエンジニアとしての業務内容について、ご存知でしょうか？"`
- **Turn 1 (Q2 Answer)**: Candidate explains motivation around high-throughput distributed architectures.  
  Response: `nextQuestionIndex: 2`, `isFinished: false`, Interviewer: `"御社が提供するサービスにおける技術的な課題や、それに取り組む姿勢についてどう考えていますか？"`
- **Turn 2 (Q3 Answer)**: Candidate details strength in DB bottleneck analysis and Redis caching.  
  Response: `nextQuestionIndex: 3`, `isFinished: false`, Interviewer: `"ご自身の課題や弱みと、それを克服するために取り組んでいることを教えてください。"`
- **Turn 3 (Q4 Answer)**: Candidate explains teamwork, code review, and communication.  
  Response: `nextQuestionIndex: 4`, `isFinished: false`, Interviewer: `"チーム開発におけるコミュニケーションやコード品質への意識についてどう考えていますか？"`
- **Turn 4 (Q5 Answer)**: Candidate outlines career vision to become a tech lead for scalable platforms.  
  Response: `nextQuestionIndex: 5`, `isFinished: true`!  
  Interviewer closing: `"面接は以上となります。お疲れ様でした。回答の分析結果をご確認ください。"` (No Q6 generated).
- **Stale Turn Rejection**: Re-sending turn 0 returned **HTTP 409** `{"detail": "This interview has already ended"}`.
- **Analysis Fetch**: `GET /api/v1/interviews/{id}/analysis` -> **HTTP 200**:
  - `overallScore`: 85, `communicationScore`: 82, `technicalScore`: 86, `japaneseScore`: 88, `confidenceScore`: 84.
  - 5 Radar metrics (score & benchmark).
  - Strong points & weak points generated.
  - Comprehensive `aiOverallFeedback` and structured `starSuggestion`.
  - 11 preserved transcript messages.

---

## 8. Files Changed

### Added Files:
- `backend/backend-spring/src/main/java/com/tronai/interview/InterviewController.java`
- `backend/backend-spring/src/main/java/com/tronai/interview/InterviewException.java`
- `backend/backend-spring/src/main/java/com/tronai/interview/service/InterviewService.java`
- `backend/backend-spring/src/main/java/com/tronai/interview/service/InterviewAnalysisService.java`
- `backend/backend-spring/src/main/java/com/tronai/interview/session/InterviewSession.java`
- `backend/backend-spring/src/main/java/com/tronai/interview/session/InterviewTurn.java`
- `backend/backend-spring/src/main/java/com/tronai/interview/session/InterviewSessionManager.java`
- `backend/backend-spring/src/main/java/com/tronai/interview/generator/InterviewQuestionGenerator.java`
- `backend/backend-spring/src/main/java/com/tronai/interview/dto/InterviewConfigDto.java`
- `backend/backend-spring/src/main/java/com/tronai/interview/dto/InterviewQuestionDto.java`
- `backend/backend-spring/src/main/java/com/tronai/interview/dto/TranscriptMessageDto.java`
- `backend/backend-spring/src/main/java/com/tronai/interview/dto/InterviewStartResponse.java`
- `backend/backend-spring/src/main/java/com/tronai/interview/dto/InterviewAnswerRequest.java`
- `backend/backend-spring/src/main/java/com/tronai/interview/dto/InterviewAnswerResponse.java`
- `backend/backend-spring/src/main/java/com/tronai/interview/dto/InterviewAnalysisResultDto.java`
- `backend/backend-spring/src/main/java/com/tronai/interview/dto/RadarDataDto.java`
- `backend/backend-spring/src/main/java/com/tronai/interview/dto/StarSuggestionDto.java`
- `backend/backend-spring/src/test/java/com/tronai/interview/InterviewSessionTest.java`
- `backend/backend-spring/src/test/java/com/tronai/interview/InterviewQuestionGeneratorTest.java`
- `backend/backend-spring/src/test/java/com/tronai/interview/InterviewAnalysisServiceTest.java`
- `backend/backend-spring/src/test/java/com/tronai/interview/InterviewEndpointTest.java`

### Modified Files:
- `backend/backend-spring/src/main/java/com/tronai/error/GlobalExceptionHandler.java` (added handler for `InterviewException`)

---

## 9. Confirmation of Constraints

- [x] **Frontend untouched**: `frontend/` was NOT modified.
- [x] **Voice-answer NOT implemented in Spring yet**: Voice-answer endpoint deferred until STT/Whisper is migrated.
- [x] **Whisper NOT connected**: Faster-whisper remains in FastAPI for now.
- [x] **Voice untouched**: Voice turn and TTS remain in legacy FastAPI.
- [x] **No interview DB persistence added**: Sessions remain in-memory server-side.
- [x] **No JWT/Auth added**: Auth layers were not introduced.
- [x] **No new models downloaded**: `qwen3:8b` remains the active model.
- [x] **FastAPI untouched**: `backend-old/` remains frozen and unmodified.
- [x] **No commit/push performed**: Git state intact.

---

## 10. Known Limitations

1. **In-Memory Session Storage**: Sessions are maintained in memory (`ConcurrentHashMap`). Server restarts will invalidate active sessions until database session persistence is added in subsequent steps.
2. **Audio Voice-Answer Integration**: Voice answer submissions (`POST /api/v1/interviews/{id}/voice-answer`) continue to route to FastAPI until the Whisper STT audio worker is migrated in Step 6B/Voice migration.
