# Namaz V8 Foundation + Prayer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first production-ready Namaz V8 slice as a native Kotlin/Jetpack Compose Android app containing onboarding, five-center navigation shell, Today dashboard, canonical prayer-time domain model, prayer tracking, per-prayer alert settings, exact-alarm scheduling, adhan playback, congregation mode controls, travel-aware city update prompts, qibla entry point, and Notification Health Center, with a verified debug/release APK.

**Architecture:** Create a new `v8app` Android application module so V7 remains untouched and installable. Use a single `PrayerRepository` as the canonical source for app UI, future widgets/lock status/Wear; DataStore for settings and lightweight tracking, Room only when history tables are required, WorkManager for refresh jobs, AlarmManager for prayer alerts, Media3 for adhan playback, Compose Navigation for module-local back stacks. The first milestone must work offline with cached/manual city data and degrade clearly when permissions or exact alarms are unavailable.

**Tech Stack:** Kotlin, Android Gradle Plugin 8.7.3, Java 17, Jetpack Compose Material 3, Navigation Compose, DataStore Preferences, Room, WorkManager, AlarmManager, Media3, kotlinx-coroutines, JUnit, AndroidX test.

**Spec:** `docs/superpowers/specs/2026-09-13-namaz-v8-islamic-life-platform-design.md`

## Global Constraints

- Preserve V7 files and V7 build path; V8 is additive until final acceptance.
- V8 package/application ID: `app.namaz.tr.v8`.
- V8 display name: `Namaz V8`.
- minSdk 26, targetSdk 35, compileSdk 35, Java/Kotlin toolchain 17.
- No mandatory account, no ad SDK, no analytics SDK.
- Default religious narrative is Hanafi; Shafii differences are separately labeled.
- Prayer calculation source/method and manual offsets must be visible and stored.
- The app must never silently decide that a user is legally a traveler; it only explains criteria and asks before changing city prayer times.
- Exact alarm, notification, DND and OEM restrictions must be surfaced in Notification Health Center instead of being hidden.
- No implementation in this milestone may depend on WebView/HTML patching.
- All new behavior follows TDD: failing test -> minimal implementation -> passing test -> commit.
- No TODO/TBD/placeholder content in user-visible paths.

---

## File Structure Locked for This Milestone

- `settings.gradle` — add `:v8app` without removing existing modules.
- `build.gradle` — add Kotlin/Compose plugin declarations while preserving existing Android plugin.
- `v8app/build.gradle` — V8 application dependencies and build config.
- `v8app/src/main/AndroidManifest.xml` — V8 permissions, launcher activity, alarm/boot/service receivers.
- `v8app/src/main/java/app/namaz/tr/v8/MainActivity.kt` — Compose host only.
- `v8app/src/main/java/app/namaz/tr/v8/app/NamazV8App.kt` — root app/navigation/theme shell.
- `v8app/src/main/java/app/namaz/tr/v8/app/AppDestination.kt` — five top-level destinations and routes.
- `v8app/src/main/java/app/namaz/tr/v8/onboarding/*` — first-launch profile + simple/full mode.
- `v8app/src/main/java/app/namaz/tr/v8/prayer/domain/*` — canonical prayer domain types/calculation contracts.
- `v8app/src/main/java/app/namaz/tr/v8/prayer/data/*` — settings/cache/repository implementations.
- `v8app/src/main/java/app/namaz/tr/v8/prayer/alarm/*` — AlarmManager scheduling, receivers, boot reschedule.
- `v8app/src/main/java/app/namaz/tr/v8/prayer/adhan/*` — Media3 foreground adhan service and actions.
- `v8app/src/main/java/app/namaz/tr/v8/prayer/ui/*` — Today, Prayer detail, settings and tracker Compose screens.
- `v8app/src/main/java/app/namaz/tr/v8/health/*` — notification/exact alarm/battery/DND diagnostics.
- `v8app/src/main/java/app/namaz/tr/v8/qibla/*` — qibla launch screen and capability state stub backed by native sensors in the same module.
- `v8app/src/main/res/*` — V8 strings, icons, notification channel labels.
- `v8app/src/test/...` — pure JVM tests for domain/repository/scheduling logic.
- `v8app/src/androidTest/...` — Compose navigation and permission-state UI tests.
- `.github/workflows/namaz-v8-build.yml` — isolated V8 CI build/test/artifact verification.

