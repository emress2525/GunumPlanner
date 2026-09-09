# Günüm Planner

Android için çevrimdışı çalışan günlük planlama, görev, geçmiş ve konu bazlı not uygulaması.

## v0.2

- Geçmiş günlerin hareket günlüğünü korur.
- Farklı günlerdeki benzer notları otomatik konu altında toplar.
- Konu başlığını otomatik iyileştirir; başlık kilitlenebilir ve elle değiştirilebilir.
- Notlar ve ilgili görevler aynı konu hafızasında gösterilebilir.
- Benzer konu dosyalarını bulur ve kullanıcı onayıyla birleştirir.
- Türkçe doğal dil ile hızlı görev: `Yarın 14:30 Mehmet'i ara 30 dk her hafta`.
- `bugün`, `yarın`, `öbür gün`, hafta günleri, `dd.MM`, saat, süre, öncelik ve tekrar ifadelerini algılar.
- Sesle görev ve sesli not ekleme (telefondaki konuşma tanıma servisini kullanır).
- Tekrarlanan görevler: her gün, hafta içi, her hafta, her ay.
- Tekrarlanan görev tamamlanınca sıradaki görev otomatik oluşturulur ve hatırlatması kurulur.
- Görev süresi ve öncelik kaydı.
- Arama: eski görev ve notların başlık/içeriğinde arama.
- Gün özeti: açık, tamamlanan, not ve erteleme sayıları.
- Akıllı plan önerisi: gecikmiş/saatsiz işleri süre ve önceliğe göre boş saatlere dizer; kullanıcı onayı olmadan değişiklik yapmaz.
- Bildirimden tamamla ve 10 dakika ertele.
- Telefon yeniden başlatılınca gelecek hatırlatmaları yeniden kurar.
- SQLite ile veriler cihazda tutulur; mevcut v0.1 verileri v0.2'ye otomatik taşınır.

## APK

Her `main` güncellemesinde GitHub Actions otomatik debug APK üretir:

**Actions → Build Android APK → Artifacts → GunumPlanner-debug-apk**

## Teknik

- Java 17
- Android SDK 35
- minSdk 26
- Harici Android kütüphanesi yok
