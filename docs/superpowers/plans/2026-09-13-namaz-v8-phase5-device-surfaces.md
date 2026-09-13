# Namaz V8 Faz 5 — Widget, Kilit Ekranı, Kıble ve Cihaz Yüzeyleri Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Namaz verisini ana ekran widget’larına, kilit ekranı bildirimine ve kıble/cami araçlarına güvenilir biçimde taşımak; Android platform sınırlarını kullanıcıya açık göstermek.

**Architecture:** Uygulama açıldığında canonical PrayerRepository sonucu küçük bir `WidgetPrayerSnapshot` olarak SharedPreferences’a yazılır. Yedi AppWidgetProvider aynı snapshot’ı okur. Keyguard widget desteklenmeyen cihazlarda sessiz public ongoing notification kullanılır. Kıble, Kaabe bearing hesabı + sensör doğruluk durumu ile sunulur.

**Tech Stack:** AppWidgetProvider/RemoteViews, NotificationManager, SensorManager, Location opt-in, Compose.

**Spec:** `docs/superpowers/specs/2026-09-13-namaz-v8-islamic-life-platform-design.md`

## Global Constraints
- Yedi widget birbirinden ayrı seçilebilir.
- Widget/lock-screen değerleri ana uygulama ile aynı PrayerRepository’den türetilir.
- Lock-screen widget evrensel diye vaat edilmez; fallback bildirim bulunur.
- Konum izni isteğe bağlıdır; manuel şehir çalışmaya devam eder.

---

### Task 1: Shared snapshot
**Files:** Create `WidgetPrayerSnapshotStore.kt`, tests.
- [ ] PrayerSchedule → snapshot dönüşüm testi yaz.
- [ ] Sıradaki namaz/kalan süre/şehir/Hicrî/5 vakit/takip/Quran resume alanlarını kaydet.
- [ ] Uygulama refresh olduğunda bütün widget’ları güncelle.

### Task 2: Seven native widgets
**Files:** Create seven providers + XML layouts + provider metadata.
- [ ] Sıradaki Namaz 1x2.
- [ ] Büyük Geri Sayım 2x2.
- [ ] Tüm Vakitler 4x2.
- [ ] Namaz Takibi 2x2.
- [ ] Günün Ayeti 4x2.
- [ ] Hicrî Tarih 2x2.
- [ ] Kur’an’da Kaldığın Yer 2x2.
- [ ] Manifestte yedi provider ve `home_screen|keyguard` metadata doğrulaması ekle.

### Task 3: Lock status fallback
**Files:** Create `PrayerStatusNotifier.kt`, `LockStatusSettings.kt`.
- [ ] Low importance, silent, vibration kapalı, public visibility ve ongoing notification kur.
- [ ] Kullanıcı toggle’ı ile aç/kapat; Android 13+ bildirim iznini kontrol et.

### Task 4: Qibla
**Files:** Create `QiblaScreen.kt`, `QiblaMath.kt`, tests.
- [ ] Ankara/İstanbul gibi referans koordinatlarda bearing aralığını test et.
- [ ] Sensör azimutunu bearing ile birleştir; düşük doğrulukta kalibrasyon uyarısı göster.
- [ ] Sensör yoksa sayısal yön/bearing göster.

### Task 5: Mosque + travel quick tools
**Files:** Create `MosqueScreen.kt`.
- [ ] Kullanıcı izni varsa mevcut koordinatla `geo:` cami aramasını aç.
- [ ] İzin yoksa manuel şehir üzerinden arama yönlendirmesi sağla.
- [ ] Uygulama içi sonuç uydurma; harita sağlayıcısına açıkça yönlendir.

### Task 6: Wear boundary
**Files:** Create `WearInfoScreen.kt`.
- [ ] Telefon APK’sının modern Wear OS cihazına otomatik companion kuramayacağını açıkça belirt.
- [ ] Telefon tarafında paylaşılabilir namaz snapshot sözleşmesini hazırla; ayrı wearable teslimatı gerektiğini saklama.

### Task 7: Integration + verification
- [ ] Widget merkezi, kıble, cami ve kilit ekranı ayarlarını Daha Fazla’ya bağla.
- [ ] Manifest/provider ve snapshot testlerini çalıştır.
- [ ] Final CI’da yedi receiver + widget metadata + notification class kontrol edilsin.
