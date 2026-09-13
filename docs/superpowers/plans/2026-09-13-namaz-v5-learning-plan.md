# Namaz v5 Learning App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a visibly new, content-rich Namaz V5 Android APK with a premium Today screen, Learn center, expanded Namaz Hocasi, daily-life Islamic guidance, denser widget, and existing Quran/qibla/adhan native capabilities preserved.

**Architecture:** Keep the current install-safe hybrid shell because the native bridge already provides background adhan, qibla sensor, Quran audio and Android widgets. Replace the sparse web UI with a richer app shell and learning data, while keeping native services unchanged. Give V5 a new package id and visible version identity so it cannot be confused with previous builds.

**Tech Stack:** Android 35, Java native bridge/services/widgets, WebView HTML/CSS/JS UI, Python deterministic asset patchers, GitHub Actions, Gradle 8.9, Android apksigner.

**Spec:** `docs/superpowers/specs/2026-09-13-namaz-v5-learning-design.md`

## Global Constraints
- Package id must be `app.namaz.tr.v5`.
- Visible app label must be `Namaz V5`.
- No ads, account requirement, analytics SDK, or unsupported religious claims.
- Religious learning cards show source notes; school-specific differences are labeled.
- Bottom navigation must not obscure page content.
- Existing qibla, Quran audio, adhan, and native widget paths must continue compiling.

---

### Task 1: Define V5 regression expectations

**Files:**
- Create: `namaz-native/test_v5_upgrade.py`

**Interfaces:**
- Consumes: generated `index.html`, `app.js`, native widget layout.
- Produces: test expectations for `v5_upgrade.apply_upgrade(project_root)`.

- [ ] **Step 1: Write failing tests** checking for five-tab navigation, V5 marker, Learn screen, beginner path, Namaz Hocasi modules, social-life topics, source labels, safe bottom padding, and widget density markers.
- [ ] **Step 2: Run `PYTHONPATH=namaz-native python3 -m unittest namaz-native/test_v5_upgrade.py -v` and verify failure because `v5_upgrade.py` does not exist.**
- [ ] **Step 3: Commit failing tests.**

### Task 2: Implement the V5 UI/content patcher

**Files:**
- Create: `namaz-native/v5_upgrade.py`
- Test: `namaz-native/test_v5_upgrade.py`

**Interfaces:**
- Produces: `apply_upgrade(project_root: Path) -> None` and `self_test(project_root: Path) -> None`.

- [ ] **Step 1: Build deterministic CSS injection** for emerald/cream visual system, large hero, compact prayer chips, Learn progress cards, lesson accordions, source pills, social-life grid, and 130px safe bottom area.
- [ ] **Step 2: Replace Today screen** with next-prayer hero, six times, daily spiritual cards, tracker/continue cards, and prominent Learn/Namaz Hocasi actions while preserving IDs used by existing JS (`nextPrayer`, `countdown`, `prayers`, `verseAr`, `verseTr`, `verseSrc`, `lastRead`, `dateText`, `locText`).
- [ ] **Step 3: Add Learn screen** with beginner path and direct modules for abdest, namaz movements/words, five daily prayers, Namaz Hocasi, Elif-Ba roadmap, and daily-life guidance.
- [ ] **Step 4: Expand Namaz Hocasi and social-life sections** with source-labelled original summaries covering purification, prayer essentials, special prayers, congregation, travel/illness, qada, khushu, family, trade, debt, rights, gossip, anger, privacy and social media.
- [ ] **Step 5: Replace bottom navigation** with Today / Quran / Learn / Worship / More while keeping Qibla reachable from Today and More.
- [ ] **Step 6: Add visible `V5` marker** to app UI.
- [ ] **Step 7: Run regression test until green and commit.**

### Task 3: Upgrade widget density and identity

**Files:**
- Modify: `namaz-native/overlay/app/src/main/res/layout/prayer_widget.xml`
- Modify: `namaz-native/overlay/app/src/main/java/app/namaz/tr/PrayerWidgetProvider.java`
- Modify: `namaz-native/overlay/app/src/main/res/xml/prayer_widget_info.xml`

**Interfaces:**
- Widget reads shared preferences already written by `NativeBridge.updateWidget`/`updateAyah`.

- [ ] **Step 1: Add a clear V5 header, city/Hijri row, next prayer + countdown, six-time grid, daily ayah, and 0/5 tracker row.**
- [ ] **Step 2: Keep RemoteViews-compatible widgets only.**
- [ ] **Step 3: Ensure missing preference values degrade to `—` without crashing.**
- [ ] **Step 4: Run Android unit compilation and commit.**

### Task 4: Build V5 as a distinct installable package

**Files:**
- Modify: `.github/workflows/namaz-native-build.yml`

**Interfaces:**
- Consumes all patchers and native overlay.
- Produces artifact `Namaz-V5.apk`.

- [ ] **Step 1: Change applicationId to `app.namaz.tr.v5`, versionCode to 5, versionName to `5.0.0`.**
- [ ] **Step 2: Rewrite generated app label from `Namaz` to `Namaz V5`.**
- [ ] **Step 3: Apply `v5_upgrade.py --apply` after existing patchers and run `--self-test`.**
- [ ] **Step 4: Add asset assertions for V5 marker, Learn screen, Namaz Hocasi, and social-life content.**
- [ ] **Step 5: Verify with `apksigner verify --verbose`, `unzip -t`, `aapt dump badging`, and artifact-content grep.**
- [ ] **Step 6: Upload `Namaz-V5.apk`.**

### Task 5: Fresh verification and delivery

**Files:** none

- [ ] **Step 1: Inspect the final GitHub Actions run and require every build/test/verify step to conclude `success`.**
- [ ] **Step 2: Download the workflow artifact.**
- [ ] **Step 3: Unzip locally, verify the APK exists, and perform a second archive/content sanity check.**
- [ ] **Step 4: Deliver one APK link only after verification evidence is fresh.**

## Self-review
- Spec coverage: Today, Learn, Namaz Hocasi, social life, visual redesign, widget richness, V5 identity, native qibla/Quran/adhan preservation all map to tasks above.
- Scope intentionally excludes licensed full-offline Quran packaging and map-based mosque discovery for this build.
- No placeholders or undefined cross-task function names remain.
