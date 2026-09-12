# Nexus AI No-Login Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace provider web/login flows with a native Nexus AI experience that answers through keyless anonymous endpoints, supports media modes where anonymous APIs exist, speaks results locally, and refreshes provider configuration remotely.

**Architecture:** Native Java Android app. `RequestClassifier` selects a mode, `RemoteConfigRepository` provides validated endpoint metadata, `TextGateway` performs ordered failover, `MediaGateway` handles anonymous media endpoints, and `MainActivity` renders all results in-app. No provider WebView remains.

**Tech Stack:** Android SDK 35, Java 17, `HttpURLConnection`, `org.json`, Android `TextToSpeech`, JUnit 4, GitHub Actions/Gradle 8.9.

**Spec:** `docs/superpowers/specs/2026-09-12-nexusai-no-login-design.md`

## Global Constraints
- Minimum Android API remains 26.
- No provider login pages or WebViews.
- No secret API keys embedded in the APK.
- Network generation endpoints must use HTTPS.
- Remote config may change data only; no downloaded executable code.
- A bundled fallback config is mandatory.

---

### Task 1: Request classification

**Files:**
- Create: `aihub/src/main/java/com/emre/nexusai/RequestMode.java`
- Create: `aihub/src/main/java/com/emre/nexusai/RequestClassifier.java`
- Test: `aihub/src/test/java/com/emre/nexusai/RequestClassifierTest.java`

**Interfaces:**
- Produces: `RequestMode classify(String prompt)`.

- [ ] Write tests covering CHAT, CODE, RESEARCH, IMAGE, VIDEO, AUDIO.
- [ ] Run `gradle :aihub:testDebugUnitTest` and confirm the classifier tests fail because the new types do not exist.
- [ ] Implement keyword-scored classification with VIDEO checked before IMAGE and CODE before CHAT.
- [ ] Re-run tests and confirm pass.

### Task 2: Runtime config validation

**Files:**
- Create: `aihub/src/main/java/com/emre/nexusai/NexusConfig.java`
- Create: `aihub/src/main/java/com/emre/nexusai/ConfigParser.java`
- Create: `aihub/src/main/assets/nexus-config.json`
- Test: `aihub/src/test/java/com/emre/nexusai/ConfigParserTest.java`

**Interfaces:**
- Produces: `NexusConfig parse(String json)` containing ordered text endpoints and optional media endpoints.

- [ ] Test valid HTTPS config, reject cleartext URLs, reject empty text endpoint list, and preserve endpoint order.
- [ ] Verify tests fail before parser implementation.
- [ ] Implement immutable config objects and strict JSON validation.
- [ ] Verify tests pass.

### Task 3: Text response parsing and failover policy

**Files:**
- Create: `aihub/src/main/java/com/emre/nexusai/TextResponseParser.java`
- Create: `aihub/src/main/java/com/emre/nexusai/FailoverPolicy.java`
- Test: `aihub/src/test/java/com/emre/nexusai/TextResponseParserTest.java`
- Test: `aihub/src/test/java/com/emre/nexusai/FailoverPolicyTest.java`

**Interfaces:**
- Produces: `String parseOpenAiResponse(String json)` and ordered attempt selection.

- [ ] Test OpenAI-compatible `choices[0].message.content`, malformed responses, and deterministic endpoint ordering.
- [ ] Verify RED.
- [ ] Implement parser/policy.
- [ ] Verify GREEN.

### Task 4: Native gateways and remote config

**Files:**
- Create: `aihub/src/main/java/com/emre/nexusai/TextGateway.java`
- Create: `aihub/src/main/java/com/emre/nexusai/MediaGateway.java`
- Create: `aihub/src/main/java/com/emre/nexusai/RemoteConfigRepository.java`
- Create: `aihub/src/main/java/com/emre/nexusai/NexusResult.java`
- Create: `aihub/src/main/java/com/emre/nexusai/NexusAiEngine.java`

**Interfaces:**
- `NexusAiEngine.execute(String prompt, RequestMode mode, Callback callback)`.
- Text gateway sends OpenAI-compatible JSON to keyless HTTPS endpoints, applies timeouts, and fails over on network/HTTP/parse failure.
- Remote config fetches public JSON, caches valid config, otherwise returns bundled asset.

- [ ] Add focused tests for pure URL/config helper behavior before production implementation.
- [ ] Implement gateways with background threads; never block UI thread.
- [ ] Enforce HTTPS and response-size limits.
- [ ] Preserve actionable error messages when all endpoints fail.

### Task 5: Replace provider UI

**Files:**
- Modify: `aihub/src/main/java/com/emre/nexusai/MainActivity.java`
- Delete: `aihub/src/main/java/com/emre/nexusai/ProviderWebActivity.java`
- Delete: `aihub/src/main/java/com/emre/nexusai/ProviderRegistry.java`
- Modify: `aihub/src/main/AndroidManifest.xml`

**Interfaces:**
- Main screen keeps one prompt box, mode chips, send button, conversation/result area, copy/share/speak actions.

- [ ] Remove every provider brand card and navigation action.
- [ ] Wire send button to `NexusAiEngine`.
- [ ] Render text responses inside Nexus AI.
- [ ] Render image/video links/previews where media gateway returns a URL.
- [ ] Add local Android TextToSpeech for text results.
- [ ] Add privacy note: cloud prompts are sent to anonymous public inference endpoints.

### Task 6: Remote update source and CI verification

**Files:**
- Create: `nexus-runtime/nexus-config.json`
- Modify: `.github/workflows/build-aihub.yml`

**Interfaces:**
- App remote URL: raw GitHub URL for `nexus-runtime/nexus-config.json` on `ai-nexus-apk`.

- [ ] Add runtime config matching bundled defaults.
- [ ] Add CI unit tests, lint, and APK build.
- [ ] Add a bounded network smoke test for configured text endpoints; at least one must answer successfully before artifact upload.
- [ ] Upload `NexusAI-no-login-debug.apk` only after all checks pass.

### Task 7: Final regression verification

- [ ] Run full `gradle :aihub:testDebugUnitTest`.
- [ ] Run `gradle :aihub:lintDebug`.
- [ ] Run `gradle :aihub:assembleDebug`.
- [ ] Confirm manifest has no `ProviderWebActivity` and source contains no provider-login/WebView flow.
- [ ] Download the CI artifact and deliver the APK.
