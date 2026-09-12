# Jarvis v2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the verified Jarvis v1.1 Android app to a broader v2 voice assistant with contact-aware communication, reminders, routines, command history, and safe multi-step command execution without regressing any v1.1 behavior.

**Architecture:** Keep pure command parsing in `core`, add a deterministic `CommandPlanner` for sequential phrases, extend `CommandResult`, and implement Android-specific execution in `JarvisListeningService` plus a dedicated `ReminderReceiver`. Persist bounded local state in SharedPreferences and keep external side-effect actions confirmable through Android system UI.

**Tech Stack:** Java, Android SDK 36, minSdk 29, JUnit4, GitHub Actions Android emulator API 36.

**Spec:** `docs/superpowers/specs/2026-09-12-jarvis-v2-design.md`

## Global Constraints
- Keep package `com.sesliasistan.jarvis`.
- Keep compileSdk/targetSdk 36 and minSdk 29.
- Preserve every existing v1.1 tested command.
- Do not embed API secrets or fake an online AI backend.
- Calls use the dialer; SMS uses the composer; calendar uses system insertion UI.
- No release until tests, lint, build, signature/metadata and Android 16 runtime smoke all pass.

---

### Task 1: Expand pure command model and parser with TDD

**Files:**
- Modify: `jarvis-build/app/src/test/java/com/sesliasistan/jarvis/core/CommandRouterTest.java`
- Modify: `jarvis-build/app/src/main/java/com/sesliasistan/jarvis/core/CommandResult.java`
- Modify: `jarvis-build/app/src/main/java/com/sesliasistan/jarvis/core/CommandRouter.java`

**Produces:** contact-call/contact-SMS/reminder/calendar/routine/history typed commands.

- [ ] Add failing tests for contact call, contact SMS, relative reminder, calendar insert, routine save/run/list/delete, history read/clear.
- [ ] Run CI/unit tests and confirm RED because new command types/behavior do not exist.
- [ ] Add the minimal enum payloads and parsing required.
- [ ] Run the full unit suite and confirm GREEN with all v1.1 tests retained.

### Task 2: Add deterministic multi-step planner with TDD

**Files:**
- Create: `jarvis-build/app/src/main/java/com/sesliasistan/jarvis/core/CommandPlanner.java`
- Create: `jarvis-build/app/src/test/java/com/sesliasistan/jarvis/core/CommandPlannerTest.java`

**Interface:** `public static List<CommandResult> plan(String rawCommand)`.

- [ ] Write failing tests for `spotify aç sonra sesi yükselt`, `ardından`, maximum 8 actions, and routine-definition preservation.
- [ ] Confirm RED in CI.
- [ ] Implement safe splitting and route each fragment through `CommandRouter`.
- [ ] Confirm planner tests and all router regression tests pass.

### Task 3: Implement Android reminder delivery and permission-safe contact actions

**Files:**
- Create: `jarvis-build/app/src/main/java/com/sesliasistan/jarvis/ReminderReceiver.java`
- Modify: `jarvis-build/app/src/main/AndroidManifest.xml`
- Modify: `jarvis-build/app/src/main/java/com/sesliasistan/jarvis/JarvisListeningService.java`
- Modify: `jarvis-build/app/src/main/java/com/sesliasistan/jarvis/MainActivity.java`

**Behavior:** optional READ_CONTACTS, contact resolver, ACTION_DIAL/ACTION_SENDTO handoff, local notification reminders, ACTION_INSERT calendar event.

- [ ] Add READ_CONTACTS permission and non-exported reminder receiver.
- [ ] Request contacts permission as optional so denying it does not block Jarvis startup.
- [ ] Resolve contact names only when permission exists; never crash on denial/empty contacts.
- [ ] Schedule reminders through AlarmManager with platform-safe fallback and post notification through `ReminderReceiver`.
- [ ] Open calendar event insertion using CalendarContract.
- [ ] Route service execution through the new commands and planner.

### Task 4: Implement local routines and bounded command history

**Files:**
- Modify: `jarvis-build/app/src/main/java/com/sesliasistan/jarvis/JarvisListeningService.java`

**Behavior:** save/run/list/delete routines in SharedPreferences; maximum nested routine depth 3; store bounded recent command history and support read/clear.

- [ ] Add routine persistence keyed by normalized routine name.
- [ ] Execute routine raw command through `CommandPlanner` with nesting guard.
- [ ] Keep last 20 command entries and expose spoken read/clear behavior.
- [ ] Ensure routine/history storage failures degrade to spoken errors rather than crashes.

### Task 5: Polish v2 UI/version/help without regressing startup

**Files:**
- Modify: `jarvis-build/app/build.gradle`
- Modify: `jarvis-build/app/src/main/java/com/sesliasistan/jarvis/MainActivity.java`
- Modify: `jarvis-build/app/src/main/res/values/strings.xml`

- [ ] Increment to versionCode 3 / versionName 2.0.0 only after implemented paths compile.
- [ ] Update capability/help text to include contacts, reminders, calendar, routines, history and multi-step commands.
- [ ] Keep the existing dark Jarvis identity and start/stop behavior.

### Task 6: Full verification and release artifact

**Files:**
- Modify only CI workflow if a verification script defect is found; do not weaken checks.

- [ ] Run static/lint.
- [ ] Run full unit suite and regression suite.
- [ ] Assemble debug APK.
- [ ] Verify APK signature and package/version/min/target metadata.
- [ ] Install on Android API 36 emulator and cold-launch `MainActivity`.
- [ ] Verify package component registration for `ReminderReceiver` and listening service.
- [ ] Download the verified artifact and SHA-256 check it locally.
- [ ] Deliver APK only when every automatable gate passes; explicitly label physical-phone wake/Bluetooth/OEM behavior as unverified.
