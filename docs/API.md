# Future Bound Tech — REST API Documentation

The platform is a server-rendered Thymeleaf application. A focused set of JSON
REST endpoints (all under `/api/**`) powers the mobile/programmatic client and the
in-page `fetch` calls. This document lists every endpoint, its auth model and request/response shape.

## Conventions

- **Base URL:** `${APP_BASE_URL}` (e.g. `http://localhost:8080` in dev, `https://<your-domain>` in prod).
- **Content type:** `application/json` for request and response bodies, except
  assignment submission which is `multipart/form-data`.
- **Envelope:** every response is wrapped in `ApiResponse`:
  ```json
  { "success": true, "message": "…", "data": { } }
  ```
  Errors use `success: false` with an appropriate HTTP status code.
- **Pagination:** list endpoints accept Spring page params — `page` (0-based),
  `size`, `sort` — and return `PageDto` (`content`, `page`, `size`, `totalElements`, `totalPages`).

## Authentication & authorization

- The REST layer shares the **same session cookie** (`JSESSIONID`) as the browser UI.
  Call `POST /api/auth/login` (or log in through the web form) and reuse the returned
  cookie on subsequent calls.
- `/api/**` endpoints are **CSRF-exempt** (they authenticate by session, not by the
  HTML form-token flow), so no CSRF token is required for JSON calls.
- Unauthenticated `/api/**` calls return **401** (JSON, via `ApiSecurityHandler`),
  never an HTML redirect. Requests to a role-restricted endpoint with the wrong role
  return **403**.
- Roles: `PUBLIC` (no login), `STUDENT`, `TRAINER`, `ADMIN`.

---

## Health

| Method | Path | Auth | Description |
|-------|------|------|-------------|
| GET | `/api/health` | PUBLIC | Liveness probe. Returns status, app name and timestamp. |

## Authentication

| Method | Path | Auth | Body | Description |
|-------|------|------|------|-------------|
| POST | `/api/auth/register` | PUBLIC | `{ "fullName", "email", "phone", "password" }` | Creates a **STUDENT** account. `201` on success, `409` on duplicate. |
| POST | `/api/auth/login` | PUBLIC | `{ "username", "password" }` | Authenticates and starts a session; sets `JSESSIONID`. Returns user summary (id, email, name, role). `401` on bad credentials. |
| POST | `/api/auth/logout` | any | — | Invalidates the session. |

## Courses (public catalog)

| Method | Path | Auth | Description |
|-------|------|------|-------------|
| GET | `/api/courses` | PUBLIC | Paged, sortable catalog of published courses. |
| GET | `/api/courses/{id}` | PUBLIC | Course detail. |
| GET | `/api/courses/{courseId}/syllabus` | PUBLIC | Module + lesson outline for a course. |

## Classes

| Method | Path | Auth | Description |
|-------|------|------|-------------|
| GET | `/api/classes/upcoming` | PUBLIC | Paged upcoming live classes (sorted by `startTime`). |

## Enrollments & payments (student)

| Method | Path | Auth | Body / Params | Description |
|-------|------|------|---------------|-------------|
| POST | `/api/enrollments` | STUDENT | `{ "courseSlug", "batchId?" }` | Enroll the current student in a course (optionally into a batch). |
| GET | `/api/student/enrollments` | STUDENT | page params | The current student's enrollments. |
| GET | `/api/student/assignments` | STUDENT | page params | Assignments for the student's active enrollments. |
| POST | `/api/student/assignments/{id}/submit` | STUDENT | `multipart/form-data` (file + note) | Submit an assignment attachment. |
| GET | `/api/student/payments` | STUDENT | page params | The student's payment history. |

## Trainer

| Method | Path | Auth | Description |
|-------|------|------|-------------|
| GET | `/api/trainer/submissions` | TRAINER | Paged assignment submissions for the trainer's batches. |

## Future Mentor (student, optional learning assistant)

An **optional** study assistant that helps students with Java/Python/SQL/web/AWS/AI,
interview prep, code explanations and syllabus navigation. It is **never** a dependency
for the core LMS: with no AI provider configured it answers from a built-in offline
study guide, and the whole feature can be switched off via `future-mentor.enabled=false`.
Every call is scoped to the signed-in student — it never exposes other students' data
or private admin information — and is rate-limited to blunt abuse / cap outbound spend.

| Method | Path | Auth | Body / Description |
|-------|------|------|--------------------|
| POST | `/api/student/mentor/chat` | STUDENT | `{ "message" }` → `MentorReplyDto` (`aiConfigured`, `aiGenerated`, `reply`). `400` on empty/over-long input, `429` when rate-limited. |
| GET | `/api/student/mentor/history` | STUDENT | The current student's stored turns (oldest first). Empty when history is disabled. |
| DELETE | `/api/student/mentor/history` | STUDENT | Erases the current student's stored history. |

The page lives at `GET /student/mentor` (Thymeleaf, STUDENT role). A non-correctness
disclaimer is always shown; the UI badges each answer as **AI** vs **Offline guide**.

### Configuration (environment variables)

