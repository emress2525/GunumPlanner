# Namaz Native Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a single installable Namaz APK that fixes bottom-nav overlap, uses native qibla sensors, supports Quran audio controls, background prayer notifications/adhan, an Android widget, and sourced Islamic social-life guidance.

**Architecture:** Keep the existing Quran/meal HTML UI as the content layer but move device-dependent capabilities behind a native Android Java bridge. Build a separate `namaz-native` Android project so the unrelated planner app in this repository is untouched. Native receivers/services handle alarms, adhan playback, widget updates and reboot rescheduling.

**Tech Stack:** Android Gradle Plugin 8.7.3, Gradle 8.9, Java 17, Android SDK 35, minSdk 26, WebView, SensorManager, LocationManager, AlarmManager, MediaPlayer/TextToSpeech, AppWidgetProvider, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-09-13-namaz-native-upgrade-design.md`

## Global Constraints
- No forced account, ads or analytics SDK.
- Personal tracking/settings remain local on device.
- Kaaba coordinates are 21.4225, 39.8262.
- Religious/social-life content must display Quran/hadith source metadata and must not flatten disputed rulings into universal certainty.
- Enabled prayer alarms must respect Android notification/exact-alarm/DND policies.
- One Gradle build produces the APK and the APK must pass `apksigner verify`.

---

### Task 1: Project shell and regression tests

**Files:**
- Create: `namaz-native/settings.gradle`
- Create: `namaz-native/build.gradle`
- Create: `namaz-native/gradle.properties`
- Create: `namaz-native/app/build.gradle`
- Create: `namaz-native/app/src/test/java/app/namaz/tr/QiblaMathTest.java`
- Create: `namaz-native/app/src/test/java/app/namaz/tr/PrayerLogicTest.java`

**Interfaces:**
- Produces: `QiblaMath.bearing(double lat,double lon): double`, `PrayerLogic.nextPrayer(Map<String,String>, LocalDateTime): PrayerEvent`.

- [ ] Write failing JUnit tests for Ankara qibla bearing being in a realistic range and next-prayer selection across midnight.
- [ ] Run `gradle :app:testDebugUnitTest`; confirm tests fail because implementation classes do not exist.
- [ ] Add minimal project configuration and source classes.
- [ ] Re-run tests and confirm PASS.
- [ ] Commit project shell and tests.

### Task 2: Web content migration and overlap fix

**Files:**
- Create: `namaz-native/app/src/main/assets/index.html`
- Create: `namaz-native/app/src/main/assets/app.js`
- Create: `namaz-native/app/src/main/assets/core.js`

**Interfaces:**
- Consumes: native object exposed as `window.AndroidBridge`.
- Produces: `window.onNativeQibla(heading,bearing,accuracy)`, `window.onNativeAudioState(json)`, and settings calls to native bridge.

- [ ] Copy the current working Quran/meal assets from the last APK into the new project.
- [ ] Add bottom safe-area/content padding so reader text, snackbars and mini-player remain above the fixed navigation.
- [ ] Replace Web Speech/device-orientation capability checks with native bridge calls when the bridge exists, preserving browser fallback only for development.
- [ ] Add a sourced `İslami Bilgiler` screen covering family, parents, spouse, neighbours, work, trade, debt, rights, social media/privacy, anger/disputes, friendship, hospitality, illness, travel, cleanliness and daily manners.
- [ ] Run static JavaScript syntax checks and package-resource checks.

### Task 3: Native WebView bridge and Quran audio

**Files:**
- Create: `namaz-native/app/src/main/java/app/namaz/tr/MainActivity.java`
- Create: `namaz-native/app/src/main/java/app/namaz/tr/NativeBridge.java`
- Create: `namaz-native/app/src/main/java/app/namaz/tr/QuranAudioController.java`
- Create: `namaz-native/app/src/main/AndroidManifest.xml`

**Interfaces:**
- `NativeBridge.startQibla()` / `stopQibla()`
- `NativeBridge.speakTurkish(String text)` / `stopSpeech()`
- `NativeBridge.playAudio(String url,String label)` / `pauseAudio()` / `resumeAudio()` / `stopAudio()`
- `NativeBridge.savePrayerSchedule(String json)`

- [ ] Write an audio-state unit test for idle → playing → paused → playing → stopped.
- [ ] Implement MainActivity WebView with JavaScript bridge, safe window insets and Android 13+ notification permission request.
- [ ] Implement native TextToSpeech for Turkish meal reading and MediaPlayer for remote Quran recitation with one-stream-only semantics.
- [ ] Emit audio state changes into the page so the mini-player is always above navigation.
- [ ] Run unit tests and compile.

### Task 4: Sensor-driven qibla

**Files:**
- Create: `namaz-native/app/src/main/java/app/namaz/tr/QiblaMath.java`
- Create: `namaz-native/app/src/main/java/app/namaz/tr/QiblaController.java`

**Interfaces:**
- `QiblaController.start()` / `stop()`
- Callback: `onQibla(float heading,double bearing,int accuracy,boolean sensorsAvailable)`

- [ ] Keep the failing bearing unit test from Task 1 as the guard.
- [ ] Implement great-circle bearing to Kaaba.
- [ ] Implement accelerometer + magnetometer orientation via `SensorManager.getRotationMatrix` and `getOrientation`.
- [ ] Use last-known/requested location when permission is available; otherwise use stored/manual city coordinates.
- [ ] Emit sensor accuracy and explicit fallback state when required sensors are unavailable.
- [ ] Run qibla unit tests and compile.

### Task 5: Prayer scheduler, adhan playback and stop action

**Files:**
- Create: `namaz-native/app/src/main/java/app/namaz/tr/PrayerLogic.java`
- Create: `namaz-native/app/src/main/java/app/namaz/tr/PrayerScheduler.java`
- Create: `namaz-native/app/src/main/java/app/namaz/tr/PrayerAlarmReceiver.java`
- Create: `namaz-native/app/src/main/java/app/namaz/tr/AdhanService.java`
- Create: `namaz-native/app/src/main/java/app/namaz/tr/BootReceiver.java`

**Interfaces:**
- `PrayerScheduler.scheduleToday(Context, List<PrayerEvent>, String mode)`
- Alarm extras: prayer name, epoch millis, sound mode.
- Notification action: `ACTION_STOP_ADHAN` handled by `AdhanService`.

- [ ] Extend next-prayer tests to include end-of-day rollover.
- [ ] Implement exact alarm scheduling when allowed and `setAndAllowWhileIdle` fallback otherwise.
- [ ] Implement prayer notification channels and foreground adhan playback with Stop action.
- [ ] Reschedule after boot/timezone/time changes from locally cached prayer times.
- [ ] Persist per-prayer enabled flags and modes: full adhan, short tone, notification only, off.
- [ ] Run tests and compile.

### Task 6: Android home-screen widget

**Files:**
- Create: `namaz-native/app/src/main/java/app/namaz/tr/PrayerWidget.java`
- Create: `namaz-native/app/src/main/res/layout/prayer_widget.xml`
- Create: `namaz-native/app/src/main/res/xml/prayer_widget_info.xml`

**Interfaces:**
- Widget reads cached next-prayer data from SharedPreferences and opens MainActivity on tap.

- [ ] Implement compact widget with next prayer, clock time, remaining-time snapshot and optional daily ayah line.
- [ ] Update widget on prayer refresh/alarm transition and periodic system widget refresh.
- [ ] Register provider in manifest.
- [ ] Build and inspect generated manifest/resources.

### Task 7: Integration, religious-content audit and release build

**Files:**
- Modify: `namaz-native/app/src/main/assets/index.html`
- Modify: `namaz-native/app/src/main/assets/app.js`
- Create/Modify: `.github/workflows/namaz-native-build.yml`

**Interfaces:**
- GitHub Actions artifact name: `Namaz-APK` containing a single `Namaz.apk`.

- [ ] Verify every religious/social-life guidance card has a visible Quran or hadith source and no unsupported generated ruling is presented as authoritative.
- [ ] Run `gradle :app:testDebugUnitTest :app:assembleDebug`.
- [ ] Run `apksigner verify --verbose` on the APK and `unzip -t` for ZIP integrity.
- [ ] Upload exactly one APK artifact named `Namaz.apk`.
- [ ] Download the workflow artifact and deliver the single APK to the user.
