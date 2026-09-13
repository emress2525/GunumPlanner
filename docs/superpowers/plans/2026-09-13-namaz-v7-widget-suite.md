# Namaz V7 Widget Suite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the single overloaded V6 widget with seven focused home-screen widgets and add a reliable lock-screen prayer-status fallback notification while preserving the V6 Academy and prayer/audio behavior.

**Architecture:** Keep the current native Android overlay and SharedPreferences data pipeline. Add one shared `WidgetDataRepository` for reading/formatting widget data, one `WidgetRenderUtils` helper for click intents and common presentation rules, seven independent `AppWidgetProvider` implementations with dedicated layouts/provider XML, and a `PrayerStatusNotification` helper for lock-screen visibility. `PrayerWidgetProvider` remains only for backwards compatibility; all new installs expose the seven V7 widgets.

**Tech Stack:** Android SDK 35, Java 17, `AppWidgetProvider`, `RemoteViews`, `SharedPreferences`, `AlarmManager`, Android notifications, current WebView/native bridge, GitHub Actions/Gradle 8.9.

**Spec:** `docs/superpowers/specs/2026-09-13-namaz-v7-widget-suite-design.md`

## Global Constraints

- Preserve all V6 Academy content, Qur'an, qibla, adhan, prayer scheduling, and existing stored user data.
- Build V7 as `applicationId 'app.namaz.tr.v7'`, `versionCode 7`, `versionName '7.0.0'`, app label `Namaz V7`.
- Keep current project `minSdk` unchanged.
- Use `RemoteViews`; do not require Compose/Glance.
- Expose seven independent widgets: next prayer, countdown, all prayer times, tracker, daily ayah, Hijri date, Quran resume.
- Lock-screen fallback notification is silent by default and separate from the adhan channel.
- No second-by-second widget refresh; countdown is minute-level/best-effort and refreshed at prayer transitions/app refresh.
- Existing legacy widget must not crash existing installations.
- Android 13+ notification permission denial must not crash the app.

---

### Task 1: Shared widget data model and regression tests

**Files:**
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/WidgetDataRepository.java`
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/WidgetRenderUtils.java`
- Create: `namaz-native/test_v7_widgets.py`

**Interfaces:**
- Consumes: `PrayerScheduler.PREFS` SharedPreferences keys already written by V6.
- Produces: `WidgetDataRepository.Snapshot load(Context)` and `WidgetRenderUtils.openAppIntent(Context, String)` used by every V7 provider.

- [ ] **Step 1: Write the failing packaging/structure test**

Create `namaz-native/test_v7_widgets.py` asserting the repo contains all seven provider class names, seven layout names, seven provider XML names, V7 package/version markers, and the lock-screen helper/channel marker.

- [ ] **Step 2: Run the test and verify RED**

Run: `PYTHONPATH=namaz-native python3 -m unittest namaz-native.test_v7_widgets -v`

Expected: FAIL because V7 provider/resources do not exist yet.

- [ ] **Step 3: Implement `WidgetDataRepository`**

Create an immutable `Snapshot` carrying:
`city`, `hijri`, `gregorian`, `nextPrayerName`, `nextPrayerTime`, `nextPrayerEpoch`, six prayer times, `tracked`, `dailyAyahText`, `dailyAyahSource`, `quranResumeText`.

`load(Context)` reads existing SharedPreferences and returns safe fallbacks. Add `remainingText(long epoch)` returning `"1 sa 12 dk kaldı"`, `"23 dk kaldı"`, or `"Vakitleri güncellemek için Namaz'ı aç"`.

- [ ] **Step 4: Implement `WidgetRenderUtils`**

Provide a stable `PendingIntent openAppIntent(Context context, String destination, int requestCode)` that opens `MainActivity` and includes extra `openScreen` when a specific destination is supplied.

- [ ] **Step 5: Run all Python regression tests**

Run: `PYTHONPATH=namaz-native python3 -m unittest discover -s namaz-native -p 'test_*.py' -v`

Expected: existing tests remain green except assertions waiting for later V7 files.

- [ ] **Step 6: Commit**

Commit message: `feat: add shared V7 widget data layer`

---

### Task 2: Seven independent widget providers and layouts