All keys are wired through `application.properties` as `${ENV_VAR:default}` — **no API
keys are ever hardcoded**. AI is **off by default** so the app runs with zero external
dependency.

| Env var | Property | Default | Purpose |
|---------|----------|---------|---------|
| `FUTURE_MENTOR_ENABLED` | `future-mentor.enabled` | `true` | Master on/off for the whole feature. |
| `FUTURE_MENTOR_AI_ENABLED` | `future-mentor.ai-enabled` | `false` | Enable a real AI provider (otherwise offline guide). |
| `FUTURE_MENTOR_PROVIDER` | `future-mentor.provider` | `openai` | Provider id. |
| `FUTURE_MENTOR_API_KEY` | `future-mentor.api-key` | *(empty)* | Provider API key. Required for real AI. |
| `FUTURE_MENTOR_BASE_URL` | `future-mentor.base-url` | `https://api.openai.com/v1` | Chat-completions base URL. |
| `FUTURE_MENTOR_MODEL` | `future-mentor.model` | `gpt-4o-mini` | Model name. |
| `FUTURE_MENTOR_MAX_TOKENS` | `future-mentor.max-tokens` | `500` | Response token cap. |
| `FUTURE_MENTOR_TEMPERATURE` | `future-mentor.temperature` | `0.4` | Sampling temperature. |
| `FUTURE_MENTOR_TIMEOUT_SECONDS` | `future-mentor.timeout-seconds` | `20` | HTTP timeout; on failure it falls back to the offline guide. |
| `FUTURE_MENTOR_HISTORY_ENABLED` | `future-mentor.history-enabled` | `true` | Store chat history (privacy: deletable per student; off = nothing stored). |
| `FUTURE_MENTOR_HISTORY_MAX_TURNS` | `future-mentor.history-max-turns-sent` | `10` | Recent turns replayed to the model. |
| `FUTURE_MENTOR_RATE_LIMIT_RPM` | `future-mentor.rate-limit.requests-per-minute` | `12` | Per-student sliding-window limit. |
| `FUTURE_MENTOR_MAX_INPUT_CHARS` | `future-mentor.rate-limit.max-input-chars` | `2000` | Max question length. |

## Certificates (public)

| Method | Path | Auth | Description |
|-------|------|------|-------------|
| GET | `/api/certificates/verify` | PUBLIC | Verify a certificate number. Used by the public verification page. |

## Razorpay payment endpoints

| Method | Path | Auth | Body | Description |
|-------|------|------|------|-------------|
| POST | `/api/payments/create-order` | STUDENT | `{ "courseSlug", "couponCode"? }` | Creates a Razorpay order for the checkout amount and returns `orderId`, `keyId`, amount and currency. |
| POST | `/api/payments/verify` | STUDENT | `{ "razorpayOrderId", "razorpayPaymentId", "razorpaySignature" }` | Verifies the HMAC-SHA256 checkout signature (constant-time compare) and records a successful payment + enrollment. `400` on signature mismatch. |
| POST | `/api/payments/webhook` | Razorpay | raw JSON + `X-Razorpay-Signature` | Server-to-server webhook. Validated against `RAZORPAY_WEBHOOK_SECRET`. Idempotent. |

> In **demo mode** (`razorpay.enabled=false`, dev/test only) checkout is settled
> server-side without contacting Razorpay. Demo mode must never be enabled in
> production (enforced by `ProductionValidator`).

## Admin (management)

| Method | Path | Auth | Description |
|-------|------|------|-------------|
| GET | `/api/admin/dashboard` | ADMIN | Aggregate stats (users, courses, revenue, etc.). |
| GET | `/api/admin/students` | ADMIN | Paged, searchable student directory. |
| GET | `/api/admin/reports` | ADMIN | Reporting dataset. |
| GET | `/api/admin/payments` | ADMIN | Paged payment ledger. |
| POST | `/api/admin/courses` | ADMIN | Create a course. |
| PUT | `/api/admin/courses/{id}` | ADMIN | Update a course. |
| DELETE | `/api/admin/courses/{id}` | ADMIN | Soft-delete a course (blocked if related records exist). |
| POST | `/api/admin/courses/{courseId}/modules` | ADMIN | Add a syllabus module. |
| POST | `/api/admin/modules/{moduleId}/lessons` | ADMIN | Add a lesson to a module. |
| PUT | `/api/admin/lessons/{lessonId}` | ADMIN | Update a lesson. |
| POST | `/api/admin/classes` | ADMIN | Create a live class. |
| PUT | `/api/admin/classes/{id}` | ADMIN | Update a live class. |

---

## Error model

`ApiExceptionHandler` (`@RestControllerAdvice`) maps exceptions on `/api/**` to JSON:

- `ResourceNotFoundException` → **404**
- `BusinessException` / validation failure → **400 / 409**
- `RateLimitedException` → **429** (Future Mentor per-student throttle)
- `AccessDeniedException` → **403**
- unhandled → **500** (message sanitized; stack traces never returned to the client)

The Thymeleaf UI uses `GlobalExceptionHandler` to render branded `error/error` and
`error/access-denied` pages for the same exception types.
