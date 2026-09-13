# Namaz V6 Learning Academy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the shallow V5 Learn screen with an in-app, source-labelled, Hanefi-first learning academy whose lessons are detailed, navigable, searchable, persistent, and visually polished without breaking Quran, qibla, adhan, prayer times or widgets.

**Architecture:** Keep the install-tested Android/WebView shell and native bridge. Add a dedicated `assets/academy/` subsystem with separate data, Hanefi lesson content, Shafii difference notes, academy controller JS, and academy CSS. A deterministic Python patcher injects only the academy shell and asset references into the generated HTML. Android back first delegates to the academy history stack. V6 is packaged separately from V5.

**Tech Stack:** Android SDK 35, Java 17, WebView/JavascriptInterface, localStorage, HTML/CSS/JavaScript academy assets, Python deterministic patchers/tests, Gradle 8.9, GitHub Actions, Android apksigner.

**Spec:** `docs/superpowers/specs/2026-09-13-namaz-v6-learning-academy-design.md`

## Global Constraints

- Main fiqh presentation is Hanefi; relevant Shafii differences are shown in a separate visible note.
- Do not copy copyrighted `Tam Namaz Hocası` prose; only use topic scope and write original explanations.
- Every lesson must include at least one visible source record.
- Core Learn cards must never call `show('knowledge')`, `show('duas')` or `show('namazHocasi')`.
- Existing Quran, qibla, adhan, prayer times, tracker and widget behavior must remain available.
- Learn content must remain usable offline after installation.
- Package id is `app.namaz.tr.v6`; versionCode `6`; versionName `6.0.0`; visible label `Namaz V6`.
- Bottom navigation and media player may not cover lesson content; safe-area padding must be present.

---

### Task 1: Define failing V6 academy regression tests

**Files:**
- Create: `namaz-native/test_v6_academy.py`

**Interfaces:**
- Consumes: `v6_academy.upgrade_html`, `v6_academy.asset_files`, `v6_academy.validate_assets`.
- Produces: executable expectations for all V6 learning behavior/content packaging.

- [ ] **Step 1: Write the failing tests**

```python
import unittest
import v6_academy

class V6AcademyTests(unittest.TestCase):
    def test_learn_shell_never_routes_core_lessons_to_legacy_screens(self):
        out = v6_academy.upgrade_html(BASE_HTML)
        self.assertIn('id="academyRoot"', out)
        self.assertNotIn("onclick=\"show('knowledge')\"", v6_academy.learn_section(out))
        self.assertNotIn("onclick=\"show('duas')\"", v6_academy.learn_section(out))
        self.assertNotIn("onclick=\"show('namazHocasi')\"", v6_academy.learn_section(out))

    def test_assets_have_nine_categories_five_prayers_sources_and_shafii_notes(self):
        files = v6_academy.asset_files()
        v6_academy.validate_assets(files)
        joined = '\n'.join(files.values())
        for token in ['abdest','gusul','teyemmum','sabah-namazi','ogle-namazi','ikindi-namazi','aksam-namazi','yatsi-namazi','Şafiî','Kaynak']:
            self.assertIn(token, joined)
```

- [ ] **Step 2: Run** `PYTHONPATH=namaz-native python3 -m unittest namaz-native/test_v6_academy.py -v`.
- [ ] **Step 3: Confirm RED** because `v6_academy` does not exist.
- [ ] **Step 4: Commit the failing test only.**

### Task 2: Create the academy asset subsystem and detailed content

**Files:**
- Create: `namaz-native/v6_academy.py`
- Generated at build time: `app/src/main/assets/academy/academy-data.js`
- Generated at build time: `app/src/main/assets/academy/lessons-hanafi.js`
- Generated at build time: `app/src/main/assets/academy/lessons-shafii-notes.js`
- Generated at build time: `app/src/main/assets/academy/academy.js`
- Generated at build time: `app/src/main/assets/academy/academy.css`
- Test: `namaz-native/test_v6_academy.py`

**Interfaces:**
- `asset_files() -> dict[str,str]`
- `upgrade_html(html: str) -> str`
- `apply(root: Path) -> None`
- `validate_assets(files: dict[str,str]) -> None`
- `self_test(root: Path) -> None`

- [ ] **Step 1: Implement catalog data** with at least nine categories and real lesson IDs. Required lesson IDs include `islam-ve-iman`, `32-farz`, `abdest`, `gusul`, `teyemmum`, `namazin-sartlari`, `namazin-hareketleri`, `namazda-okunanlar`, all five daily-prayer lessons, `cemaat`, `cuma`, `sehiv-secdesi`, `seferilik`, `kaza`, `hastalikta-namaz`, `vesvese`, `oruc-temel`, `zekat-temel`, `hac-umre-temel`, `aile`, `komsuluk`, `ticaret`, `borc-kul-hakki`, `giybet`, `ofke`, `mahremiyet-sosyal-medya`.
- [ ] **Step 2: Write Hanefi lesson content** as structured JS objects with `summary`, `sections`, `mistakes`, `hanafi`, `sources`, optional `arabicBlocks`, `steps`, and `quiz`. Abdest must explain its four Hanefi farz, step-by-step order, bozanlar and common errors. Gusul must explain Hanefi farz and full sequence. Teyemmum must have conditions and steps. Namaz structure must explain şart/farz/vacip/sünnet and movements.
- [ ] **Step 3: Write five-prayer lessons rekât by rekât.** Each must show total farz/sünnet structure, every rekât’s main movement/reading sequence, first/final sitting differences, and a beginner-friendly “Şimdi ne yapıyorum?” step flow.
- [ ] **Step 4: Add prayer readings** with Arabic, Turkish pronunciation, meaning and usage for Sübhaneke, Fatiha, İhlas, Kevser, Asr, Felak, Nas, ruku/secde tasbih, Ettehiyyatü, Salli/Barik and Rabbena duası. Do not invent audio URLs; the academy uses native TTS only for Turkish explanatory text when requested.
- [ ] **Step 5: Add Shafii difference notes** at minimum for ablution intention/order, head wiping quantity, selected prayer-rukn/vacip distinctions, congregation classification and applicable special-case lessons. Notes are shown only where relevant.
- [ ] **Step 6: Add source records** referencing Qur'an verse identifiers, DİB Namaz İlmihali/İlmihal and Din İşleri Yüksek Kurulu topic names; no lesson may have an empty source list.
- [ ] **Step 7: Run tests and keep GREEN.**

