# Namaz V8 Full APK Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Faz-1 native V8 tabanını, placeholder bırakmadan Kur’an, Öğren, İbadet, araçlar, kıble, yedekleme ve yedi widget ile tek kurulabilir telefon APK’sına tamamlamak.

**Architecture:** Mevcut `namaz-v8/` Kotlin/Compose projesi korunur. Yeni işlevler ayrı feature paketlerinde/odaklı dosyalarda tutulur; kritik namaz/ezan katmanı değiştirilmeden üstüne bağlanır. Kur’an verisi derleme sırasında sürümü sabitlenmiş `quran-json@3.1.2` Türkçe paketinden APK asset’ine alınır ve kaynak ekranında CC-BY-SA 4.0 + veri kaynağı açıkça belirtilir.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose Material 3, Room, DataStore, Media3, AlarmManager, Android AppWidget/RemoteViews, Android ICU, Java 17, SDK 35.

**Spec:** `docs/superpowers/specs/2026-09-13-namaz-v8-islamic-life-platform-design.md`

## Global Constraints

- Paket adı `app.namaz.tr.v8`, versionCode `8`, versionName `8.0.0` kalır.
- WebView kullanılmaz.
- V7 kaynakları değiştirilmez.
- Hesap zorunlu değildir, reklam SDK’sı eklenmez.
- Namaz/ezan sistemi Kur’an/öğren/ibadet modüllerinden bağımsız kalır.
- Dini içerikte kaynak görünür olur; Hanefî ana anlatım ve Şafiî farkları ayrı işaretlenir.
- Kaynaklı dini asistan yalnız yerel doğrulanmış içerikte eşleşme varsa cevap döndürür; aksi halde fetva uydurmaz.
- Kadınlara özel kayıtlar varsayılan yedek dışında tutulur.
- Wear OS ayrı kurulum gerektirdiği için telefon APK’sına sahte bir saat uygulaması gömülmez; V8 telefon APK’sında saat senkron altyapısına hazır veri modeli bulunur, gerçek wearable teslimatı ayrı paket olur.
- Her davranış test-first ilerler; tamamlanma iddiası ancak fresh CI test + lint + assemble + apksigner + aapt doğrulamasından sonra yapılır.

---

### Task 1: Full-product contract tests

**Files:**
- Create: `namaz-v8/app/src/test/java/app/namaz/tr/v8/FullProductContractTest.kt`
- Create: `namaz-v8/core/model/src/test/java/app/namaz/tr/v8/model/FullProductRulesTest.kt`

**Interfaces:**
- Consumes: existing `AppDestination`, prayer models.
- Produces: failing contracts proving placeholders are gone and pure rules exist for hatim, zakat, qibla and source-grounded answers.

- [ ] **Step 1: Write failing tests** that reference `HatimPlan`, `ZakatCalculator`, `QiblaMath`, `SourceGroundedSearch` and assert five main destinations remain.
- [ ] **Step 2: Run** `gradle -p namaz-v8 testDebugUnitTest --stacktrace`; expect compilation failure because the new types do not exist.
- [ ] **Step 3: Do not change tests after the intended missing-type failure is observed.**

### Task 2: Quran Pro native reader

**Files:**
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/quran/QuranModels.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/quran/QuranAssetRepository.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/quran/QuranProgressStore.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/quran/QuranScreen.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/quran/HatimPlan.kt`
- Modify: `.github/workflows/namaz-v8-build.yml`
- Modify: `namaz-v8/app/build.gradle.kts`

**Interfaces:**
- Produces: `@Composable fun QuranScreen()`; asset repository with surah list/search/ayah data; `HatimPlan.pagesPerDay(totalPages:Int=604, days:Int):Int`.

- [ ] **Step 1: Add a unit test for hatim page distribution** expecting ceiling division.
- [ ] **Step 2: Run test and confirm RED** because `HatimPlan` is missing.
- [ ] **Step 3: Implement minimal model/repository/store/UI.** Reader must support surah list, Turkish search, Arabic + meal display, font-size control, bookmark/last-read, simple hatim target and source/license panel. No WebView.
- [ ] **Step 4: CI fetches** `https://cdn.jsdelivr.net/npm/quran-json@3.1.2/dist/quran_tr.json` into `app/src/main/assets/quran_tr.json` before Gradle tasks and verifies it is non-empty JSON.
- [ ] **Step 5: Run unit tests; expect GREEN.**

### Task 3: Learning Academy + verified local content

