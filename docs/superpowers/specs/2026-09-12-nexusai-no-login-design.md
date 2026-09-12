# Nexus AI No-Login Design

## Goal
Nexus AI must behave as its own Android AI application. It must not open ChatGPT, Claude, Gemini, Grok, Perplexity, DeepSeek, ElevenLabs, Suno, Puter, or any other provider login page. The user writes one request inside Nexus AI and receives the result inside Nexus AI.

## Product behavior
- No provider sign-in screens.
- No user-entered API keys.
- One native Nexus AI interface for chat/code/research/image/video/audio modes.
- Text/code/research requests use anonymous, keyless public LLM endpoints with automatic failover.
- Image and video requests use anonymous public generation endpoints only when their documented anonymous endpoint is available; failures are surfaced cleanly and never disguised as successful output.
- Audio reading uses Android TextToSpeech locally so it works without provider login.
- The app periodically downloads a small public JSON configuration from GitHub. This lets endpoint ordering, labels, and enabled capabilities change without rebuilding the APK.
- A bundled default configuration is always available so the app still launches if remote configuration is unreachable.

## Architecture
1. `AiEngine` is the app-facing interface. It receives a `NexusRequest` and returns a `NexusResult`.
2. `TextGateway` implements OpenAI-compatible/keyless HTTP calls and ordered failover across free endpoints.
3. `MediaGateway` handles image/video HTTP generation where anonymous public endpoints are configured.
4. `RemoteConfigRepository` downloads runtime provider configuration from a public GitHub raw URL, validates HTTPS URLs, caches it locally, and falls back to the bundled defaults.
5. `MainActivity` is a native single-screen chat/studio UI. It never navigates to provider websites.
6. `ResultRenderer` displays text, generated image/video URLs, errors, and copy/share actions inside the app.
7. Android `TextToSpeech` provides local spoken output.

## Default text failover
The bundled config uses keyless endpoints that were publicly documented in September 2026, with a strict timeout and failover. Runtime configuration can disable or reorder them if availability changes.

## Security and privacy
- HTTPS only; cleartext HTTP is blocked.
- No secret provider keys are embedded in the APK.
- Prompts are sent only to the currently selected anonymous inference endpoint for requests that require cloud generation.
- The app shows a short privacy note explaining that cloud prompts leave the device.
- Local TTS never requires an external provider account.

## Updating
At startup the app tries to refresh `nexus-config.json` from the public repository. A valid config replaces only the endpoint metadata; executable code is never downloaded or executed. This keeps routing current without creating a remote-code-execution path.

## Verification
- Unit tests for request-mode classification, config parsing/validation, failover order, response parsing, and URL safety.
- Android lint must pass.
- Debug APK must assemble successfully.
- A CI network smoke test checks at least one configured text endpoint before publishing the artifact; if all keyless text endpoints are unavailable, the build must not be presented as a working AI build.