---

### Task 1: Create the isolated native V8 module and CI skeleton

**Files:**
- Modify: `settings.gradle`
- Modify: `build.gradle`
- Create: `v8app/build.gradle`
- Create: `v8app/src/main/AndroidManifest.xml`
- Create: `v8app/src/main/java/app/namaz/tr/v8/MainActivity.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/app/NamazV8App.kt`
- Create: `v8app/src/main/res/values/strings.xml`
- Create: `.github/workflows/namaz-v8-build.yml`
- Test: `v8app/src/test/java/app/namaz/tr/v8/BuildContractTest.kt`

**Interfaces:**
- Produces: installable `app.namaz.tr.v8`, versionCode 8, versionName `8.0.0-dev1`, app label `Namaz V8`.

- [ ] **Step 1: Write failing build-contract test** asserting package constants/version-label resource contract.
- [ ] **Step 2: Run** `./gradlew :v8app:testDebugUnitTest` and confirm it fails because `v8app` does not exist.
- [ ] **Step 3: Add Kotlin Android + Compose plugin declarations and include `:v8app`.** Keep existing `:app` untouched.
- [ ] **Step 4: Create `v8app/build.gradle`** with min/target/compile SDK 26/35/35, Java/Kotlin 17, Compose enabled, test dependencies, applicationId `app.namaz.tr.v8`, versionCode 8, versionName `8.0.0-dev1`.
- [ ] **Step 5: Add minimal Compose launcher** rendering `Namaz V8` and no WebView.
- [ ] **Step 6: Add CI workflow** running unit tests, lint, `assembleDebug`, `assembleRelease`, then verifying APK paths and package metadata with `aapt dump badging`.
- [ ] **Step 7: Run** `./gradlew :v8app:testDebugUnitTest :v8app:assembleDebug` and require PASS.
- [ ] **Step 8: Commit** `feat: create native Namaz V8 app foundation`.

### Task 2: Add onboarding, Simple/Full mode and five-center navigation

**Files:**
- Create: `v8app/src/main/java/app/namaz/tr/v8/app/AppDestination.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/app/AppMode.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/onboarding/UserProfile.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/onboarding/OnboardingRepository.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/onboarding/OnboardingScreen.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/app/MainScaffold.kt`
- Test: `v8app/src/test/java/app/namaz/tr/v8/onboarding/OnboardingRepositoryTest.kt`
- Test: `v8app/src/androidTest/java/app/namaz/tr/v8/app/NavigationTest.kt`

**Interfaces:**
- Produces: `enum class UserProfile { BEGINNER_RELIGION, NEW_TO_PRAYER, LEARN_QURAN, DAILY_TRACKING }`
- Produces: `enum class AppMode { SIMPLE, FULL }`
- Produces top-level routes: `today`, `quran`, `learn`, `worship`, `more`.

- [ ] **Step 1:** Write repository test: first launch has no profile, saving profile/mode persists, mode can change without changing profile.
- [ ] **Step 2:** Run test and require FAIL.
- [ ] **Step 3:** Implement DataStore-backed onboarding repository.
- [ ] **Step 4:** Write Compose navigation test verifying five destinations exist in FULL mode and SIMPLE mode keeps core destinations visible without deleting access to full features.
- [ ] **Step 5:** Run UI test and require FAIL.
- [ ] **Step 6:** Implement onboarding screen and `MainScaffold` with Material 3 bottom navigation and module-local back behavior.
- [ ] **Step 7:** Run unit + UI tests and require PASS.
- [ ] **Step 8:** Commit `feat: add V8 onboarding and main navigation`.

### Task 3: Build the canonical prayer domain model and repository

