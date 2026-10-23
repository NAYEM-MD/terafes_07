# TRON AI — STEP 2: MYSQL + JPA + FLYWAY FOUNDATION REPORT

**Date:** 2026-10-15  
**Author:** Backend / Spring Architecture Pair Programming  
**Status:** COMPLETE (Step 2 Verification Successful)  

---

## 1. Executive Summary

In Step 2, the persistence foundation for TRON AI was fully implemented and verified on Spring Boot 4.1.1 and Java 21, connected to a live MySQL 8.4 database:
1. **Git Root Safety:** Inspected `C:\Users\engrm\.git` and `C:\Users\engrm\OneDrive\Desktop\Group7`. Ensured all operations run with explicit paths without relying on the parent directory git root.
2. **Database Engine & Environment:** Installed and initialized local MySQL Community Server **8.4.9** on port `3306`. Created database `tron_ai` and user `tronai`. Datasource is fully driven by environment variables (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`).
3. **JPA & Flyway Integration:** Enabled Spring Data JPA, Hibernate ORM 7.4.5, MySQL connector, and Flyway. Removed Step 1 exclusion workarounds. Schema management is strictly owned by Flyway with `spring.jpa.hibernate.ddl-auto=validate`.
4. **Initial Migration & Persistence:** Created `V1__initial_schema.sql` defining foundational tables `users` and `user_settings`. Created JPA entities `User` and `UserSettings` with corresponding Spring Data repositories `UserRepository` and `UserSettingsRepository`.
5. **Database Health Endpoint:** Upgraded `GET /health` to perform a live connection check against the `DataSource`, returning `HTTP 200 {"status":"ok","service":"tron-ai-spring","database":"reachable"}`.
6. **Automated Testing & Live Verification:**
   - `./mvnw.cmd test` passes with **BUILD SUCCESS** (4 tests run, 0 failures, 0 errors).
   - Live Spring Boot server started with `SPRING_PROFILES_ACTIVE=local`, executed Flyway migration V1, confirmed tables exist, verified `GET /health` HTTP 200, and verified non-destructive schema validation across full server restart.
7. **Scope Constraints Enforced:** Frontend untouched, Chat/Ollama not migrated, legacy FastAPI unchanged, no git commit/push.

---

## 2. Environment & Software Versions

- **MySQL Version:** `8.4.9` (MySQL Community Server - GPL, x64)
- **Spring Boot Version:** `4.1.1`
- **Spring Framework Version:** `7.0.9`
- **Hibernate Version:** `7.4.5.Final`
- **Java Version:** OpenJDK `21.0.7` (Eclipse Temurin `jdk-21.0.7.6-hotspot`)
- **Maven Version:** `3.9.9` (via `./mvnw.cmd`)

---

## 3. Git Workspace Safety Check (Part 1)

### Audit Findings
- **`C:\Users\engrm\.git`:** An accidental git repository exists in the user root directory (`C:\Users\engrm`) on branch `master` with no commits. Unanchored git commands in non-git subdirectories automatically bubble up to this root.
- **`C:\Users\engrm\OneDrive\Desktop\Group7`:** The intended workspace contains `frontend/`, `backend/`, and `backend-old/`, but does not currently contain its own `.git` root.

### Recommendation
`C:\Users\engrm\OneDrive\Desktop\Group7` **should become its own dedicated Git repository** (or root of the monorepo tracking `frontend` and `backend`). When approved, the team should run `git init` inside `C:\Users\engrm\OneDrive\Desktop\Group7`.  
During all operations in this mission, git commands were isolated using explicit `-C` paths.

---

## 4. Datasource & Environment Configuration (Part 2)

### 4.1 Environment Variables
All connection parameters are externalized without hardcoding secrets:

| Variable | Description | Default in application.yml | Local Development Override |
| :--- | :--- | :--- | :--- |
| `DB_HOST` | Database host | `127.0.0.1` | `127.0.0.1` |
| `DB_PORT` | Database port | `3306` | `3306` |
| `DB_NAME` | Database schema name | `tron_ai` | `tron_ai` |
| `DB_USERNAME` | Database username | `tronai` | `tronai` |
| `DB_PASSWORD` | Database password | *(empty / required in env)* | `tronai_pass` (in local profile only) |

### 4.2 Configuration Files Updated
- **`application.yml`**: Base production-ready settings with `ddl-auto: validate`, Flyway enabled, and parameterized datasource.
- **`application-local.yml`**: Local developer configuration safe for git-ignored local credentials.
- **`.env.example`**: Standardized environment variable template with MySQL 8 parameters.

---

## 5. JPA + Flyway Architecture (Parts 3 & 4)

### 5.1 Schema Ownership Strategy
- `spring.jpa.hibernate.ddl-auto=validate`: Hibernate validates existing database entities against the active schema on startup, refusing to start if there are mismatches. It **never** drops or modifies tables automatically.
- `spring.flyway.enabled=true`: Flyway executes ordered SQL scripts from `classpath:db/migration` and maintains audit records in `flyway_schema_history`.

### 5.2 Initial Migration: `V1__initial_schema.sql`
File location: `src/main/resources/db/migration/V1__initial_schema.sql`

```sql
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NULL,
    display_name VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    preferred_language VARCHAR(50) NULL DEFAULT 'ja',
    preferred_model VARCHAR(100) NULL DEFAULT 'qwen3:8b',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_settings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_user_settings_user_id ON user_settings(user_id);
```

---

## 6. JPA Entities and Repositories (Part 5)

Foundational domain classes created in package `com.tronai.user`:

1. **`User.java`**:
   - Primary key: `id` (BIGINT, IDENTITY)
   - Unique: `email` (VARCHAR 255)
   - Nullable: `passwordHash`, `displayName`
   - Timestamps: `createdAt` (updatable=false), `updatedAt` managed via JPA lifecycle hooks (`@PrePersist`, `@PreUpdate`)
   - Relationship: One-to-One with `UserSettings` (`cascade = ALL`, `orphanRemoval = true`, `fetch = LAZY`)
2. **`UserSettings.java`**:
   - Foreign key: `user_id` (BIGINT, UNIQUE, referencing `users.id`)
   - Preferences: `preferredLanguage` (default `'ja'`), `preferredModel` (default `'qwen3:8b'`)
   - Timestamps: `createdAt`, `updatedAt`
3. **`UserRepository.java`**:
   - `Optional<User> findByEmail(String email)`
   - `boolean existsByEmail(String email)`
4. **`UserSettingsRepository.java`**:
   - `Optional<UserSettings> findByUserId(Long userId)`

*Note: No authentication, JWT, or registration/login controllers were implemented in this foundation step.*

---

## 7. Database Health Endpoint (Part 6)

`HealthController` was upgraded to inject `javax.sql.DataSource` and check connection validity (`connection.isValid(2)`).

### Contract Response (Database Reachable)
```json
{
  "status": "ok",
  "service": "tron-ai-spring",
  "database": "reachable"
}
```
*HTTP Status:* `200 OK`

### Degraded Response (Database Unreachable)
```json
{
  "status": "degraded",
  "service": "tron-ai-spring",
  "database": "unreachable"
}
```
*HTTP Status:* `503 Service Unavailable`

---

## 8. Test Strategy & Results (Part 7)

### 8.1 Strategy: Isolated In-Memory H2 (MySQL Mode)
- **Design:** Tests run against an isolated in-memory H2 database configured with `MODE=MySQL;DATABASE_TO_LOWER=TRUE` via `application-test.yml` using the `@ActiveProfiles("test")` profile.
- **Why H2 over Testcontainers for Unit Suite:** Testcontainers requires an active Docker daemon, which is not installed on this host environment. H2 in MySQL mode provides instant, zero-dependency CI test execution.
- **Documented Limitations of H2 in MySQL Mode:**
  - H2 does not support certain MySQL-specific connection parameters (e.g. `DEFAULT_NULL_ORDER`, `allowPublicKeyRetrieval`).
  - H2 enforces case sensitivity and schema name conventions slightly differently from MySQL InnoDB.
  - To offset this limitation, comprehensive live verification was performed against actual MySQL 8.4 (see Section 9).

### 8.2 Test Suite Execution (`./mvnw.cmd test`)
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.tronai.HealthEndpointTest
...
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 8.390 s -- in com.tronai.HealthEndpointTest
[INFO] Running com.tronai.user.UserPersistenceTest
...
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.747 s -- in com.tronai.user.UserPersistenceTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  15.013 s
```

---

## 9. Live MySQL 8 Verification (Part 8)

### 9.1 Database Creation & Setup
- Local MySQL 8.4 Community Server started on `127.0.0.1:3306`.
- Database initialized: `tron_ai`.
- Development user: `tronai` (`tronai_pass`).

### 9.2 Flyway Execution on Live MySQL
When Spring Boot was started with `SPRING_PROFILES_ACTIVE=local`, Flyway automatically executed migration `V1__initial_schema.sql`.

Querying `flyway_schema_history`:
```
installed_rank: 1
version: 1
description: initial schema
type: SQL
script: V1__initial_schema.sql
execution_time: 91 ms
success: 1
```

Querying tables in `tron_ai`:
- `flyway_schema_history`
- `users`
- `user_settings`

### 9.3 Live HTTP Health Endpoint Response
```http
HTTP/1.1 200 
Content-Type: application/json

{"status":"ok","service":"tron-ai-spring","database":"reachable"}
```

### 9.4 Non-Destructive Schema Validation Across Server Restart
1. Inserted test record into MySQL:
   - User `persist.check@tronai.dev` (id 1)
   - UserSettings: `preferred_language = 'ja'`, `preferred_model = 'qwen3:8b'` (id 1, user_id 1)
2. Terminated Spring Boot process.
3. Restarted Spring Boot server against the same database.
4. Verified:
   - Server started cleanly without DDL errors (`ddl-auto: validate` succeeded).
   - `GET /health` returned `HTTP 200` with `database: "reachable"`.
   - Records in `users` and `user_settings` remained 100% intact with identical timestamps.

---

## 10. Files Changed in Step 2

1. `backend/backend-spring/pom.xml`: Added `spring-boot-starter-flyway` and test-scoped `h2`.
2. `backend/backend-spring/src/main/resources/application.yml`: Configured MySQL datasource, JPA `ddl-auto: validate`, and Flyway; removed exclusion blocks.
3. `backend/backend-spring/src/main/resources/application-local.yml`: Added local dev datasource settings for `tronai` user.
4. `backend/backend-spring/.env.example`: Updated to standard `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`.
5. `backend/backend-spring/src/main/resources/db/migration/V1__initial_schema.sql`: Initial Flyway migration creating `users` and `user_settings`.
6. `backend/backend-spring/src/main/java/com/tronai/user/User.java`: User JPA entity.
7. `backend/backend-spring/src/main/java/com/tronai/user/UserSettings.java`: UserSettings JPA entity.
8. `backend/backend-spring/src/main/java/com/tronai/user/UserRepository.java`: Spring Data repository for User.
9. `backend/backend-spring/src/main/java/com/tronai/user/UserSettingsRepository.java`: Spring Data repository for UserSettings.
10. `backend/backend-spring/src/main/java/com/tronai/system/HealthController.java`: Upgraded `/health` to check database status.
11. `backend/backend-spring/src/test/resources/application-test.yml`: Added isolated H2 test profile.
12. `backend/backend-spring/src/test/java/com/tronai/HealthEndpointTest.java`: Updated test to assert `database: "reachable"`.
13. `backend/backend-spring/src/test/java/com/tronai/user/UserPersistenceTest.java`: Added test suite for Flyway, User persistence, and UserSettings relationship.
14. `backend/STEP02_MYSQL_FOUNDATION_REPORT.md`: This report.

---

## 11. Confirmation Checklist

- [x] No frontend changes.
- [x] No Chat migration started.
- [x] No Ollama migration started.
- [x] No legacy FastAPI code or `backend-old` modified or deleted.
- [x] No git commit or push performed.
- [x] Spring Boot remains 4.1.1 and Java remains 21.
- [x] Live MySQL 8.4 connected, Flyway migrated, tables verified, tests passing (0 failures).

---
*End of Report — Awaiting approval before proceeding to Step 3.*
