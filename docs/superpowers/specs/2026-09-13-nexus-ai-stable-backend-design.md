# Nexus AI Stable Backend Design

Date: 2026-09-13
Branch: `ai-nexus-apk`
Status: Proposed for user review

## Goal

Replace Nexus AI's fragile direct-to-public-anonymous-AI calls with a stable architecture where the Android app talks only to a Nexus-owned backend. Keep the app login-free for the user, preserve local chat history, keep API/provider secrets out of the APK, and provide a local fallback when the backend cannot be reached.

## Non-goals

- Do not copy or impersonate proprietary closed models.
- Do not embed provider master API keys in the APK.
- Do not claim full offline parity with frontier cloud models.
- Do not silently fabricate successful AI output when neither backend nor local fallback can complete the task.

## User Experience

1. User opens Nexus AI with no provider login screen.
2. Existing local chat history remains visible.
3. User enters a prompt and chooses or auto-detects a mode.
4. Nexus sends one request to the Nexus backend.
5. Backend selects a healthy model/provider based on task type and current availability.
6. Response is shown and stored in local chat history.
7. If the backend is temporarily unreachable, Nexus attempts a local fallback for supported text tasks.
8. If neither path can complete the request, Nexus shows a precise recoverable error with a Retry action instead of the current generic "check your internet" message.

## Architecture

### Android app

Keep the current `aihub` app and UI, but replace direct anonymous provider calls with a single `NexusBackendClient` abstraction.

Core client components:

- `NexusBackendClient`: sends task requests to the backend.
- `BackendRequest`: prompt, mode, locale, client version, conversation context metadata.
- `BackendResponse`: content kind, content, model/backend metadata safe for UI, retryability.
- `LocalFallbackEngine`: lightweight fallback for basic chat/code assistance when the backend is unavailable.
- `ConnectionFailureClassifier`: converts network exceptions into specific user-facing failure categories.
- Existing `ChatHistoryStore`: remains the source of persistent local conversation history.

The Android app will no longer contact Vireonix, Animica, Cehpoint, or similar public anonymous model endpoints directly.

### Nexus backend

A small HTTPS service with one stable public hostname owned by the Nexus deployment.

Primary endpoints:

- `GET /health` — liveness/readiness and provider health summary without secrets.
- `POST /v1/tasks` — unified task execution endpoint.
- `GET /v1/config` — signed or versioned routing/config metadata safe for clients.

Backend responsibilities:

- Validate input and enforce request size limits.
- Route by capability: chat, code, research, image, video, audio.
- Maintain provider/model health and fail over between configured engines.
- Keep provider credentials exclusively in server-side environment secrets.
- Apply timeouts, bounded retries, and circuit-breaker behavior.
- Return normalized JSON to the APK regardless of provider-specific response format.
- Never expose provider secrets to the APK or logs.

## Provider strategy

The backend uses adapters behind a common interface. Provider choice is configuration, not Android code.

Suggested adapter contract:

- `supports(capability)`
- `execute(request)`
- `healthCheck()`
- `priority`
- `timeout`
- `enabled`

Routing rules:

1. Filter to enabled adapters supporting the requested capability.
2. Exclude adapters currently marked unhealthy by the circuit breaker.
3. Try by configured priority.
4. Accept only responses that pass quality validation.
5. Fail over to the next adapter on timeout, transport failure, malformed response, or known low-quality/refusal patterns.
6. Return a structured error only after all configured paths fail.

This keeps the Android APK stable while providers can be changed server-side.

## Local fallback

The local fallback is intentionally limited and honest.

Supported initially:

- Basic Turkish/English chat templates and deterministic helpers.
- Prompt improvement and restructuring.
- Code explanation/checklists and lightweight offline guidance.
- Previously cached useful responses when the exact same request hash exists.

Not supported offline initially:

- Frontier-quality free-form reasoning.
- Fresh web research.
- Image/video/music generation.

For unsupported offline tasks, the app shows that Nexus backend access is temporarily unavailable and keeps a Retry action.

## Research mode

Research remains backend-orchestrated:

1. Backend performs live web search through configured search transports.
2. Backend normalizes sources into title, URL, excerpt, timestamp when available.
3. Model receives grounded source context.
4. Response includes a source list and never invents URLs.
5. Android renders the normalized result and stores the text response locally.

## Error handling

Replace generic network errors with structured categories:

- `NO_NETWORK`: device has no usable internet path.
- `DNS_FAILURE`: name resolution failed.
- `TLS_FAILURE`: secure connection could not be established.
- `BACKEND_TIMEOUT`: Nexus backend did not answer in time.
- `BACKEND_UNAVAILABLE`: backend returned 5xx/readiness failure.
- `UPSTREAM_EXHAUSTED`: backend is online but all configured AI engines failed.
- `RATE_LIMITED`: retry-after metadata when available.
- `UNSUPPORTED_OFFLINE`: local fallback cannot perform that capability.

UI behavior:

- Keep the user's message in history.
- Do not permanently save transient "working" cards.
- Save final successful answers.
- Save a compact failure event only when useful for continuity.
- Show `TEKRAR DENE` for retryable failures.
- Retry reuses the original prompt/mode without duplicating the user's history entry.

## Security

- No provider master secret is stored in APK resources, source, BuildConfig, or SharedPreferences.
- Backend secrets are environment variables or hosting-platform secret storage.
- HTTPS only.
- Certificate pinning may be added after the backend hostname is stable; initial rollout uses normal Android trust validation to avoid bricking the client during certificate rotation.
- Install-scoped client identifier is generated locally; it is not treated as a secret.
- Optional backend abuse protection uses rotating installation tokens issued by the backend, not a static master key embedded in the APK.
- Logs redact prompts by default or truncate them; secrets are never logged.

## Persistence

Existing `ChatHistoryStore` remains local-first.

- App restarts restore the conversation.
- New successful messages append instead of replacing history.
- "Sohbeti temizle" remains available with confirmation.
- Backend is not required to store conversation history for v1 of this architecture.

This keeps private chat history on the phone unless a request must be sent to the backend for generation.

## Versioning and updates

- Android client sends `clientVersion` with each request.
- Backend returns minimum-supported-client metadata through `/v1/config`.
- Routing/provider changes happen server-side without requiring a new APK.
- APK update is required only for UI/protocol/client capability changes.

## Testing strategy

### Android unit tests

- Backend request serialization and response parsing.
- Failure classification.
- Retry does not duplicate the user history entry.
- Existing chat persistence tests remain green.
- Local fallback clearly distinguishes supported vs unsupported tasks.

### Backend unit tests

- Provider failover ordering.
- Circuit breaker behavior.
- Timeout and malformed-response handling.
- Quality-filter rejection followed by failover.
- Secret redaction.
- Research source normalization.

### Integration tests

- `GET /health` returns healthy state.
- `POST /v1/tasks` returns a normalized Turkish text response for a Turkish prompt.
- Simulated primary-provider failure causes secondary-provider success.
- Backend-unavailable test triggers Android local fallback for supported text tasks.
- Unsupported offline capability produces retryable structured error, not fabricated output.

### Release gate

A release APK is produced only when:

- Android unit tests pass.
- Backend tests pass.
- Android lint passes.
- Backend live health check passes.
- At least one end-to-end text request through the Nexus backend succeeds.
- Research live smoke test succeeds.
- APK assembles successfully and artifact integrity is verified.

## Deployment

Backend code lives in the same repository under a separate directory such as `nexus-backend/` so client and backend protocol changes are versioned together.

Target runtime should support:

- HTTPS service deployment.
- Environment secrets.
- Health checks.
- Logs.
- Simple redeploys from GitHub.

The exact hosting provider is deployment configuration and can be changed without changing the Android API contract.

## Migration plan

1. Add backend protocol models and tests.
2. Implement backend service and adapter interface.
3. Deploy backend and verify `/health` plus one real task.
4. Add Android `NexusBackendClient` behind tests.
5. Switch text/research paths from direct public endpoints to backend.
6. Add structured retry/error UI.
7. Add local fallback for supported tasks.
8. Remove legacy direct anonymous endpoint configuration from Android.
9. Run full regression suite and build the next APK.

## Acceptance criteria

- No ChatGPT/Claude/Gemini/etc. login screen is shown.
- Android APK contains no provider master API keys.
- Direct Vireonix/Animica/Cehpoint calls are removed from the app.
- A single Nexus backend hostname handles cloud AI requests.
- At least one backend provider failure can occur without breaking the user request when another healthy provider is configured.
- Chat history still survives app restart.
- Retry works without duplicating the user's message.
- Generic "internetini kontrol et" is shown only for a genuine no-network condition; other failures are accurately classified.
- Local fallback works for supported text tasks and is explicit about unsupported offline capabilities.
- CI verifies backend + Android before producing the APK.
