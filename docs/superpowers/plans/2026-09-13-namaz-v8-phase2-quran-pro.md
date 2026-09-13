# Namaz V8 Faz 2 — Kur’an Pro Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** V8 içine tamamen native, offline-first Kur’an okuyucu; Türkçe meal, Elmalılı tefsir, arama, yer imi/geçmiş, hatim, ses ve ezber araçları eklemek.

**Architecture:** Kur’an içeriği build sırasında lisansı ve kaynağı sabitlenmiş veri deposundan APK asset’lerine alınır. `feature:quran` yalnız asset/repository API’sini tüketir; kullanıcı durumu DataStore/SharedPreferences’ta yerel tutulur. Ses, ayet URL’lerinden Media3 ile oynatılır; tam tilavetler APK’ya gömülmez.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose, Media3, org.json, Android assets, DataStore.

**Spec:** `docs/superpowers/specs/2026-09-13-namaz-v8-islamic-life-platform-design.md`

## Global Constraints
- WebView yok.
- Arapça metin ve varsayılan Türkçe meal internet olmadan açılır.
- İçerik kaynağı/atfı görünürdür; kaynak belirsizse dini hüküm üretilmez.
- Ses indirmeleri isteğe bağlıdır; tek APK şişirilmez.
- Son 10 okuma konumu saklanır.

---

### Task 1: Offline Quran content pack
**Files:** Modify `.github/workflows/namaz-v8-build.yml`; create `namaz-v8/app/src/main/assets/content-sources.json`.
**Produces:** `assets/quran/1.json..114.json`, `assets/tafseer/elmalili/1.md..114.md`.
- [ ] CI’da 114 sure JSON’unu ve 114 Elmalılı tefsir dosyasını sabit kaynak URL’lerinden indir.
- [ ] Sayı ve minimum dosya boyutu doğrulaması ekle.
- [ ] Kaynak/lisans metadata dosyasını APK’ya koy.
- [ ] Final doğrulamada `quran/1.json`, `quran/114.json`, tefsir ve metadata asset’lerini kontrol et.

### Task 2: Quran repository + tests
**Files:** Create `feature/quran/build.gradle.kts`, `QuranModels.kt`, `AssetQuranRepository.kt`, tests `QuranRepositoryContractTest.kt`.
**Interfaces:** `QuranRepository.surahList()`, `surah(number)`, `search(query)`, `tafsir(number)`.
- [ ] Önce parser/sure sırası/search davranışını doğrulayan test yaz.
- [ ] Testin başarısız olduğunu doğrula.
- [ ] JSON parser ve arama indeksini uygula.
- [ ] Testleri yeşile getir.

### Task 3: Reader UI
**Files:** Create `QuranScreen.kt`, `QuranReaderScreen.kt`, `QuranSearchScreen.kt`.
- [ ] Sure listesi ve arama UI test sözleşmesini yaz.
- [ ] Sure başlığı, Arapça ayet, meal, görünür kaynak, ayet numarası ve tefsir aç/kapat akışını uygula.
- [ ] Büyük Arapça yazı, meal görünürlük anahtarı ve koyu tema uyumunu koru.

### Task 4: Bookmark/history/khatm
**Files:** Create `QuranProgressStore.kt`, `KhatmPlanner.kt`, tests.
- [ ] Son 10 konum, yer imi, günlük hedef ve 30/60/90/özel plan hesap testlerini yaz.
- [ ] Yerel kalıcı store ve yeniden dağıtma hesabını uygula.
- [ ] Reader işlemlerine “Kaydet / Hatime Ekle / Ezbere Ekle” bağla.

### Task 5: Audio + memorization
**Files:** Create `QuranAudioController.kt`, `MemorizationScreen.kt`.
- [ ] Ayet ses URL seçimi, oynat/durdur ve tekrar sayaç durum testini yaz.
- [ ] Media3 oynatıcıyı ekle; çevrimdışı indirilmiş dosya varsa onu tercih et.
- [ ] Dinle-tekrar, kelime gizleme, devamını getir ve karışık tekrar modlarını ekle.
- [ ] Sesli değerlendirmede yalnız “kontrol et” belirsizlik dili kullan; kesin mahreç/tajvid hükmü verme.

### Task 6: App integration + verification
**Files:** Modify `settings.gradle.kts`, `app/build.gradle.kts`, `NamazApp.kt`.
- [ ] `feature:quran` modülünü bağla ve Kur’an placeholder’ını kaldır.
- [ ] `testDebugUnitTest`, `lintDebug`, `assembleDebug` çalıştır.
- [ ] CI’da 114 sure asset’i, kaynak metadata ve placeholder yokluğu doğrulansın.