**Files:**
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/domain/PrayerName.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/domain/PrayerTime.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/domain/DailyPrayerSchedule.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/domain/PrayerSettings.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/domain/CalculationMethod.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/domain/AsrSchool.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/data/PrayerRepository.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/data/PrayerRepositoryImpl.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/data/PrayerSettingsStore.kt`
- Test: `v8app/src/test/java/app/namaz/tr/v8/prayer/PrayerRepositoryTest.kt`

**Interfaces:**
- `suspend fun PrayerRepository.schedule(date: LocalDate): DailyPrayerSchedule`
- `fun PrayerRepository.observeToday(): Flow<DailyPrayerSchedule>`
- `fun PrayerRepository.observeSettings(): Flow<PrayerSettings>`
- `suspend fun PrayerRepository.updateSettings(settings: PrayerSettings)`
- `fun DailyPrayerSchedule.nextPrayer(now: ZonedDateTime): PrayerTime?`
- Schedule includes FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA; SUNRISE is never trackable as a prayer.

- [ ] **Step 1:** Write tests for chronological ordering, next-prayer selection, after-Isha rollover, Sunrise excluded from tracker, manual minute offsets, Hanafi/Shafii Asr setting persistence.
- [ ] **Step 2:** Run tests and require FAIL.
- [ ] **Step 3:** Implement domain models and deterministic repository with a local cached schedule source contract. For this milestone, cached/manual city schedule is authoritative; network provider remains an interface, not a fake API.
- [ ] **Step 4:** Implement DataStore settings for city, country, calculation method, Asr school and per-prayer minute offsets.
- [ ] **Step 5:** Run tests and require PASS.
- [ ] **Step 6:** Commit `feat: add canonical prayer schedule repository`.

### Task 4: Implement Today dashboard and prayer tracking

**Files:**
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/data/PrayerTrackingStore.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/domain/PrayerTrackingStatus.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/ui/TodayViewModel.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/ui/TodayScreen.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/ui/PrayerTimesStrip.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/ui/PrayerTrackerCard.kt`
- Test: `v8app/src/test/java/app/namaz/tr/v8/prayer/TodayViewModelTest.kt`
- Test: `v8app/src/androidTest/java/app/namaz/tr/v8/prayer/TodayScreenTest.kt`

**Interfaces:**
- Simple status: `PRAYED`, `NOT_SET`.
- Optional detailed statuses: `ON_TIME`, `CONGREGATION`, `MAKEUP`.
- Tracking is user-entered only; no automatic debt calculation.

- [ ] **Step 1:** Write ViewModel tests verifying hero card uses canonical next prayer, countdown updates, five prayer tracker rows exclude Sunrise, tracking survives reload.
- [ ] **Step 2:** Run and require FAIL.
- [ ] **Step 3:** Implement tracking store + ViewModel.
- [ ] **Step 4:** Write Compose test verifying hierarchy: next prayer hero -> six-time strip with Sunrise informational -> tracker -> Today cards; no dense all-in-one card.
- [ ] **Step 5:** Implement polished Material 3 Today screen with emerald/cream tokens, large-text-safe layout and accessibility labels.
- [ ] **Step 6:** Run unit + UI tests and require PASS.
- [ ] **Step 7:** Commit `feat: build V8 Today prayer dashboard`.

### Task 5: Implement per-prayer alert configuration and alarm scheduling