**Files:**
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/learn/LearningContent.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/learn/LearnScreen.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/learn/LearningProgressStore.kt`

**Interfaces:**
- Produces: native lesson cards for iman, temizlik, abdest, namaz, namazda okunanlar, Elif-Bâ, oruç, zekât, hac and günlük ahlak. Every `Lesson` has `source`, `hanafi`, optional `shafiiNote`.

- [ ] **Step 1: Test** that every lesson has a non-blank source and at least one lesson has a Shafi‘i note; verify RED before implementation.
- [ ] **Step 2: Implement** lesson catalog, searchable category screen, detail screen and mini quiz/progress using SharedPreferences/DataStore.
- [ ] **Step 3: Run tests; expect GREEN.**

### Task 4: Worship hub + source-grounded answer engine

**Files:**
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/worship/WorshipModels.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/worship/WorshipScreen.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/worship/WorshipStore.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/worship/SourceGroundedSearch.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/worship/ZakatCalculator.kt`

**Interfaces:**
- Produces: dua, zikir sayaç, Esma, hadis, Ramazan, Hac/Umre, kadınlara özel kayıt, seyahat rehberi, takvim, zekât matematik yardımcısı, kaynaklı yerel arama.
- `SourceGroundedSearch.answer(query:String): GroundedAnswer?` returns null when no local verified source matches.
- `ZakatCalculator.amount(eligibleWealth:Double, rate:Double=.025):Double` rejects negative values.

- [ ] **Step 1: Tests first** for source-miss returns null, source-hit exposes source, zikir increments without losing state, zakat calculation is pure math.
- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: Implement native hub and detail flows** with visible source labels and privacy note for women-specific data.
- [ ] **Step 4: Verify GREEN.**

### Task 5: Qibla, mosque launcher, backup and accessibility tools

**Files:**
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/tools/QiblaMath.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/tools/ToolsScreen.kt`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/tools/BackupManager.kt`

**Interfaces:**
- `QiblaMath.bearing(latitude:Double, longitude:Double):Double` returns 0..360 bearing to Kaaba.
- `BackupManager` exports/imports non-sensitive preference JSON; women-specific store is excluded unless explicitly opted in.

- [ ] **Step 1: Test Ankara qibla bearing falls in a physically plausible 150..170 degree range and normalization stays 0..360; verify RED.**
- [ ] **Step 2: Implement qibla bearing + compass UI, mosque search via geo intent, Islamic calendar/Hijri display, local backup export/import and large-text toggle.**
- [ ] **Step 3: Verify GREEN.**

### Task 6: Seven home-screen widgets + lock-screen status fallback

**Files:**
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/widget/WidgetPrayerData.kt`
- Create: seven provider classes under `.../widget/`
- Create: seven layouts under `namaz-v8/app/src/main/res/layout/`
- Create: seven provider metadata XML files under `namaz-v8/app/src/main/res/xml/`
- Create: `namaz-v8/app/src/main/java/app/namaz/tr/v8/widget/PrayerStatusNotification.kt`
- Modify: `namaz-v8/app/src/main/AndroidManifest.xml`

**Interfaces:**
- Widgets: next prayer, countdown, all times, prayer tracker, daily ayah, Hijri date, Quran resume.
- Providers read the same prayer settings/calculator and Quran progress snapshot.

- [ ] **Step 1: Add manifest/resource contract test and verify RED before providers exist.**
- [ ] **Step 2: Implement RemoteViews providers and keyguard/home_screen metadata.**
- [ ] **Step 3: Add silent public ongoing prayer-status notification as OEM fallback, user-controlled from More.**
- [ ] **Step 4: Verify GREEN.**

### Task 7: Integrate navigation, remove placeholders, release QA

**Files:**
- Modify: `namaz-v8/app/src/main/java/app/namaz/tr/v8/ui/NamazApp.kt`
- Modify: `namaz-v8/app/src/main/java/app/namaz/tr/v8/AppGraph.kt`
- Modify: `namaz-v8/app/src/main/java/app/namaz/tr/v8/ui/theme/NamazTheme.kt`
- Modify: `.github/workflows/namaz-v8-build.yml`

**Interfaces:**
- Five tabs open real screens; no `PlaceholderScreen` remains in product navigation.

- [ ] **Step 1: Add test that scans `NamazApp.kt` source and fails if `PlaceholderScreen` or `sonraki faz` remains; verify RED.**
- [ ] **Step 2: Wire Quran/Learn/Worship/Tools real screens, add settings/source/about/large-text controls, keep Android back stack internal to module.**
- [ ] **Step 3: Run** `gradle -p namaz-v8 testDebugUnitTest --stacktrace`, `gradle -p namaz-v8 lintDebug --stacktrace`, `gradle -p namaz-v8 :app:assembleDebug --stacktrace`.
- [ ] **Step 4: Verify APK** with `apksigner verify --verbose`, `unzip -t`, `aapt dump badging`, manifest checks for prayer services + seven widget receivers, and verify `assets/quran_tr.json` is packaged.
- [ ] **Step 5: Upload artifact as `Namaz-V8-Full-APK` only after every previous command exits 0.**
