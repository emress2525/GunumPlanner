# Jarvis v2 Hybrid Voice Assistant Design

Date: 2026-09-12
Branch: `jarvis-apk-build`
Package: `com.sesliasistan.jarvis`
Baseline: Jarvis v1.1.0

## 1. Goal

Turn the existing Android Jarvis app into a Siri/Gemini/Bixby/Alexa/ChatGPT-style hybrid voice assistant without regressing the working v1.1.0 features. Jarvis v2 must combine fast local device commands with a conversational reasoning layer, while respecting Android 16 restrictions and requiring confirmation for sensitive actions.

The assistant remains Android-first, Turkish-first, and usable from a wake phrase (`Jarvis`) plus an in-app/manual activation path.

## 2. Product Principles

1. **Hybrid execution:** deterministic device commands stay local; open-ended questions are routed to a conversational provider only when one is configured.
2. **No fake AI:** the APK must never claim to have an online model if no real provider/backend is configured.
3. **Safe actions:** calling, messaging, external navigation, and other user-impacting actions use Android intents or explicit confirmation rather than hidden automation.
4. **Graceful degradation:** features that need optional permissions or unavailable hardware must fail safely and explain what is missing.
5. **Regression protection:** every v1.1.0 command remains supported unless Android itself blocks the action.
6. **Android-compliant background behavior:** foreground microphone service and assistant-role integration are used; no claim of OEM-independent always-on hotword reliability.

## 3. Competitive Patterns to Combine

Jarvis v2 intentionally combines patterns rather than copying one assistant:

- **Siri-style personal context:** session context and recent-action continuity.
- **Gemini-style multimodal path:** architecture prepared for camera/screen context, but only enabled where Android permissions and implementation are real.
- **Bixby-style device control:** direct local device/app actions when Android permits them.
- **Alexa-style routines:** named, multi-step routines stored locally and executed sequentially.
- **ChatGPT Voice-style conversation:** multi-turn context, concise spoken responses, interruption-friendly speech flow.
- **Perplexity-style action routing:** open apps, maps, web, messages, phone dialer, and other Android surfaces through intents.

## 4. Existing v1.1.0 Features That Must Remain

- Turkish wake variants for Jarvis.
- Time and date.
- Battery percentage.
- Alarm creation.
- Timer creation.
- Flashlight on/off.
- Volume up/down/mute/max.
- Media play/pause/next/previous.
- Camera launch.
- Dial a numeric phone number.
- Compose SMS to a numeric phone number.
- Navigation to a spoken destination.
- Save and read the last note.
- Wi-Fi, Bluetooth, location, display, sound, and general settings shortcuts.
- Explicit web search.
- Open installed apps, including the current known-app aliases.
- Unknown command fallback to web search when no conversational provider is configured.
- Foreground listening service, Turkish SpeechRecognizer, Turkish TTS, and assistant-role integration.

## 5. v2 Functional Scope

### 5.1 Contact-aware communication

Add optional contact lookup with runtime `READ_CONTACTS` permission.

Examples:
- `Jarvis annemi ara`
- `Jarvis Ahmet'e mesaj yaz toplantıya geliyorum`

Rules:
- If one contact matches, open dialer or SMS composer.
- If multiple contacts match, do not guess; speak/display the alternatives.
- Never auto-send an SMS and never place a call invisibly.
- If contacts permission is denied, keep numeric-number behavior working.

### 5.2 Reminder engine

Add app-owned reminders with local persistence and Android notification delivery.

Examples:
- `10 dakika sonra kaynakçıyla konuşmayı hatırlat`
- `yarın saat 08:00 çizimleri gönder diye hatırlat`

Requirements:
- Parse relative duration and simple absolute date/time expressions.
- Persist reminders so they survive process death/reboot where Android permits.
- Notification tap opens Jarvis.
- Duplicate or malformed reminders must not crash the app.

### 5.3 Routines / multi-step automation

Support locally stored named routines.

Examples:
- `işe gidiyorum rutini`
- `gece modu rutini`

Initial supported routine steps are limited to actions Jarvis can already execute safely: app open, settings page, volume action, media key, timer/alarm, navigation launch, and spoken confirmation.

No unsupported silent Wi-Fi/Bluetooth toggling is promised on modern Android.

### 5.4 Conversation context

Introduce a bounded local conversation session:

- Remember the latest user utterances and Jarvis replies within the active session.
- Support follow-up references for simple deterministic actions, such as destination/app/note continuation.
- Clear session context after inactivity and allow manual clear.
- Do not store sensitive conversation history permanently by default.

### 5.5 Conversational AI provider boundary

Add a provider interface but do not embed a vendor API secret in the APK.

Behavior:
- If no provider is configured, unknown questions continue to safe web-search fallback.
- If a provider is configured later through a secure backend or user-owned credential mechanism, the conversation engine can route open-ended questions there.
- Provider failure falls back cleanly; local commands keep working.

This preserves an honest, working APK with no placeholder backend.

### 5.6 Command confidence and confirmation

Introduce action risk classes:

- **Low risk:** time, date, battery, open app, web search, flashlight, volume, media, settings.
- **Medium risk:** navigation, reminder creation, routine execution.
- **Sensitive:** contact-based call/message preparation and any future data-sharing action.

Sensitive actions require an explicit spoken/display confirmation when the resolved target could be ambiguous or when content is being sent outside Jarvis.

### 5.7 Command history

Store a compact local history of recent successfully parsed commands and outcomes.

- History is visible in the app.
- User can clear it.
- Failed speech-recognition fragments are not permanently stored by default.
- Keep storage bounded.

### 5.8 Improved help and discoverability