**Files:**
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/NextPrayerWidgetProvider.java`
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/CountdownWidgetProvider.java`
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/PrayerTimesWidgetProvider.java`
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/PrayerTrackerWidgetProvider.java`
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/DailyAyahWidgetProvider.java`
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/HijriDateWidgetProvider.java`
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/QuranResumeWidgetProvider.java`
- Create layouts: `widget_next_prayer.xml`, `widget_countdown.xml`, `widget_prayer_times.xml`, `widget_tracker.xml`, `widget_daily_ayah.xml`, `widget_hijri.xml`, `widget_quran_resume.xml`

**Interfaces:**
- Consumes: `WidgetDataRepository.load(Context)` and `WidgetRenderUtils.openAppIntent(...)`.
- Produces: each provider exposes static `updateAll(Context)` and renders only its own focused content.

- [ ] **Step 1: Expand the failing test with provider/layout assertions**

Assert each provider references exactly its dedicated `R.layout.widget_*` resource and does not reference the old `R.layout.prayer_widget`.

- [ ] **Step 2: Verify RED**

Run the V7 test; expect missing providers/layouts.

- [ ] **Step 3: Implement providers**

Each provider loads one `Snapshot`, builds one `RemoteViews`, applies text/fallback visibility, attaches the correct click destination, and calls `manager.updateAppWidget`.

- [ ] **Step 4: Implement compact layouts**

Use rounded translucent/cream surfaces with dark green text, 12–20sp typography depending on widget size, no long paragraphs except the dedicated ayah widget, and no six-time grid outside `widget_prayer_times.xml`.

- [ ] **Step 5: Run tests**

Run V7 test and all Python tests; expect provider/layout assertions PASS.

- [ ] **Step 6: Commit**

Commit message: `feat: add seven focused prayer widgets`

---

### Task 3: Widget provider metadata and Android manifest registration

**Files:**
- Create XML metadata: `res/xml/widget_next_prayer_info.xml`, `widget_countdown_info.xml`, `widget_prayer_times_info.xml`, `widget_tracker_info.xml`, `widget_daily_ayah_info.xml`, `widget_hijri_info.xml`, `widget_quran_resume_info.xml`
- Modify native overlay/patch path that writes `AndroidManifest.xml` so all seven receivers are registered.
- Modify: `namaz-native/overlay/app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: provider classes/layouts from Task 2.
- Produces: seven independently selectable widget entries in Android's widget picker.

- [ ] **Step 1: Add failing metadata/manifest assertions**

Assert seven `appwidget-provider` XML files exist, each points to its correct layout, and generated manifest contains seven `<receiver>` entries with `android.appwidget.action.APPWIDGET_UPDATE` plus matching metadata.

- [ ] **Step 2: Verify RED**

Run the V7 test; expect manifest/metadata assertions FAIL.

- [ ] **Step 3: Implement metadata**

Use focused min sizes: next prayer 110x60dp, countdown 180x110dp, prayer times 250x110dp, tracker 180x110dp, ayah 250x110dp, Hijri 180x110dp, Quran resume 180x110dp. Set `resizeMode="horizontal|vertical"` and `widgetCategory="home_screen|keyguard"` where accepted by platform metadata.

- [ ] **Step 4: Register all receivers and add picker labels**

Add strings exactly:
`Namaz — Sıradaki Vakit`, `Namaz — Büyük Geri Sayım`, `Namaz — Tüm Vakitler`, `Namaz — Namaz Takibi`, `Namaz — Günün Ayeti`, `Namaz — Hicrî Tarih`, `Namaz — Kur'an'a Devam`.

- [ ] **Step 5: Run tests**

Expected: metadata/manifest assertions PASS.

- [ ] **Step 6: Commit**

Commit message: `feat: register V7 widget suite`

---

### Task 4: Common refresh path and prayer-transition updates

