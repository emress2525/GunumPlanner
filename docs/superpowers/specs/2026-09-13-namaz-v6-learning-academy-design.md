# Namaz V6 Öğrenme Akademisi Tasarımı

## Amaç

Namaz V5 içindeki yüzeysel `Öğren` ekranını, dini hiç bilmeyen bir kullanıcının uygulamadan ayrılmadan temel bilgiden günlük ibadet pratiğine kadar ilerleyebileceği kapsamlı, kaynaklı ve görsel olarak güçlü bir **Namaz Akademisi**ne dönüştürmek.

Bu sürümün ana mezhep anlatımı **Hanefî** olacaktır. Hanefî–Şafiî farkı bulunan konularda fark ayrı ve görünür bir kutuda gösterilecek; ihtilaflı konu tek doğru gibi sunulmayacaktır.

## Kapsam

Bu tasarım V6 için öncelikle `Öğren` alt sistemini kapsar. Namaz vakitleri, Kur'an, kıble, ezan ve widget mevcut işlevlerini korur. Öğren ekranından bu bölümlere bağlantı verilebilir ancak kullanıcı bir derse bastığında sebepsizce başka ana ekrana atılmaz.

V6'nın hedefi “çok özellikli ama boş” bir ekran değil; her kartın gerçek, tamamlanabilir ve geri dönülebilir bir ders açtığı bir eğitim sistemi oluşturmaktır.

## Temel Kullanıcı Deneyimi

### 1. Öğren ana ekranı

Alt menüdeki `Öğren` sekmesi aşağıdaki yapıyla açılır:

- Üst bölüm: “Namaz Akademisi” başlığı, mevcut seviye, tamamlanan ders sayısı ve ilerleme yüzdesi.
- “Sıfırdan Başla” öğrenme yolu: yeni başlayan için önerilen sıralı program.
- “Konuya Göre Öğren” kütüphanesi: dersleri kategori bazında açar.
- “Bugün 5 Dakika Öğren” kartı: kısa günlük ders.
- “Kaldığın Yerden Devam Et” kartı.
- “Kaynaklar ve Mezhep Notları” bilgi alanı.

Kartlar başka ana ekranlara yönlendirilmez. Her kart `lesson detail` görünümünü açar.

### 2. Ders ekranı

Her ders şu ortak iskeleti kullanır:

1. Ders başlığı ve kategori.
2. “Bu derste ne öğreneceksin?” özeti.
3. Adım adım ana anlatım.
4. Gerekli yerde Arapça metin.
5. Türkçe okunuş.
6. Türkçe anlam.
7. Uygulamalı hareket/pozisyon açıklaması.
8. “Sık yapılan hata” kutusu.
9. “Hanefî görüş” kutusu.
10. Gerekirse “Şafiî farkı” kutusu.
11. “Kaynak” bölümü.
12. 3–5 soruluk kısa kontrol testi.
13. “Dersi tamamla” düğmesi.
14. Önceki / sonraki ders gezinmesi.

### 3. Geri davranışı

- Ders içindeyken Android geri tuşu önce ders listesinin ilgili konumuna döner.
- Öğren ana ekranındayken geri tuşu ana uygulama ekranına döner.
- Öğrenme akışı `show('knowledge')`, `show('duas')`, `show('namazHocasi')` gibi başka ekranlara zorla atılmaz.
- Kullanıcının scroll konumu ve kaldığı ders cihazda saklanır.

## Ders Mimarisi

İçerik tek dev HTML içine gömülmez. Aşağıdaki yapı kullanılır:

- `assets/academy/academy-data.js` — ders katalogları, kategoriler, ders metadata'sı.
- `assets/academy/lessons-hanafi.js` — Hanefî ana ders içeriği.
- `assets/academy/lessons-shafii-notes.js` — yalnızca fark bulunan konulardaki Şafiî notları.
- `assets/academy/academy.js` — liste, ders açma, ilerleme, test ve geri navigasyonu.
- `assets/academy/academy.css` — Öğren ekranına özel tasarım sistemi.

Amaç: mevcut `v5_upgrade.py` içindeki dev HTML/CSS stringlerini daha fazla büyütmemek ve içerik ile görünümü birbirinden ayırmak.

## Ders Kategorileri

### A. İman ve İslam'ın temelleri

