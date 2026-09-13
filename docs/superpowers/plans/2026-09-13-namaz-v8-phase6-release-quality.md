# Namaz V8 Faz 6 — Gizlilik, Erişilebilirlik ve Final Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** V8’i tek kurulabilir telefon APK’sı olarak son kalite kapısından geçirmek; yerel yedek, görünüm/erişilebilirlik, izin minimizasyonu, içerik kaynak ekranı ve kapsamlı CI doğrulaması eklemek.

**Architecture:** Ayarlar yerel saklanır; dışa aktarma kullanıcı eylemiyle SAF üzerinden yapılır. Hassas kadın kayıtları varsayılan yedeğin dışında kalır. Final workflow tüm regresyonları, yeni unit testleri, lint, assemble, source contract, offline asset sayıları, placeholder taraması, manifest, signature ve package metadata’yı doğrular.

**Tech Stack:** Kotlin/Compose, Android Storage Access Framework, DataStore, CI shell assertions, apksigner/aapt.

**Spec:** `docs/superpowers/specs/2026-09-13-namaz-v8-islamic-life-platform-design.md`

## Global Constraints
- Tek telefon APK’sı `app.namaz.tr.v8`, `8.0.0`.
- Reklam/hesap zorunluluğu yok.
- Hassas veri cihazda; izinler işlev çağrıldığında istenir.
- Son APK’da “sonraki fazda”, TODO veya boş placeholder ekranı bulunmaz.
- Fiziksel cihazda test edildiği iddia edilmez; CI/emülatör doğrulaması açıkça belirtilir.

---

### Task 1: Appearance and accessibility
**Files:** Create `AccessibilitySettings.kt`, `AppearanceScreen.kt`; modify theme.
- [ ] Standart/Büyük Yazı/Yaşlı modu ve açık/koyu/sistem tema durum testlerini yaz.
- [ ] Daha büyük dokunma hedefleri, yüksek okunabilirlik ve font ölçeklerini uygula.
- [ ] Arapça metnin ayrı ölçek kontrolünü ekle.

### Task 2: Backup/export
**Files:** Create `BackupManager.kt`, `DataPrivacyScreen.kt`, tests.
- [ ] Dışa aktarılabilir güvenli alanların allowlist testini yaz.
- [ ] Namaz takibi, ayarlar, Kur’an ilerleme/yer imi, öğrenme ilerleme ve zikir hedeflerini JSON yedek formatına ekle.
- [ ] Kadınlara özel kayıtları varsayılan yedekten hariç tut.
- [ ] SAF CreateDocument/OpenDocument ile kullanıcı kontrollü export/import ekle.

### Task 3: Sources/privacy screen
**Files:** Create `SourcesScreen.kt`, `PrivacyScreen.kt`.
- [ ] Kur’an veri paketi, tefsir, namaz hesap yöntemi ve uygulama içi dini içerik kaynaklarını göster.
- [ ] “hesap zorunlu değil / reklam yok / veriler varsayılan olarak cihazda” politikasını görünür yap.

### Task 4: Final regression contracts
**Files:** Create `namaz-native/test_v8_final_contract.py`; modify workflow.
- [ ] Kaynak ağacında WebView, TODO ve placeholder metni bulunmamasını test et.
- [ ] Yedi widget provider, namaz receiver/service, Quran/Learn/Worship ekranları ve final artifact adını test et.
- [ ] CI’da 114 sure + 114 tefsir asset sayısını doğrula.

### Task 5: Final APK pipeline
**Files:** Modify `.github/workflows/namaz-v8-build.yml`.
- [ ] Workflow adını `Build Namaz V8 Final APK` yap.
- [ ] V7 regresyon + tüm V8 unit tests + lint + assemble çalıştır.
- [ ] `apksigner verify`, `unzip -t`, `aapt dump badging/xmltree` kontrol et.
- [ ] Paket `app.namaz.tr.v8`, versionCode 8, versionName 8.0.0 ve label `Namaz V8` doğrula.
- [ ] `Namaz-V8.apk` adıyla artifact `Namaz-V8-APK` yükle.
- [ ] SHA-256 çıktısını logla.

### Task 6: Final verification
- [ ] En son commit üzerinde workflow tamamen `success` olmadan tamamlandı deme.
- [ ] Artifact’ı indir, ZIP’i aç, gerçek APK’nın yolunu ve hashini yeniden doğrula.
- [ ] Kullanıcıya yalnız final `Namaz-V8.apk` bağlantısını ver; test kapsamını kısa ve doğru açıkla.