### Task 3: Build a real academy UI/controller instead of dashboard links

**Files:**
- Modify generated `academy/academy.js` and `academy/academy.css` strings in `namaz-native/v6_academy.py`.
- Modify generated `index.html` via `upgrade_html`.

**Interfaces:**
- Global functions: `academyHome()`, `academyOpenCategory(categoryId)`, `academyOpenLesson(lessonId, anchor)`, `academyBack()`, `academyNextLesson()`, `academyPreviousLesson()`, `academySearch(query)`, `academyToggleComplete(lessonId)`, `academyHandleAndroidBack()`.

- [ ] **Step 1: Replace the V5 Learn section** with a shell containing `academyRoot`, progress hero, search input, category chips, beginner path, continue card, lesson list and detail container.
- [ ] **Step 2: Implement internal history** so lesson/category navigation never calls `show()` and `academyBack()` restores the previous Learn view.
- [ ] **Step 3: Persist progress** in `localStorage` keys `academy.completed`, `academy.lastLesson`, `academy.quiz`, `academy.review`, and `academy.scroll` with defensive JSON parsing.
- [ ] **Step 4: Implement search** over title, keywords and lesson body and open results directly in the matching lesson.
- [ ] **Step 5: Render lesson detail** with source box, Hanefi box, optional Shafii difference box, common-error callout, Arabic/pronunciation/meaning blocks, quiz and previous/next controls.
- [ ] **Step 6: Build academy CSS** for emerald/cream/gold visual system, 44px touch targets, one-column fallback under 390px, dark-theme variables, elderly-mode-friendly sizing, Arabic blocks, progress ring/bar, category chips and minimum bottom safe-area padding `calc(132px + env(safe-area-inset-bottom))`.
- [ ] **Step 7: Add simple inline SVG posture diagrams** for kıyam/rükû/secde/oturuş without external image dependencies.
- [ ] **Step 8: Run Python regression tests and self-test.**

### Task 4: Make Android back respect academy navigation

**Files:**
- Modify: `namaz-native/overlay/app/src/main/java/app/namaz/tr/MainActivity.java`

**Interfaces:**
- JS returns boolean from `window.academyHandleAndroidBack()`.

- [ ] **Step 1: Change `onBackPressed()`** to evaluate `window.academyHandleAndroidBack && window.academyHandleAndroidBack()` first.
- [ ] **Step 2: If JS reports handled, remain in the academy. If not, fall back to WebView history then Activity back.**
- [ ] **Step 3: Build Android unit/compile target to ensure no Java regression.**

### Task 5: Package distinct V6 and verify the academy is really inside the APK

**Files:**
- Modify: `.github/workflows/namaz-native-build.yml`
- Modify: `namaz-native/overlay/app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces artifact `Namaz-V6.apk`.

- [ ] **Step 1: Rename workflow to `Build Namaz V6 APK`.**
- [ ] **Step 2: Set generated package metadata** to `app.namaz.tr.v6`, versionCode `6`, versionName `6.0.0`, label `Namaz V6`.
- [ ] **Step 3: Run `v6_academy.py --apply namazapp` after V5 patching and `--self-test namazapp` after generation.**
- [ ] **Step 4: Change widget visible brand to `Namaz V6`.**
- [ ] **Step 5: Add verification assertions** for `assets/academy/academy-data.js`, `lessons-hanafi.js`, `lessons-shafii-notes.js`, `academy.js`, `academy.css`, package metadata, `id="academyRoot"`, daily prayer lesson IDs, source markers and Shafii note markers.
- [ ] **Step 6: Run `gradle -p namazapp testDebugUnitTest`, `assembleDebug`, `apksigner verify --verbose`, `unzip -t`, `aapt dump badging`.**
- [ ] **Step 7: Upload only `Namaz-V6.apk`.**

### Task 6: Fresh delivery verification

**Files:** none

- [ ] **Step 1: Require every workflow step to report success.**
- [ ] **Step 2: Download the workflow artifact and unpack it.**
- [ ] **Step 3: Check final APK SHA-256 and ZIP integrity locally.**
- [ ] **Step 4: Deliver the single V6 APK and state only what was actually verified; do not claim device installation unless a real device test happened.**

## Self-review

- Spec coverage: internal lesson navigation, persistent progress, search, detailed ablution/gusul/tayammum, five prayers, readings, Hanefi-first/Shafii-notes, social-life lessons, sources, visual redesign, safe area and V6 packaging are assigned to tasks.
- Existing qibla/Quran/adhan/widget code is intentionally preserved except visible V6 identity and Android-back integration.
- There are no TODO/TBD placeholders and all cross-task function names are defined above.