- İslam nedir?
- İman nedir?
- İmanın 6 şartı.
- İslam'ın 5 şartı.
- Allah'ın sıfatlarına temel giriş.
- Peygamberlik ve Hz. Muhammed'in örnekliği.
- Ahiret, kader ve sorumluluk.
- Niyet nedir?
- Farz, vacip, sünnet, müstehap, mubah, mekruh, haram kavramları.
- 32 Farz öğretim şeması.

### B. Temizlik ve namaza hazırlık

- Taharetin anlamı.
- Necasetten temizlik.
- Abdest neden alınır?
- Abdestin farzları.
- Abdestin sünnetleri ve adabı.
- **Abdestin adım adım alınışı:** niyet, eller, ağız, burun, yüz, kollar, mesh, kulak/boyun notu, ayaklar.
- Abdesti bozan durumlar.
- Abdestte sık yapılan hatalar.
- Guslün farzları.
- Guslün adım adım alınışı.
- Guslü gerektiren durumlar.
- Teyemmümün şartları ve adımları.
- Mest üzerine mesh.
- Namaz için elbise ve yer temizliği.
- Setr-i avret.
- Vakit.
- Kıble.
- Niyet.

### C. Namazın yapısı

- Namazın dışındaki şartları.
- Namazın içindeki rükün/farzları.
- Vacipler.
- Sünnetler.
- Mekruhlar.
- Namazı bozan durumlar.
- Tekbir.
- Kıyam.
- Kıraat.
- Rükû.
- Rükûdan doğrulma.
- Secde.
- İki secde arasındaki oturuş.
- İlk oturuş.
- Son oturuş.
- Selam.
- Tâdil-i erkân.
- Huşû.

### D. Namazda okunanlar

Her metin için Arapça, Türkçe okunuş, Türkçe anlam, ne zaman okunduğu ve isteğe bağlı ses düğmesi bulunur:

- Sübhaneke.
- Eûzü-Besmele.
- Fâtiha.
- Kısa sûreler: İhlâs, Kevser, Asr, Felak, Nâs başta olmak üzere başlangıç seti.
- Rükû tesbihi.
- Rükûdan kalkış zikri.
- Secde tesbihi.
- Ettehiyyâtü.
- Allahümme Salli.
- Allahümme Bârik.
- Rabbenâ Âtinâ.
- Rabbenâğfir lî.
- Kunut dualarına ilgili yerde açıklama.

### E. Beş vakit namaz — rekât rekât

Her namaz için ayrı ders akışı oluşturulur. Kullanıcı “kaç rekât?” bilgisinin yanında **her rekâtta ne yapacağını ve ne okuyacağını** görür.

#### Sabah
- 2 rekât sünnet.
- 2 rekât farz.

#### Öğle
- İlk 4 rekât sünnet.
- 4 rekât farz.
- Son 2 rekât sünnet.

#### İkindi
- 4 rekât sünnet.
- 4 rekât farz.

#### Akşam
- 3 rekât farz.
- 2 rekât sünnet.

#### Yatsı
- İlk 4 rekât sünnet.
- 4 rekât farz.
- Son 2 rekât sünnet.
- Vitir namazı.

Her namaz dersinde:

- Rekât çizelgesi.
- Rekât numarasına göre hareket sırası.
- O rekâtta okunanlar.
- İlk/son oturuş farkı.
- Farz/sünnet ayrımı.
- “Şimdi ne yapıyorum?” görünümü.
- “Sonraki adım” düğmesi.

### F. Cemaat ve cami

- Cemaatle namazın anlamı.
- İmama uyma.
- Saf düzeni.
- Geç kalan kişinin temel davranışı.
- Cami adabı.
- Ezan ve kamet.
- Cuma namazı.
- Bayram namazına giriş.
- Cenaze namazına giriş.

### G. Özel durumlar

- Sehiv secdesi.
- Kaza namazı.
- Seferîlik.
- Hastalıkta namaz ve ima.
- Oturarak namaz.
- Namazı kaçırma durumunda temel yaklaşım.
- Kerahat vakitleri.
- Kadınların ibadetle ilgili temel özel durumları; mahrem ve hassas dil.
- Vesvese.
- Unutma/şüphe durumları.

Bu bölümde mezhep farklarının bulunduğu maddelerde Hanefî ana anlatımın hemen altında Şafiî farkı gösterilir.

### H. Oruç, zekât ve hac temelleri

V6'nın ana odağı namaz olsa da dini hiç bilmeyen kullanıcı için temel çerçeve verilir:

