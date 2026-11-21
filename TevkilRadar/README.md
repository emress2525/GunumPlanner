# Tevkil Radarı — Android 1.2 (bildirimsiz Instagram API desteği)

Ankara merkezli tevkil ilanlarını kullanıcı tarafından seçilen anahtar kelimeler, ilçeler ve ücret sınırına göre ayıklayan **yerel Android uygulaması**. Ayrı iki takip yolu sunar:

1. **Bildirim erişimi (isteğe bağlı):** WhatsApp/Telegram/Instagram'ın telefon ekranına gelen görünür bildirim metinlerini cihazda filtreler. Bunlara erişmek için Android bildirim dinleyicisi izni gerekir.
2. **Instagram resmî Meta API (isteğe bağlı ve farklı):** Instagram bildirimlerini açmadan, **Meta Business Discovery yetkisi sağlanmışsa**, listedeki profesyonel Instagram hesaplarının ulaşılabilen gönderi açıklamalarını kontrol eder. Başlangıç listesinde `@tevkiliniz` yer alır. Bu özellik **kendiliğinden yetki sağlamaz**; hesap türü, profesyonel IG User ID, Meta onayları ve erişim anahtarı gerekir. Kişisel hesap gönderilerine, kapalı içeriklere ve hikâyelere erişim vaadi yoktur.

## APK kurulumu

GitHub Actions iş akışının başarıyla tamamlandığı çalıştırmada `TevkilRadari-APK` ZIP dosyasını indir; içindeki `app-debug.apk` dosyasını telefona kur. Kaynak proje Java 17 / Gradle 8.10.2 / Android API 35 ile derlenir.

## Resmî Instagram API kurulum koşulları

- Meta for Developers üzerinden uygun bir uygulama ve Facebook Login temelli Instagram Graph API erişimi kur. Kullanacağın Instagram hesabı profesyonel (Business/Creator) ve uygun Facebook Sayfası ile bağlantılı olmalı.
- Meta'nın gerekli izinleri ve erişim düzeyi sağlanmalı (`instagram_basic`, `pages_read_engagement` veya geçerli sürümdeki karşılıkları; kullanım senaryosuna göre App Review gerekebilir). Başka profesyonel hesaplar için Business Discovery erişimi gerekir.
- Kendi profesyonel Instagram hesabının **sayısal IG User ID** değerini ve **Meta Graph API access token** değerini **cihazındaki** `Kaynaklar → Meta API bağlantısı kur` ekranına kaydet. Token Android Keystore ile AES-GCM şifrelenir; uygulama Instagram şifreni toplamaz.
- `Şimdi Tara · API Testi` ile erişim durumunu kontrol et. Başarılıysa `Periyodik Taramayı Başlat` ile cihazın planlayıcısını etkinleştir. 15 dakika **alt sınırdır**, garanti edilen kontrol aralığı değildir; Android batarya optimizasyonları geciktirebilir.
- Uygulama API erişimi olmadan `@tevkiliniz` veya başka bir hesabı uzaktan izlemez. Hesabın profesyonel olup olmadığı kullanıcı tarafından henüz doğrulanmadı.

Bu kurulum son kullanıcı için zahmetlidir. Üretim sürümünde güvenilir ve kullanıcı onaylı OAuth ile token yönetimi ve sunucu kaynaklı zamanlama gerekir. **Uygulama 7/24 kesintisiz gerçek zamanlı Instagram taraması sağlamaz.** Ücretsiz bir herkesi tarama servisi olarak sunulmaz; Meta API'nin erişim/limit koşullarına bağlıdır.

## Kullanım ve gizlilik

- Kaynaklara Instagram profil bağlantısı veya `@hesap` eklenebilir; kaynaklar yerel listede saklanır.
- İzinden bağımsız API taraması, ilgili gönderi açıklamasını ve permalink'ini alır; Ankara ve belirtilen kriterlerle eşleşirse SQLite'a ekler ve Android'in **Tevkil Radarı** bildirimiyle uyarır. İlana dokununca orijinal Instagram paylaşımı açılır.
- Aynı Meta gönderisinin ID'siyle tekrar kayıt oluşturulmaz. Başvuru mesajı ancak kullanıcı onayından sonra paylaşılır.
- WhatsApp/Telegram sohbetlerini doğrudan uzaktan izlemez. Bildirim dinleyici yetkisi verilmezse bu yolla mesaj gelmez.
- Token Meta Graph API isteğine yalnızca HTTPS üzerinde `Authorization` başlığıyla gönderilir; uygulama başka sunucuya ilan ya da token göndermez. İlana ait veriler yerel veritabanında saklanır. `Ayarlar → Kayıtları Sil` ile temizlenebilir. `Kaynaklar → Meta Bağlantısı → Bağlantıyı sil` şifreli anahtarı ve kimlik bilgilerini cihazdan kaldırır.
- İzin verilmemiş hesapları kazımaya yönelik scraping, Instagram kullanıcı adı/şifre otomasyonu veya otomatik spam başvuru yapılmaz. Avukatlık meslek kuralları, müşteri bilgileri ve kişisel verilerin korunması ayrıca gözetilmelidir.

## Geliştirici testleri

```bash
mkdir -p tests/build
javac -encoding UTF-8 -d tests/build app/src/main/java/com/tevkil/radar/FilterEngine.java app/src/main/java/com/tevkil/radar/InstagramSources.java tests/FilterEngineTest.java tests/InstagramSourcesTest.java
java -cp tests/build FilterEngineTest
java -cp tests/build InstagramSourcesTest
gradle :app:assembleDebug
gradle :app:lintDebug
```

Yerel testler sadece algoritmayı doğrular; canlı Meta API erişimi ve fiziksel cihaz davranışı ancak gerekli yetkiler ve cihaz mevcut olduğunda doğrulanabilir.

Resmî kaynaklar: https://developers.facebook.com/docs/instagram-platform/instagram-graph-api/reference/ig-user/ , https://developers.facebook.com/docs/graph-api/overview/rate-limiting/ ve https://developer.android.com/reference/android/app/job/JobInfo.Builder