**Files:**
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/WidgetSuiteUpdater.java`
- Modify: `NativeBridge.java`
- Modify: `PrayerScheduler.java` and/or `PrayerAlarmReceiver.java`
- Modify legacy `PrayerWidgetProvider.java` only to delegate common refresh behavior where safe.

**Interfaces:**
- Produces: `WidgetSuiteUpdater.updateAll(Context)` calling all seven providers plus legacy provider.

- [ ] **Step 1: Add failing test**

Assert `WidgetSuiteUpdater.updateAll` contains all seven provider update calls and that prayer-data refresh paths call `WidgetSuiteUpdater.updateAll` rather than only `PrayerWidgetProvider.updateAll`.

- [ ] **Step 2: Verify RED**

Run V7 test; expected FAIL.

- [ ] **Step 3: Implement updater and replace single-widget refresh calls**

The update order is next prayer, countdown, all times, tracker, daily ayah, Hijri, Quran resume, legacy.

- [ ] **Step 4: Run tests**

Expected: refresh assertions PASS and all previous V6 regression tests remain PASS.

- [ ] **Step 5: Commit**

Commit message: `feat: refresh all V7 widgets from prayer data changes`

---

### Task 5: Lock-screen prayer-status notification fallback

**Files:**
- Create: `namaz-native/overlay/app/src/main/java/app/namaz/tr/PrayerStatusNotification.java`
- Modify: `MainActivity.java` and/or `NativeBridge.java` to expose enable/disable refresh.
- Modify manifest generation to retain `POST_NOTIFICATIONS` permission and required components.
- Modify Web settings UI patch to add a `Kilit ekranında namaz durumu` toggle persisted with existing configuration.

**Interfaces:**
- Produces: `PrayerStatusNotification.update(Context)` and `PrayerStatusNotification.cancel(Context)`.
- Channel id: `prayer_status`; importance low/silent; `Notification.VISIBILITY_PUBLIC`.

- [ ] **Step 1: Add failing tests**

Assert helper creates `prayer_status`, uses `IMPORTANCE_LOW`, disables sound/vibration, sets public visibility, includes next prayer/time/remaining, and provides `cancel(Context)`.

- [ ] **Step 2: Verify RED**

Run V7 test; expected FAIL because helper does not exist.

- [ ] **Step 3: Implement notification helper**

On Android 13+, if notification permission is denied, return without throwing. Use one stable notification id. Clicking opens app home. Do not include daily ayah text.

- [ ] **Step 4: Add user setting bridge**

Default enabled for V7 only after notification permission is granted; expose clear UI text explaining OEM lock-screen widget support varies and this notification is the fallback.

- [ ] **Step 5: Refresh notification when prayer/widget data refreshes**

Call `PrayerStatusNotification.update` from the same data refresh points as `WidgetSuiteUpdater` when the setting is enabled.

- [ ] **Step 6: Run tests**

Expected: V7 notification assertions PASS; V6 behavior tests remain green.

- [ ] **Step 7: Commit**

Commit message: `feat: add lock-screen prayer status fallback`

---

### Task 6: V7 build identity, package verification, and APK artifact

**Files:**
- Modify: `.github/workflows/namaz-native-build.yml`
- Modify: `namaz-native/test_v7_widgets.py`
- Modify: `namaz-native/overlay/app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces artifact: `Namaz-V7.apk`, workflow artifact `Namaz-V7-APK`.

- [ ] **Step 1: Add failing workflow assertions**

Require `applicationId 'app.namaz.tr.v7'`, `versionCode 7`, `versionName '7.0.0'`, `Namaz V7`, `Namaz-V7.apk`, `Namaz-V7-APK`, and package checks for all seven provider/layout/XML assets.

- [ ] **Step 2: Verify RED**

Run V7 test; expected workflow identity assertions FAIL while workflow still targets V6.

- [ ] **Step 3: Update workflow identity and package verification**

Keep V6 Academy generation/verification, then overlay V7 widget resources. Verify with `apksigner verify --verbose`, `aapt dump badging`, `unzip -t`, and `unzip -l`/`grep` for each V7 widget asset and provider class.

- [ ] **Step 4: Run complete local/static regression suite in CI**

Commands in CI:
`PYTHONPATH=namaz-native python3 -m unittest discover -s namaz-native -p 'test_*.py' -v`
`gradle -p namazapp testDebugUnitTest --stacktrace`
`gradle -p namazapp assembleDebug --stacktrace`

Expected: all PASS.

- [ ] **Step 5: Verify final artifact**

Require package `app.namaz.tr.v7`, version 7/7.0.0, label `Namaz V7`, successful APK v2 signature verification, ZIP integrity success, and all seven widget assets present.

- [ ] **Step 6: Commit**

Commit message: `build: package verified Namaz V7 widget suite`

---

## Final Verification Checklist

- [ ] Python V6 + V7 regression tests all pass.
- [ ] Java unit tests pass.
- [ ] Android debug APK assembles successfully.
- [ ] `apksigner verify --verbose Namaz-V7.apk` succeeds.
- [ ] `aapt dump badging` reports `app.namaz.tr.v7`, version 7, label `Namaz V7`.
- [ ] APK contains seven provider classes, seven widget layouts, seven provider XML files, V6 Academy assets, and adhan audio.
- [ ] Lock-screen notification channel is separate/silent and permission-safe.
- [ ] Existing legacy widget provider remains package-valid.
- [ ] Download workflow artifact, extract `Namaz-V7.apk`, run local ZIP integrity and SHA-256 checks before delivery.