- Oruç nedir, kimlere farzdır, temel bozanlar.
- Ramazan ve sahur/iftar adabı.
- Zekâtın amacı ve temel şartları.
- Sadaka.
- Hac ve umrenin temel kavramları.

Bu konular detaylı fetva motoru değildir; temel eğitim seviyesinde kalır ve kaynaklara yönlendirir.

### I. Günlük hayat ve ahlak

- Anne-baba hakkı.
- Eşler ve aile sorumluluğu.
- Çocuklara davranış.
- Akrabalık bağları.
- Komşuluk.
- Arkadaşlık.
- Selamlaşma.
- Misafirlik.
- İş ahlakı.
- Ticaret ve dürüstlük.
- Borç ve emanet.
- Kul hakkı.
- Gıybet ve iftira.
- Öfke kontrolü.
- Adalet.
- İsraf.
- Mahremiyet.
- Sosyal medya adabı.
- Hasta ziyareti.
- Cenaze/taziye adabı.
- Yolculuk adabı.

## Dinî İçerik Güvenliği ve Kaynak İlkesi

Uygulama kendi başına fetva üretmez.

Öncelik sırası:

1. Kur'an ayetleri.
2. Diyanet İşleri Başkanlığı ilmihal / namaz rehberi / Din İşleri Yüksek Kurulu açıklamaları.
3. Sahih hadis kaynakları; kaynak adı ve bölüm/numara bilgisi mümkün olduğunca görünür.
4. Mezhep farklılıklarında güvenilir fıkıh/ilmihal referansı.

İçerik yazım kuralları:

- Kaynaksız kesin hüküm yazılmaz.
- İhtilaf varsa “Hanefî görüş” ve “Şafiî görüş/not” ayrılır.
- “Şu kesin haramdır/farzdır” gibi ifadeler ancak güvenilir kaynakla kullanılır.
- Sağlık, gebelik, hastalık, yolculuk gibi özel durumlarda kullanıcıya gerektiğinde yetkin din görevlisine danışma notu gösterilir.
- `Tam Namaz Hocası` kitabının telifli metni kopyalanmaz; yalnızca konu kapsamından yararlanılır ve anlatım özgün oluşturulur.

## Görsel Tasarım

### Tasarım dili

- Koyu zümrüt ana renk.
- Krem/kemik arka plan.
- Altın yalnızca vurgu için.
- Büyük, rahat okunur başlıklar.
- Arapça metinler için ayrı tipografi ve geniş satır aralığı.
- Kartlarda aşırı gölge yerine temiz sınır ve katman.
- Emoji yerine tutarlı çizgisel ikon yaklaşımı.
- Bir ekranda en fazla bir ana çağrı düğmesi.

### Öğren ana sayfası

“Dashboard kartları” yerine kurs uygulaması görünümü:

- Büyük ilerleme kartı.
- Yatay kategori çipleri.
- Dikey ders yolu.
- Tamamlanan derste tik.
- Kilit sistemi yok; kullanıcı istediği dersi açabilir.
- Önerilen sıra görsel olarak belirtilir.

### Ders sayfası

- Sticky olmayan, içeriği kapatmayan üst başlık.
- Bölüm ilerleme çizgisi.
- Adım kartları.
- Arapça blokları ayrı yüzeyde.
- Hanefî/Şafiî fark kutuları farklı ama erişilebilir vurguda.
- Kaynak kutusu ders sonunda.
- Alt menü ders sırasında içerik üzerine binmez.
- Android safe area hesaba katılır.

### Hareket anlatımı

Namaz hareketlerinde yalnızca metin kullanılmaz. Kıyam, rükû, secde ve oturuş için sade, saygılı, cinsiyet veya yüz ayrıntısına gereksiz vurgu yapmayan şematik görseller/SVG çizimleri kullanılır. Görselin altında hareketin adı ve kritik duruş notu bulunur.

## Öğrenme İlerlemesi

Cihazda şu veriler saklanır:

- Son açılan ders ID'si.
- Son scroll konumu.
- Tamamlanan ders ID'leri.
- Kategori ilerleme yüzdesi.
- Mini test sonuçları.
- Kullanıcının “daha sonra tekrar et” işaretleri.

Hesap açmak zorunlu değildir.

## Arama