**Files:**
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/domain/AlertMode.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/domain/PrayerAlertConfig.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/alarm/PrayerAlarmScheduler.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/alarm/AndroidPrayerAlarmScheduler.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/alarm/PrayerAlarmReceiver.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/alarm/BootReceiver.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/ui/PrayerAlertSettingsScreen.kt`
- Modify: `v8app/src/main/AndroidManifest.xml`
- Test: `v8app/src/test/java/app/namaz/tr/v8/prayer/PrayerAlarmPlannerTest.kt`

**Interfaces:**
- `enum class AlertMode { OFF, NOTIFICATION, SHORT_SOUND, FULL_ADHAN }`
- Per prayer: mode + optional pre-alert minutes from `{5,10,15,30}`.
- `suspend fun PrayerAlarmScheduler.reschedule(schedule: DailyPrayerSchedule, settings: PrayerSettings)`.

- [ ] **Step 1:** Write planner tests for one alarm per enabled prayer, pre-alert timing, disabled prayers, manual offsets, next-day scheduling, unique request codes.
- [ ] **Step 2:** Run and require FAIL.
- [ ] **Step 3:** Implement pure planner then Android AlarmManager adapter using exact alarm only when allowed; otherwise schedule best-effort and expose degraded state.
- [ ] **Step 4:** Add boot/timezone/time-change rescheduling receiver paths.
- [ ] **Step 5:** Add settings UI for each prayer independently.
- [ ] **Step 6:** Run tests and require PASS.
- [ ] **Step 7:** Commit `feat: add reliable per-prayer alarm scheduling`.

### Task 6: Implement foreground adhan playback and actions

**Files:**
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/adhan/AdhanService.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/adhan/AdhanNotificationFactory.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/adhan/AdhanActionReceiver.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/adhan/AdhanController.kt`
- Modify: `v8app/src/main/AndroidManifest.xml`
- Test: `v8app/src/test/java/app/namaz/tr/v8/prayer/adhan/AdhanControllerTest.kt`

**Interfaces:**
- Actions: `STOP`, `REMIND_5_MIN`, `OPEN_SETTINGS`.
- Full adhan uses foreground service + Media3; short sound uses notification sound path; notification mode never starts audio service.

- [ ] **Step 1:** Write controller tests mapping alert modes to actions and verifying stop/remind behavior.
- [ ] **Step 2:** Run and require FAIL.
- [ ] **Step 3:** Implement Media3 foreground service and action receiver with notification channel separation.
- [ ] **Step 4:** Implement 5-minute reminder by scheduling a one-shot reminder alarm, not restarting the whole prayer schedule.
- [ ] **Step 5:** Run tests and require PASS.
- [ ] **Step 6:** Commit `feat: add V8 adhan playback service`.

### Task 7: Add Notification Health Center and diagnostic tests

**Files:**
- Create: `v8app/src/main/java/app/namaz/tr/v8/health/HealthCheck.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/health/HealthStatus.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/health/NotificationHealthRepository.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/health/NotificationHealthScreen.kt`
- Test: `v8app/src/test/java/app/namaz/tr/v8/health/NotificationHealthRepositoryTest.kt`
- Test: `v8app/src/androidTest/java/app/namaz/tr/v8/health/NotificationHealthScreenTest.kt`

**Interfaces:**
- Checks: notifications, exact alarms, battery optimization, DND access, background restriction; OEM auto-start appears as guided status when no standard API exists.
- Status: `OK`, `WARNING`, `BLOCKED`, `UNKNOWN`.

- [ ] **Step 1:** Write tests converting platform capability inputs to statuses and user guidance.
- [ ] **Step 2:** Run and require FAIL.
- [ ] **Step 3:** Implement repository with Android platform checks and explicit `UNKNOWN` for non-queryable OEM behavior.
- [ ] **Step 4:** Implement green/yellow/red health screen plus `Test notification` and `1 minute test adhan` actions.
- [ ] **Step 5:** Run unit/UI tests and require PASS.
- [ ] **Step 6:** Commit `feat: add notification health diagnostics`.

### Task 8: Add congregation-mode controls and travel/city-change behavior

