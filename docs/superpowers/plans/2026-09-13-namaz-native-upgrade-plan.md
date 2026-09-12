# Namaz Native Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the current Namaz APK with safe-area UI fixes, native qibla sensing, native Quran/Turkish-meal audio controls, background prayer alarms with stoppable adhan audio, a home-screen widget, and sourced Islamic social-life guidance.

**Architecture:** Preserve the working WebView Quran/prayer UI, but move device-dependent features into a native Android bridge. GitHub Actions will first regenerate the current clean WebView project from the existing workflow, overlay native Java/XML resources, patch the generated HTML/JS deterministically, run unit tests, build one debug APK, and verify its signature.

**Tech Stack:** Android SDK 35, Java 17, WebView + JavascriptInterface, SensorManager, AlarmManager, BroadcastReceiver, Foreground Service/MediaPlayer, AppWidgetProvider, TextToSpeech, GitHub Actions/Gradle 8.9.

**Spec:** `docs/superpowers/specs/2026-09-13-namaz-native-upgrade-design.md`

## Global Constraints

- No forced account, ads, or analytics SDK.
- Prayer/personal tracking remains local to the device.
- Device-specific features must fail gracefully and state limitations explicitly.
- Religious guidance must show source metadata and avoid presenting disputed rulings as universally settled.
- APK must come from one Gradle build and pass Android signature verification.

---

### Task 1: Native math and schedule model

**Files:**
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/PrayerMath.java`
- Create: `namaz-native/overlay/app/src/test/java/app/namaz/tr/PrayerMathTest.java`

**Interfaces:**
- Produces: `PrayerMath.qiblaBearing(double lat, double lon)`, `PrayerMath.normalizeDegrees(double)`, `PrayerMath.combineDayAndTime(long dayEpochMs, String hhmm)`.

- [ ] **Step 1: Write the failing test**

```java
@Test public void ankaraBearingIsSoutheast() {
  double b = PrayerMath.qiblaBearing(39.9334, 32.8597);
  assertTrue(b > 150 && b < 170);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle -p namazapp testDebugUnitTest`
Expected: FAIL because `PrayerMath` does not exist.

- [ ] **Step 3: Implement the minimal math helpers**

```java
public static double normalizeDegrees(double value) {
  value %= 360d;
  return value < 0 ? value + 360d : value;
}
```

Implement the great-circle initial bearing to Kaaba coordinates `21.4225, 39.8262` and local HH:mm-to-epoch helper.

- [ ] **Step 4: Run test to verify it passes**

Run: `gradle -p namazapp testDebugUnitTest`
Expected: PASS.

### Task 2: Safe-area and native WebView bridge

**Files:**
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/MainActivity.java`
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/NativeBridge.java`
- Create: `namaz-native/patch_assets.py`

**Interfaces:**
- Consumes: existing generated `assets/index.html`, `assets/app.js`.
- Produces JS bridge methods: `playQuran(url,title)`, `pauseResumeQuran()`, `stopQuran()`, `speakTurkish(text)`, `startQibla()`, `stopQibla()`, `schedulePrayers(json)`, `updateWidget(json)`.

- [ ] **Step 1: Add patch regression tests inside `patch_assets.py --self-test`**

Verify patched CSS contains bottom content inset greater than the fixed nav height, toast/mini-player positions above the nav, and JS contains native bridge fallbacks.

- [ ] **Step 2: Run self-test before patch implementation**

Run: `python3 namaz-native/patch_assets.py --self-test`
Expected: FAIL because patch functions are not implemented.

- [ ] **Step 3: Implement deterministic CSS/JS patching**

Patch `.app` bottom padding, reader last-card spacing, toast and mini-player offsets. Replace browser-only Quran audio and speech synthesis paths with native bridge calls while retaining Web Audio fallback for non-Android preview.

- [ ] **Step 4: Verify patch self-test passes**

Run: `python3 namaz-native/patch_assets.py --self-test`
Expected: PASS.

### Task 3: Sensor-driven qibla

**Files:**
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/QiblaController.java`

**Interfaces:**
- Consumes: latitude/longitude passed from JS/configured prayer metadata.
- Produces: JavaScript callback `window.onNativeHeading(heading, accuracy)` and error callback `window.onNativeQiblaUnavailable(reason)`.

- [ ] **Step 1: Add unit tests for heading normalization through `PrayerMath`**
- [ ] **Step 2: Verify new test fails for missing behavior**
- [ ] **Step 3: Implement SensorManager accelerometer + magnetometer fusion using `getRotationMatrix` / `getOrientation`**
- [ ] **Step 4: Verify unit tests pass and compile succeeds**

### Task 4: Background prayer alarms and stoppable adhan

**Files:**
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/PrayerScheduler.java`
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/PrayerAlarmReceiver.java`
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/AdhanService.java`
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/BootReceiver.java`

**Interfaces:**
- `PrayerScheduler.schedule(Context,String,long,String)` schedules one prayer.
- `PrayerAlarmReceiver` starts notification/adhan according to mode.
- `AdhanService.ACTION_STOP` stops playback and foreground notification.

- [ ] **Step 1: Add schedule intent/request-code tests around deterministic helper methods**
- [ ] **Step 2: Verify tests fail**
- [ ] **Step 3: Implement exact alarm when permitted, `setAndAllowWhileIdle` fallback otherwise, notification permission handling, foreground playback, Stop action, reboot rescheduling**
- [ ] **Step 4: Run unit tests and Android compile**

### Task 5: Home-screen widget and sourced social-life content

**Files:**
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/PrayerWidgetProvider.java`
- Create: `namaz-native/overlay/app/src/main/res/layout/prayer_widget.xml`
- Create: `namaz-native/overlay/app/src/main/res/xml/prayer_widget_info.xml`
- Modify through patch script: generated HTML/JS to add `İslami Bilgiler` cards.

**Interfaces:**
- Widget reads `SharedPreferences("namaz_native")` keys `nextPrayerName`, `nextPrayerTime`, `nextPrayerEpoch`.
- Social-life items carry visible `source` strings such as `İsrâ 17:23-24` and `Hucurât 49:6`.

- [ ] **Step 1: Add patch test asserting every social-life card has a non-empty source label**
- [ ] **Step 2: Verify test fails before content injection**
- [ ] **Step 3: Add categories for parents/family, spouse, neighbours, speech, news verification/social media, privacy/backbiting, work/trade, debt, justice, anger/forgiveness, friendship, conflict, cleanliness and travel with Quran-source labels**
- [ ] **Step 4: Verify patch tests pass**

### Task 6: Build, signature verification and artifact delivery

**Files:**
- Create: `.github/workflows/namaz-native-build.yml`
- Create/modify through overlay: generated Android manifest/resources.

**Interfaces:**
- Workflow regenerates the known-good base app from `.github/workflows/namaz-build.yml`, overlays `namaz-native/overlay`, patches assets, downloads the documented MIT-licensed adhan audio used by the build, runs tests, assembles one APK, verifies with `apksigner`, and uploads artifact `Namaz.apk`.

- [ ] **Step 1: Configure Android 35/Java 17 build and overlay pipeline**
- [ ] **Step 2: Run `python3 namaz-native/patch_assets.py --self-test`**
- [ ] **Step 3: Run `gradle -p namazapp testDebugUnitTest assembleDebug`**
- [ ] **Step 4: Run `apksigner verify --verbose namazapp/app/build/outputs/apk/debug/app-debug.apk`**
- [ ] **Step 5: Upload exactly one user-facing APK artifact named `Namaz.apk`**