The home/help UI must expose capabilities by category:

- Phone & contacts
- Media
- Device
- Navigation
- Reminders
- Notes
- Routines
- Apps
- Search / conversation

The main screen must clearly show current listening state and the most recent recognized command/result.

## 6. Architecture

### 6.1 `CommandRouter`

Keep deterministic parsing pure Java and unit-testable.

Responsibilities:
- normalize Turkish text;
- detect wake phrase and remove it;
- classify commands;
- extract structured parameters;
- return a typed command object.

It must not directly use Android APIs.

### 6.2 `CommandResult` / typed command model

Extend the command enum/model with the minimum additional types required for:

- contact call;
- contact SMS;
- reminder creation/list/cancel as implemented;
- routine execution;
- command-history/help/session commands;
- conversational fallback.

Avoid a single unstructured string for multi-parameter commands where typed fields are needed.

### 6.3 `JarvisListeningService`

Continue to own SpeechRecognizer and TTS lifecycle, but delegate action execution into smaller components rather than growing one service indefinitely.

Target components:
- `DeviceActionExecutor`
- `CommunicationExecutor`
- `ReminderManager`
- `RoutineManager`
- `ConversationSession`
- `CommandHistoryStore`

The service orchestrates recognition -> parsing -> confirmation if required -> execution -> spoken/UI result.

### 6.4 Storage

Use Android local storage only for v2 baseline:

- SharedPreferences or a small JSON-backed store for bounded settings/routines/history if sufficient.
- Reminder metadata persisted locally.
- No cloud account is required.
- No secret API key is hard-coded.

### 6.5 UI

Keep the existing dark Jarvis identity and orb concept, but expand the UI into three clear states:

1. listening/status home;
2. recent activity/history;
3. capabilities/settings/help.

Avoid a dense developer-style control panel.

## 7. Android Permissions and Platform Boundaries

Existing permissions stay unless proven unnecessary. New permission candidates are added only for implemented features.

Expected additions:
- `READ_CONTACTS` for name resolution.
- Notification permission remains required on supported Android versions for reminder delivery.
- A boot receiver may be added only if reminder restoration actually requires it and is implemented/tested.

The app must not request `CALL_PHONE` or `SEND_SMS` merely to bypass user confirmation; v2 should continue using the dialer/SMS composer for safety and platform compatibility.

## 8. Error Handling

Every action returns a user-facing success/failure result.

Examples:
- no camera flash -> `Bu telefonda kullanılabilir flaş bulamadım.`
- contacts denied -> `Kişiler izni olmadan isimden arama yapamam; numarayı söylersen arama ekranını açabilirim.`
- no matching app -> explain and optionally web-search only if that is semantically sensible;
- no navigation handler -> explain rather than crash;
- speech recognizer unavailable -> retain manual UI path and show error;
- TTS unavailable -> visual result still works.

No swallowed exceptions for user-triggered actions.

## 9. Testing Strategy

Verification order follows the user-required release gate.

### 9.1 Static checks

- Android lint.
- Manifest validation.
- No embedded vendor secrets.
- No TODO/placeholder/fake API implementation in release path.

### 9.2 Compile/build

- Java compilation.
- `assembleDebug` APK build.
- APK signature verification.
- package/version/minSdk/targetSdk validation.

### 9.3 Unit tests

Pure-Java tests for:
- all v1.1.0 command parsing (regression);
- contact-name command parsing;
- reminder parsing and invalid dates/durations;
- routine command parsing;
- conversation/session timeout logic where pure-Java;
- wake-word variants;
- malformed/empty input;
- Turkish casing/diacritics.

### 9.4 Integration/behavior tests

Android-side smoke coverage for:
- activity launch;
- service creation where permissions allow;
- intent construction for dial/SMS/maps/alarm/timer/settings;
- reminder persistence and notification scheduling logic where testable in emulator;
- denied optional permission paths.

### 9.5 Android 16 runtime gate

On an Android API 36 emulator:
- install APK;
- verify package path;
- cold-launch `MainActivity`;
- confirm version;
- exercise non-hardware smoke flows that can be automated;
- ensure no startup crash.

### 9.6 Physical-device limitations

The automated release gate cannot prove:
- OEM-specific always-on/background wake-word reliability;
- Bluetooth headset microphone routing on the user's exact device;
- real contact database behavior beyond emulator fixtures;
- real flashlight/camera hardware behavior;
- aggressive Xiaomi/OEM battery-management behavior.

These must be labeled unverified until exercised on physical hardware.

## 10. Release Gate

`NO PASS -> NO DELIVERY`.

A v2 APK is not delivered as verified unless:

1. static/lint pass;
2. compilation/build pass;
3. unit tests pass;
4. regression tests pass;
5. APK metadata/signature pass;
6. Android 16 emulator install + cold launch pass;
7. implemented v2 behaviors covered by available automated tests pass.

Physical-device-only behaviors are reported separately rather than falsely marked as passed.

## 11. Versioning

v2 implementation will increment versionCode and use a new versionName (planned `2.0.0`) only when the feature set above is actually implemented and passes the release gate.

## 12. Explicit Non-Goals for This Release

To avoid fake or unsafe claims, v2.0 will not promise these unless a real implementation and validation path is added during development:

- OEM-independent low-power DSP hotword detection while the process is fully dead;
- hidden direct calling or automatic SMS sending;
- bypassing Android restrictions to silently toggle protected settings;
- a built-in cloud LLM with a hard-coded third-party API key;
- unrestricted autonomous screen control/accessibility automation;
- cloud sync/account features.

These can be separate future subsystems with their own security and test design.