**Files:**
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/domain/CongregationMode.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/data/TravelStateRepository.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/ui/CongregationModeScreen.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/prayer/ui/TravelPrompt.kt`
- Test: `v8app/src/test/java/app/namaz/tr/v8/prayer/TravelStateRepositoryTest.kt`

**Interfaces:**
- City change only produces `suggestedCity`; user confirmation changes prayer city.
- Congregation mode stores previous audio/DND state before mutation and restores only values it changed.

- [ ] **Step 1:** Write tests proving city is never silently changed and congregation restore is idempotent.
- [ ] **Step 2:** Run and require FAIL.
- [ ] **Step 3:** Implement repository/state machine and permission-aware UI.
- [ ] **Step 4:** Run tests and require PASS.
- [ ] **Step 5:** Commit `feat: add congregation and travel safeguards`.

### Task 9: Add qibla entry point and native sensor reliability state

**Files:**
- Create: `v8app/src/main/java/app/namaz/tr/v8/qibla/QiblaRepository.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/qibla/QiblaScreen.kt`
- Create: `v8app/src/main/java/app/namaz/tr/v8/qibla/QiblaMath.kt`
- Test: `v8app/src/test/java/app/namaz/tr/v8/qibla/QiblaMathTest.kt`

**Interfaces:**
- Qibla bearing calculation is deterministic from latitude/longitude.
- Sensor state: available/unavailable + accuracy LOW/MEDIUM/HIGH.
- Low accuracy must show calibration guidance and never label itself definitive.

- [ ] **Step 1:** Write bearing tests using known coordinate fixtures and sensor-state presentation tests.
- [ ] **Step 2:** Run and require FAIL.
- [ ] **Step 3:** Implement native sensor-backed screen and calibration guidance.
- [ ] **Step 4:** Run tests and require PASS.
- [ ] **Step 5:** Commit `feat: add native V8 qibla guidance`.

### Task 10: Full milestone regression, APK verification and artifact publication

**Files:**
- Modify: `.github/workflows/namaz-v8-build.yml`
- Create: `v8app/src/test/java/app/namaz/tr/v8/RegressionContractTest.kt`
- Create: `docs/superpowers/plans/2026-09-13-namaz-v8-quran.md` only after this milestone passes and the Quran execution begins.

**Interfaces:**
- Final milestone artifact: `Namaz-V8-Foundation.apk`.

- [ ] **Step 1:** Add regression tests covering top-level navigation, next-prayer canonical source, alarm request uniqueness, no Sunrise tracker, persisted profile/mode, persisted manual offsets.
- [ ] **Step 2:** Run full local verification: `./gradlew :v8app:testDebugUnitTest :v8app:lintDebug :v8app:assembleDebug :v8app:assembleRelease`.
- [ ] **Step 3:** Verify APK package/id/version/label with Android build tools; fail CI if package is not `app.namaz.tr.v8` or versionCode is not 8.
- [ ] **Step 4:** Verify manifest contains boot receiver, prayer alarm receiver and adhan service and does not request unrelated permissions.
- [ ] **Step 5:** Verify `v8app` source contains no `WebView` and no ad/analytics SDK dependency.
- [ ] **Step 6:** Upload the verified APK artifact from CI.
- [ ] **Step 7:** Commit `test: verify Namaz V8 prayer foundation release`.

---

## Follow-on Implementation Plans

After this milestone is green, continue in this order, each with its own TDD plan and working APK checkpoint:

1. `2026-09-13-namaz-v8-quran.md` — Quran Pro, mushaf/ayah reader, meal/tafsir provenance, audio, offline packs, search, bookmarks, hatim, tajwid and memorization assistant.
2. `2026-09-13-namaz-v8-learning.md` — Learning Academy, Elif-Ba, prayer academy, ilmihal, glossary, scenarios, spaced review.
3. `2026-09-13-namaz-v8-worship-life.md` — Dua, dhikr, Esma, hadith, source-grounded knowledge center, Ramadan, Hajj/Umrah, women-specific private tracking, calendar and mosque discovery.
4. `2026-09-13-namaz-v8-widgets-wear.md` — V7-compatible widget suite rebuilt from canonical repository, Widget Studio, lock-screen notification fallback, Wear OS companion surface.
5. `2026-09-13-namaz-v8-release-quality.md` — backup/import, accessibility, large-text/elder mode, performance, content provenance audit, complete regression matrix, signed production APK acceptance.

## Self-Review Result

- Spec coverage for the first executable milestone: covered.
- No TODO/TBD/placeholder steps: confirmed.
- Canonical prayer interfaces are named once and reused consistently.
- The full V8 spec is intentionally split into six execution plans because Quran, learning/content, worship/lifestyle, widget/Wear and final-release quality are independent subsystems and each must remain reviewable and testable.
