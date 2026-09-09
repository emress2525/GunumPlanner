# Günüm Planner

Android için yerel/çevrimdışı günlük planlama, görev, geçmiş ve konu bazlı not uygulamasının ilk çalışan sürümü.

## v0.1 özellikleri

- Bugünün görev ve notları
- Saatli görev hatırlatmaları
- Bildirimden **Tamamla** ve **10 dk ertele**
- Telefon yeniden başlatılınca gelecekteki hatırlatmaları yeniden kurma
- Görevi yarına erteleme
- Geçmiş günlerdeki hareketleri silinmeden saklama
- Eski güne dönüp oluşturma / tamamlama / erteleme / silme hareketlerini görme
- Farklı günlerde yazılan benzer notları otomatik konu altında toplama
- Otomatik konu başlığı önerme ve zamanla başlığı iyileştirme
- Konu başlığını elle değiştirme ve kilitleme
- Otomatik gruplamayı konu bazında açıp kapatma
- Notu başka konuya taşıma
- Notu mevcut konudan ayırıp yeni konu yapma
- Konuları birleştirme
- Bir konuya girip farklı tarihlerdeki bütün notları birlikte görme
- SQLite ile tamamen cihaz üzerinde veri saklama

## APK oluşturma

Repository GitHub'a yüklendiğinde **Actions > Build Android APK** workflow'u otomatik çalışır.

Başarılı çalışmanın altında **Artifacts > GunumPlanner-debug-apk** dosyasını indir. ZIP'in içindeki `app-debug.apk` telefona kurulabilir.

## Teknik yapı

- Java 17
- Android SDK 35
- minSdk 26
- Harici Android kütüphanesi yok; ilk sürüm mümkün olduğunca sade tutuldu.
- Veriler `SQLiteOpenHelper` ile yerel veritabanında tutulur.
