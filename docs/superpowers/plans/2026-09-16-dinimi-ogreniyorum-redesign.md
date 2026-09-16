# Dinimi Öğreniyorum Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename and redesign the existing verified Android app as Dinimi Öğreniyorum, fix Android system-bar overlap, and substantially deepen structured Islamic learning content.

**Architecture:** Preserve the existing single-activity native Android architecture and verified services/repositories. Add one focused `LearningPathCatalog` core class for structured lessons, expand existing catalogs, and update `MainActivity` rendering plus resources without replacing alarm/Quran/Billing subsystems.

**Tech Stack:** Android Java 17, compile/target SDK 36, min SDK 26, Google Play Billing 9.1.0, JUnit 4.13.2.

**Spec:** `docs/superpowers/specs/2026-09-16-dinimi-ogreniyorum-redesign.md`

## Global Constraints
- Display name is exactly `Dinimi Öğreniyorum`.
- Main app UI remains native Android.
- Turkish red/white visual identity with crescent/star launcher icon.
- Bottom navigation must dynamically respect system bar insets.
- Minimum supported Android API remains 26.
- Existing prayer alarm, Quran, qibla, worship, hadith, backup, widget and billing capabilities remain intact.
- Learning catalog >=150 topics, sirah >=35 items, worship guides >=30, learning path >=18, Quran duas >=20, Esma exactly 99.
- No unlicensed full-text Diyanet/TDV book redistribution.

---

### Task 1: Content contract tests
**Files:** Modify `app/src/test/java/com/namazv2/core/AdvancedCoreLogicTest.java`.
- [x] Raise catalog coverage assertions and add structured learning-path assertions.
- [x] Run a core compile/check before implementation and confirm the new requirements fail.

### Task 2: Structured learning path and expanded catalogs
**Files:** Create `app/src/main/java/com/namazv2/core/LearningPathCatalog.java`; modify `LearningCatalog.java`, `WorshipGuideCatalog.java`.
- [x] Add >=18 structured beginner lessons with objectives, explanation, practice steps, sources and madhhab notes.
- [x] Expand reference learning topics to >=150 total entries.
- [x] Expand sirah to >=35 milestones.
- [x] Expand worship guides to >=30.
- [x] Run core contract checks and confirm they pass.

### Task 3: Brand, icon and responsive shell
**Files:** Modify `MainActivity.java`, `AndroidManifest.xml`, `values/strings.xml`, `values/colors.xml`, `values/styles.xml`; create adaptive icon resources.
- [x] Rename visible product strings to Dinimi Öğreniyorum.
- [x] Apply red/white palette and Turkish-flag-inspired launcher icon.
- [x] Add system window inset handling so top/bottom chrome never sits behind status/navigation bars.
- [x] Keep >=48dp touch targets and scrollable content.

### Task 4: Learning UX
**Files:** Modify `MainActivity.java`.
- [x] Add a prominent `Başlangıç Eğitimi` entry and render structured lessons.
- [x] Improve learning page with fast category filters and content-count summary.
- [x] Preserve search, sources and madhhab notes.

### Task 5: Full CI verification and artifact
**Files:** Repack verified source parts and update `.github/workflows/namaz-v2-build.yml` source hash/artifact names.
- [ ] Run core tests and all Android unit tests.
- [ ] Run Android lint.
- [ ] Build debug APK and release AAB candidate.
- [ ] Verify APK signature.
- [ ] Install on API 35 emulator, cold-start, run 200 Monkey events, scan logcat for crash/ANR.
- [ ] Upload only after all checks pass.
