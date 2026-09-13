# Namaz V7 — Widget Suite ve Kilit Ekranı Tasarımı

## Amaç
Mevcut tek ve aşırı dolu widget yerine, kullanıcıların ihtiyacına göre ayrı ayrı ekleyebileceği küçük ve odaklı widget’lar sunmak. Ana ekranda duvar kâğıdını gereksiz kapatmayan, sade ama premium görünümlü bileşenler kullanılacak. Kilit ekranında cihaz destekliyorsa uygun widget kategorileri kullanılacak; desteklemeyen cihazlarda aynı temel bilgi güvenilir bir kilit ekranı bildirimi ile gösterilecek.

## Widget Seti
1. **Sıradaki Namaz — 1x2**
   - Sıradaki namaz adı
   - Namaz saati
   - Kalan süre
   - Kompakt şehir bilgisi

2. **Büyük Geri Sayım — 2x2**
   - Sıradaki namaz
   - Büyük tipografi ile saat
   - Büyük geri sayım
   - Minimal arka plan

3. **Tüm Vakitler — 4x2**
   - İmsak, Güneş, Öğle, İkindi, Akşam, Yatsı
   - İçinde bulunulan / sıradaki vakit belirgin
   - Hicrî tarih ve şehir küçük üst bilgi olarak

4. **Namaz Takibi — 2x2**
   - Sabah, Öğle, İkindi, Akşam, Yatsı için 5 durum göstergesi
   - Günlük 0/5…5/5 ilerleme
   - Widget’a dokununca uygulamadaki Namaz Takibi ekranı açılır

5. **Günün Ayeti — 4x2**
   - Kısa meal metni
   - Sure ve ayet numarası
   - Metin uzun olduğunda kontrollü kısaltma
   - Uygulamaya dokununca ilgili Kur’an / günlük ayet alanı açılır

6. **Hicrî Tarih — 2x2**
   - Büyük Hicrî gün numarası
   - Ay ve yıl
   - Miladî tarih küçük alt satırda
   - Şehir / konum etiketi

7. **Kur’an’da Kaldığın Yer — 2x2**
   - Sure adı
   - Ayet / sayfa bilgisi varsa gösterim
   - “Devam et” eylemi
   - Doğrudan Kur’an ekranına gider

## Görsel Tasarım
- Büyük düz beyaz blok kaldırılacak.
- Yarı saydam / açık krem ve zümrüt tonlu iki görünüm desteklenecek.
- Widget yüzeyi duvar kâğıdını mümkün olduğunca görünür bırakacak.
- Yuvarlatılmış köşeler, ince sınır, güçlü tipografi; emoji yerine mümkün olduğunca sade Android ikonografisi kullanılacak.
- Küçük widget’larda bilgi yoğunluğu sınırlandırılacak; hiçbir widget “her şeyi birden” göstermeyecek.
- Android sistem temasına göre açık / koyu uyum hedeflenecek.
- Büyük yazı ve erişilebilirlik için okunabilir kontrast korunacak.

## Mimari
Her widget ayrı `AppWidgetProvider` olacak. Ortak veri okuma ve biçimlendirme tekrarlanmayacak; bunun için bir `WidgetDataRepository` ve ortak `WidgetRenderUtils` katmanı oluşturulacak.

Önerilen sınıflar:
- `NextPrayerWidgetProvider`
- `CountdownWidgetProvider`
- `PrayerTimesWidgetProvider`
- `PrayerTrackerWidgetProvider`
- `DailyAyahWidgetProvider`
- `HijriDateWidgetProvider`
- `QuranResumeWidgetProvider`
- `WidgetDataRepository`
- `WidgetUpdateReceiver` veya ortak güncelleme yardımcı katmanı

Mevcut `PrayerWidgetProvider` geriye dönük uyumluluk için kaldırılmak yerine eski widget örneklerini kırmayacak şekilde tutulabilir; ancak widget seçicide yeni kurulum için ayrı V7 widget’ları öncelikli olacaktır. Gerekirse eski sağlayıcı “Legacy / Tüm Bilgiler” olarak görünür bırakılır veya yeni kurulum listesinde gizlenir; mevcut kurulu widget’lar bozulmamalıdır.

## Veri Akışı
- Namaz saatleri, sıradaki namaz, Hicrî tarih, günlük ayet, namaz takip durumu ve Kur’an’da son okunan konum ortak SharedPreferences/veri deposundan okunacak.
- Uygulama namaz verisini güncellediğinde tüm V7 widget’ları tek yardımcı metodla güncellenecek.
- Namaz geçişlerinde `AlarmManager` üzerinden kritik güncellemeler tetiklenecek.
- Sistem widget periyodik güncelleme limitleri nedeniyle saniyelik canlı geri sayım garanti edilmeyecek. Geri sayım dakika hassasiyetinde ve sistem kısıtlarına uygun olacak; kritik vakit geçişinde yeniden çizilecek.
- Uygulama açıldığında widget verileri her zaman tazelenecek.

## Kilit Ekranı
Android cihazlarda gerçek kilit ekranı widget desteği üretici ve sistem sürümüne göre değişebilir. Bu nedenle V7 iki aşamalı davranacak:

1. **Cihaz / launcher destekliyorsa** widget metadata’sında uygun kategori ve boyut bilgileri sağlanacak; sistemin izin verdiği kilit ekranı yüzeylerinde widget seçilebilir olacak.
2. **Destek yoksa** kalıcı ve kullanıcı tarafından kapatılabilir bir “Namaz Durumu” bildirimi kilit ekranında görünecek:
   - Sıradaki namaz
   - Namaz saati
   - Kalan süre
   - İsteğe bağlı olarak bugünün namaz takibi

Bildirim içeriği `VISIBILITY_PUBLIC` olacak ancak Ayet gibi uzun / özel içerikler kilit ekranı bildiriminin içine konmayacak. Kullanıcı ayarlardan bu kilit ekranı bildirimini tamamen kapatabilecek.

## Bildirim Kanalı
- Ayrı kanal: `Namaz durumu / kilit ekranı`
- Ezan kanalından ayrı olacak.
- Sessiz, titreşimsiz varsayılan davranış.
- Ezan sesini tetiklemez.
- Kullanıcı Android sistem ayarlarından görünürlüğünü değiştirebilir.

## Eylemler
- Sıradaki Namaz / Geri Sayım → uygulama ana sayfası
- Tüm Vakitler → ana sayfa / vakitler
- Namaz Takibi → takip ekranı
- Günün Ayeti → ilgili Kur’an / günlük ayet alanı
- Kur’an’da Kaldığın Yer → Kur’an kaldığın yer
- Hicrî Tarih → uygulama ana sayfası veya takvim alanı

## Widget Seçici İsimleri
Widget seçicisinde kullanıcı ne eklediğini açıkça görecek:
- Namaz — Sıradaki Vakit
- Namaz — Büyük Geri Sayım
- Namaz — Tüm Vakitler
- Namaz — Namaz Takibi
- Namaz — Günün Ayeti
- Namaz — Hicrî Tarih
- Namaz — Kur’an’a Devam

## Hata / Boş Veri Davranışı
- Namaz vakitleri yoksa: “Vakitleri güncellemek için Namaz’ı aç”
- Hicrî tarih yoksa miladî tarih gösterilir.
- Günlük ayet yoksa widget boş beyaz alan bırakmaz; “Günün ayeti uygulama açılınca güncellenecek” gibi kısa durum metni gösterir.
- Kur’an son okuma kaydı yoksa “Henüz okuma kaydı yok” gösterilir.
- Takip kaydı yoksa 0/5 gösterilir.

## Android Uyumluluğu
- minSdk mevcut proje ile aynı kalır.
- `RemoteViews` ile cihaz üreticileri arasında daha geniş uyumluluk korunur.
- Sistem kilit ekranı widget özelliği zorla varsayılmaz.
- Kilit ekranı bildirimi Android 13+ bildirim iznine tabi olacaktır; izin verilmezse uygulama bunu açıkça gösterecek ve sessizce başarısız olmayacaktır.

## Testler
### Unit / regression
- Her widget sağlayıcısının doğru layout’u kullandığı doğrulanır.
- Ortak veri deposu eksik veri ve tam veri durumlarında test edilir.
- Sıradaki namaz ve kalan süre metni test edilir.
- Widget isimlerinin ve XML metadata dosyalarının pakette olduğu kontrol edilir.

### Build / package
- Gradle unit tests
- `assembleDebug`
- `apksigner verify --verbose`
- `aapt dump badging`
- APK içinde tüm widget provider sınıfları, layout’lar ve provider XML dosyaları kontrol edilir.

### Manuel cihaz kontrol listesi
- Her 7 widget’ın widget seçicide ayrı görünmesi
- Her birinin bağımsız eklenebilmesi
- Farklı boyutlarda kırpılmaması
- Duvar kâğıdını aşırı kapatmaması
- Namaz vakti değişince güncellenmesi
- Takip durumunun değişince güncellenmesi
- Kur’an son okuma widget’ının doğru ekrana gitmesi
- Kilit ekranı bildiriminin görünmesi ve kapatılabilmesi
- Bildirim izni reddedilince uygulamanın çökmeden açıklama göstermesi

## Kabul Kriterleri
- Tek büyük widget zorunlu değildir; en az 7 ayrı widget seçilebilir.
- Her widget yalnız kendi görevi için gereken bilgiyi gösterir.
- Tüm Vakitler widget’ı dışında hiçbir küçük widget altı vakti birden göstermez.
- Widget’lar mevcut dev beyaz blok görünümünden daha kompakt ve görsel olarak dengeli olur.
- Namaz verisi, Hicrî tarih, ayet, takip ve Kur’an kaldığın yer doğru ortak veriden beslenir.
- Kilit ekranında cihaz destekliyorsa widget yüzeyi kullanılabilir; desteklemiyorsa sessiz kalıcı bildirim alternatifi vardır.
- Kullanıcı kilit ekranı bildirimini kapatabilir.
- APK derlenir, imzası doğrulanır ve tüm widget varlıkları paket içinde doğrulanır.
