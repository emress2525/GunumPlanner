# Namaz V8 Implementation Roadmap

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Namaz V7'yi bozmadan, yeni ve tamamen native Namaz V8 uygulamasını altı bağımsız teslimat halinde üretmek ve her aşamada kurulabilir/test edilebilir APK çıkarmak.

**Architecture:** V8 mevcut WebView/patch zincirinin üzerine büyütülmeyecek. Repo içinde bağımsız `namaz-v8/` Android projesi oluşturulacak; Kotlin + Jetpack Compose + Room + DataStore + WorkManager/AlarmManager tabanlı olacak. V7, `namaz-native/` altında çalışmaya devam edecek ve V8 tamamlanana kadar geri dönüş noktası olarak korunacak.

**Tech Stack:** Android API 26+, compile/target SDK 35, Java 17, Kotlin 2.0.21, Jetpack Compose, Material 3, Navigation Compose, Room, DataStore, WorkManager, AlarmManager, Media3, Android App Widgets/Glance, Wear OS Data Layer.

**Spec:** `docs/superpowers/specs/2026-09-13-namaz-v8-islamic-life-platform-design.md`

## Global Constraints

- Uygulama reklamsız ve hesap zorunluluğu olmadan çalışır.
- Hassas veriler varsayılan olarak cihazda tutulur.
- Offline-first yaklaşım korunur; internet yokken temel namaz, Kur'an ve öğrenme akışları çalışır.
- Hanefî ana anlatım kullanılır; Şafiî farkları açıkça etiketlenir.
- Dini asistan kaynak bulamadığında hüküm uydurmaz.
- Namaz/ezan altyapısı diğer modüllerin hatasından bağımsız çalışır.
- V7 kaynakları silinmez veya V8 için yeniden adlandırılmaz.
- Her faz TDD ile uygulanır ve ayrı çalışan APK üretir.
- V8 paket adı `app.namaz.tr.v8`, uygulama adı `Namaz V8`, versionCode `8`, versionName `8.0.0` olur.

---

## Fazlar

### Faz 1 — Native Foundation + Bugün + Namaz

**Teslimat:** Kurulabilir native Compose APK; onboarding, Basit/Tam mod, beş sekmeli kabuk, Bugün ekranı, namaz vakitleri veri modeli/cache, namaz takibi, alarm/ezan altyapısı ve Bildirim Sağlık Merkezi.

**Ayrıntılı plan:** `docs/superpowers/plans/2026-09-13-namaz-v8-phase1-foundation-prayer.md`

**Kabul:** APK açılır; WebView kullanılmaz; namaz verisi offline cache'den gösterilebilir; alarm alıcıları boot sonrası yeniden kurulabilir; notification permission/exact alarm durumu teşhis edilebilir.

### Faz 2 — Kur'an Pro + Hatim + Tajvid + Ezber

**Teslimat:** Mushaf/ayet görünümü, meal/tefsir katmanları, kelime detayları, arama, yer imi/not/geçmiş, Media3 ses oynatma, offline ses paketleri, hatim hedefi, tajvid açıklaması ve ezber modları.

**Plan dosyası:** `docs/superpowers/plans/2026-09-13-namaz-v8-phase2-quran-pro.md`

**Kabul:** Temel Kur'an metni ve varsayılan Türkçe meal internet olmadan okunur; son 10 konum geri yüklenir; indirilmiş ses offline oynar; ezber yardımcısı belirsiz ses eşleşmesini kesin hata diye sunmaz.

### Faz 3 — Öğren Akademisi + Elif-Bâ + İlmihal

**Teslimat:** Öğrenme rotaları, ders şablonu, Namaz Öğreniyorum, namazda okunanlar, Elif-Bâ, dini sözlük, 32 Farz, günlük hayat senaryoları, mini test ve ilerleme sistemi.

**Plan dosyası:** `docs/superpowers/plans/2026-09-13-namaz-v8-phase3-learning.md`

**Kabul:** Ders → alt konu → test → kaynak → geri → kaldığın yer zinciri kesintisiz çalışır; Hanefî/Şafiî ayrımı veri modelinde zorunludur; kaynak alanı olmayan doğrulanmış dini içerik yayınlanamaz.

### Faz 4 — İbadet ve Güvenilir Bilgi Merkezi

**Teslimat:** Dua, zikir, Esmaül Hüsna, hadis, kaynaklı dini arama/asistan, Ramazan, Hac/Umre, kadınlara özel kayıtlar, seyahat rehberi, İslami takvim ve zekât hesap yardımcısı.

**Plan dosyası:** `docs/superpowers/plans/2026-09-13-namaz-v8-phase4-worship-content.md`

**Kabul:** Dua/hadis kaynak alanları görünür; AI yanıtlarında kaynak etiketi vardır; kaynak yoksa cevap kesilir; hassas kadın kayıtları varsayılan yedeğe girmez; Ramazan/Hac temel paketleri offline açılır.

### Faz 5 — Widget Studio + Kilit Ekranı + Cami/Kıble + Wear OS

**Teslimat:** V7'deki yedi widget'ın native V8 eşdeğerleri, Widget Studio, public sessiz kilit ekranı bildirimi, kıble sensör güvenlik katmanı, yakın camiler ve Wear OS companion.

**Plan dosyası:** `docs/superpowers/plans/2026-09-13-namaz-v8-phase5-device-surfaces.md`

**Kabul:** Widget'lar tek PrayerRepository kaynağını kullanır; OEM'de keyguard widget yoksa fallback bildirim çalışır; Wear'da sıradaki namaz/tüm vakitler/takip/tesbih görünür; düşük pusula doğruluğu kullanıcıya açıkça bildirilir.

### Faz 6 — Gizlilik + Yedekleme + Erişilebilirlik + Release QA

**Teslimat:** Şifreli yerel yedek/içe aktarma, izin minimizasyonu, Standart/Büyük Yazı/Yaşlı modu, koyu tema, içerik paket sürümleme, performans/regresyon testleri ve son release APK.

**Plan dosyası:** `docs/superpowers/plans/2026-09-13-namaz-v8-phase6-release-quality.md`

**Kabul:** Tam otomatik test takımı yeşil; APK imzası/paket/sürüm doğrulanmış; temel offline paketler APK'da; namaz/ezan reboot, saat dilimi ve pil senaryoları test edilmiş; erişilebilirlik metin taşmaları kontrol edilmiş.

## Bağımlılık sırası

Faz 1 zorunlu tabandır. Faz 2 ve Faz 3, Faz 1'in veri/tema/navigasyon çekirdeğine bağlanır ama birbirinden bağımsız uygulanabilir. Faz 4, Faz 2 ve Faz 3'ün ortak `ContentSource`/`Citation` modelini kullanır. Faz 5, Faz 1'in PrayerRepository'si ve Faz 2'nin QuranResume verisini tüketir. Faz 6 bütün fazların üzerine son kalite/yayın katmanını ekler.

## Her faz için standart doğrulama komutları

```bash
gradle -p namaz-v8 testDebugUnitTest --stacktrace
gradle -p namaz-v8 lintDebug --stacktrace
gradle -p namaz-v8 assembleDebug --stacktrace
${ANDROID_HOME}/build-tools/35.0.0/apksigner verify --verbose namaz-v8/app/build/outputs/apk/debug/app-debug.apk
${ANDROID_HOME}/build-tools/35.0.0/aapt dump badging namaz-v8/app/build/outputs/apk/debug/app-debug.apk
```

Her faz ayrıca kendi feature testlerini ve manifest/artifact doğrulamalarını GitHub Actions içinde çalıştırır.