Öğren bölümünde yerel arama bulunur. Kullanıcı “sehiv”, “abdest bozan”, “ikindi kaç rekât”, “gıybet” gibi terimlerle ders ve ders içi başlık bulabilir.

Arama sonuçları ilgili dersin ilgili bölümüne götürür; genel ana ekrana atmaz.

## Erişilebilirlik

- Minimum dokunma hedefi 44dp eşdeğeri.
- Metin büyütmede düzen bozulmamalı.
- Yaşlı modu: daha büyük tipografi ve tek sütun.
- Koyu tema.
- Arapça ve Türkçe metinler birbirinden görsel olarak ayrılır.
- Renk tek başına anlam taşımaz; etiket/ikon da kullanılır.

## Teknik Navigasyon

Yeni iç gezinme anahtarı:

- `academyHome()`
- `academyOpenCategory(categoryId)`
- `academyOpenLesson(lessonId, anchor?)`
- `academyBack()`
- `academyNextLesson()`
- `academyPreviousLesson()`

Ana uygulamanın `show(screenId)` fonksiyonu yalnızca Öğren sekmesine ilk giriş/çıkış için kullanılır; ders kartları `show()` ile diğer ana ekranlara gönderilmez.

Tarayıcı geçmişi benzeri yerel `academyHistory` stack'i tutulur. Android geri tuşu JavaScript bridge üzerinden önce bu stack'i tüketir.

## Hata Durumları

- Eksik ders ID'si: Öğren ana sayfasına kontrollü dönüş + “Ders bulunamadı” mesajı.
- İçerik verisi yüklenemezse: boş ekran yerine yerel hata kartı.
- Sesli içerik yoksa: düğme gizlenir veya “Ses henüz mevcut değil” mesajı gösterilir; ekran kırılmaz.
- Progress verisi bozulursa: ilerleme sıfırlanabilir fakat ders içeriği açılmaya devam eder.

## Test Stratejisi

### Otomatik davranış testleri

- Öğren ana ekranındaki her ders kartının geçerli `lessonId` açtığını doğrula.
- Hiçbir temel ders kartının `show('knowledge')`, `show('duas')` veya `show('namazHocasi')` ile başka ekrana atmadığını doğrula.
- `academyBack()` geçmiş stack'ini doğru tüketir.
- Son ders ve tamamlanma durumu local storage üzerinden geri yüklenir.
- Hanefî ana içerikli fark gerektiren derslerin Şafiî notu vardır.
- Her dersin en az bir kaynak kaydı vardır.
- Beş vakit derslerinin beklenen rekât bloklarını içerdiği doğrulanır.
- Alt güvenli alan CSS kuralları mevcuttur.

### Paket doğrulama

- Python içerik/regresyon testleri.
- Java/Android unit testleri.
- `assembleDebug` başarılı.
- `apksigner verify --verbose` başarılı.
- APK içinden `academy-data.js`, ders dosyaları ve `academy.css` varlığı doğrulanır.
- Paket adı/sürüm adı V6 olarak ayırt edilir.

### Cihaz kabul kontrolü

- Öğren sekmesine gir → ders aç → geri → aynı kategori/listede kal.
- Uzun ders sonuna kadar kaydır → alt menü içeriği kapatmıyor.
- Ekran yazı boyutunu büyüt → taşma yok.
- Koyu tema → metinler okunur.
- Beş vakitten her biri açılıyor.
- Abdest, gusül, teyemmüm ayrı dersler olarak açılıyor.
- Hanefî/Şafiî fark kutusu ilgili derslerde görünür.

## V6 Başarı Ölçütleri

V6 tamamlanmış sayılabilmesi için:

- Öğren bölümündeki hiçbir ana ders kartı kullanıcıyı sebepsizce başka modüle atmamalı.
- En az 9 ana kategori gerçek ders içeriğine sahip olmalı.
- Abdest, gusül, teyemmüm ve beş vakit namaz rekât rekât ayrıntılı olmalı.
- Namazda okunan temel dua/surelerin Arapça + okunuş + anlam alanları bulunmalı.
- Her ders kaynak göstermeli.
- İhtilaflı konularda Hanefî ana anlatım + Şafiî farkı görünür olmalı.
- CSS küçük telefon, büyük yazı ve koyu temada bozulmamalı.
- Öğren ilerlemesi ve kaldığın yer cihazda saklanmalı.
- CI testleri, Gradle derlemesi ve APK imza doğrulaması geçmeden APK teslim edilmemeli.
