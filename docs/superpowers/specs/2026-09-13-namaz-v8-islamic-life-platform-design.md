# Namaz V8 — İslami Yaşam ve Öğrenme Platformu Tasarım Şartnamesi

Tarih: 2026-09-13
Durum: Kullanıcı tarafından sohbet içinde onaylanan mimari tasarım
Hedef sürüm: Namaz V8
Önceki temel: Namaz V7

## 1. Amaç

Namaz V8, mevcut V7’nin namaz vakti, ezan, kıble, Kur’an/meal, Namaz Akademisi, namaz takibi ve ayrı widget temellerini koruyarak uygulamayı modüler, offline-first, kaynak şeffaflığı yüksek bir İslami yaşam ve öğrenme platformuna dönüştürür.

Ana hedefler:

- Dini bilgisi çok az olan bir kullanıcının sıfırdan öğrenebilmesi.
- Günlük ibadetlerini takip etmek isteyen kullanıcının sade bir arayüzle hızlı iş görmesi.
- Namaz, Kur’an, dua, zikir, ilmihal, Ramazan, Hac/Umre, takvim ve öğrenme özelliklerinin tek uygulamada bulunması ancak ana ekranın kalabalıklaşmaması.
- Dini içerikte kaynak, mezhep farkı ve belirsizliklerin açıkça gösterilmesi.
- Reklamsız, hesap zorunluluğu olmayan ve hassas verileri varsayılan olarak cihazda tutan bir deneyim.
- Kritik işlevlerin, özellikle namaz vakti ve ezanın, diğer modüllerden bağımsız ve güvenilir çalışması.

## 2. Ürün ilkeleri

### 2.1 Sadelik + derinlik

Uygulama iki görünüm sunar:

- Basit Mod: Bugün, Namaz, Kur’an ve Kıble odaklı sade kullanım.
- Tam Mod: öğrenme, ibadet araçları, ilmihal, dua, zikir, Ramazan, Hac/Umre, takvim ve diğer gelişmiş modüller.

Kullanıcı modlar arasında istediği zaman geçebilir. Özellikler kilitlenmez; sadece görünüm ve öneri sırası değişir.

### 2.2 Kişiye göre başlangıç

İlk açılışta “Seni nasıl hazırlayayım?” akışı gösterilir. Dört profil bulunur:

1. Dini sıfırdan öğrenmek istiyorum.
2. Namaza yeni başladım.
3. Kur’an öğrenmek istiyorum.
4. Sadece günlük ibadetlerimi takip edeceğim.

Bu seçim, ana ekrandaki kartları ve öğrenme rotasını kişiselleştirir; uygulama işlevlerini kısıtlamaz.

### 2.3 Kaynak şeffaflığı

Dini içerik, kullanıcıya “uygulama söyledi = kesin hüküm” algısı vermemelidir. İçeriklerde mümkün olduğunda şu alanlar bulunur:

- kaynak,
- konu,
- mezhep,
- görüş ayrılığı varsa açıklama,
- kişisel fetva gerekip gerekmediği.

Hanefî anlatım ana metin olabilir; Şafiî farkları açık ve ayrı etiketlenir.

## 3. Rakiplerden alınacak ve kaçınılacak desenler

V8 tasarımı Diyanet/e-Diyanet, Muslim Pro, Athan, Pillars, Salam App, Quran Majeed, Quranly, Tarteel, Kıblem, Muslim App, Ezan Vakti Pro ve benzeri namaz/Kur’an uygulamalarındaki ortak güçlü ve zayıf desenlerden türetilmiştir.

Alınacak güçlü taraflar:

- Diyanet: güvenilir içerik ekosistemi, ilmihal ve rehber bütünlüğü.
- Pillars/Salam: sade, reklamsız ve gizlilik odaklı yaklaşım.
- Athan: günlük ibadet hedefleri ve alışkanlık takibi.
- Quran Majeed/Quranly: kelime odaklı okuma, hatim ve hedef sistemi.
- Tarteel: sesli ezber yardımcısı fikri; sonuçları kesin hüküm olarak sunmadan.
- Kıblem: offline ses, kıble kalibrasyonu, tilavet seçenekleri.
- Ezan Vakti Pro/Muslim App: akıllı saat, namazdayım/cemaat modu ve widget odaklı hızlı kullanım.

Kaçınılacak zayıflıklar:

- reklam ve premium baskısının ibadet deneyimini bölmesi,
- ana ekrana aşırı bilgi yığılması,
- küçük/okunmaz widget yazıları,
- arka plan/bildirim sorunlarının kullanıcıya açıklanmaması,
- AI yanıtlarının kaynak göstermeden dini hüküm gibi sunulması,
- yanlış ses eşleşmesini “kesin hata” olarak değerlendiren ezber sistemleri.

## 4. Ana gezinme

V8 beş ana merkeze ayrılır:

1. Bugün
2. Kur’an
3. Öğren
4. İbadet
5. Daha Fazla

Bir modül içindeki alt akış kullanıcıyı başka ana sekmeye fırlatmamalıdır. Örnek: Öğren → Abdest → Farzlar → Mini Test akışı Öğren sekmesinde kalır. Android geri tuşu önce modül geçmişini, sonra ana sekmeyi yönetir.

## 5. Bugün ekranı

Bugün ekranı sade bir günlük ibadet panelidir.

Ana bileşenler:

- Sıradaki Namaz kartı: namaz adı, saat, kalan süre, şehir, hesaplama yöntemi ve Hicrî tarih.
- 5 vakit şeridi: Sabah, Öğle, İkindi, Akşam, Yatsı; Güneş bilgi olarak ayrı gösterilir.
- Günlük akış kartları: namaz takibi, Kur’an hedefi, zikir hedefi, öğrenme dersi, dini gün uyarısı.
- Günün ayeti, hadisi ve duası; her birinde kaynak ve detay bağlantısı.
- “Bugün Öğren” kartı; kullanıcının profiline göre değişen kısa ders.

Kartlar kullanıcı tarafından gizlenebilir veya yeniden sıralanabilir.

## 6. Namaz sistemi

### 6.1 Vakitler

- Otomatik şehir/konum seçimi ve her zaman erişilebilir manuel şehir seçimi.
- Türkiye için varsayılan Diyanet uyumlu yapı.
- Alternatif hesaplama yöntemleri.
- Hanefî/Şafiî ikindi tercihi.
- Her vakte manuel dakika düzeltmesi.
- Şehir, hesap yöntemi ve düzeltmelerin widget, kilit ekranı ve Wear OS ile aynı veri kaynağından gelmesi.

### 6.2 Ezan ve uyarılar

Her vakit için ayrı seçenekler:

- kapalı,
- sadece bildirim,
- kısa ses,
- tam ezan.

Ön uyarılar: 5, 10, 15 veya 30 dakika önce.

Ezan ekranı:

- Durdur,
- 5 dk sonra hatırlat,
- bildirim ayarına git.

### 6.3 Namaz takibi

Sade mod: kıldım/kılmadım.

Detaylı mod isteğe bağlı olarak:

- vaktinde,
- cemaatle,
- kazaya kaldı

alanlarını destekler.

Kaza sistemi, kullanıcının kendi girdiği sayıları takip eder. Uygulama kendi başına “şu kadar namaz borcun var” hesabı üretmez.

### 6.4 Cemaat modu

İsteğe bağlı olarak namaz süresince telefonu sessize alır ve süre sonunda eski ses ayarına döndürür.

### 6.5 Seyahat

Konum/şehir değiştiğinde kullanıcıya vakitleri güncelleme önerisi gösterilir. Uygulama kullanıcıyı otomatik “seferî” ilan etmez; gerekli kriterleri kaynaklı biçimde açıklar.

## 7. Bildirim Sağlık Merkezi

Tek ekranda kontrol edilir:

- bildirim izni,
- exact alarm izni,
- pil optimizasyonu,
- arka plan kısıtlaması,
- DND erişimi,
- otomatik başlatma,
- üretici/OEM kısıtlamaları.

Her kontrol yeşil/sarı/kırmızı durumla sunulur. Kullanıcı test bildirimi ve gecikmeli test ezanı çalıştırabilir.

## 8. Kur’an Pro

### 8.1 Görünümler

- Mushaf görünümü.
- Ayet görünümü.

İsteğe bağlı katmanlar:

- Türkçe meal,
- tefsir,
- kelime kelime anlam,
- transliterasyon.

### 8.2 Kelime detayları

Bir kelimeye dokununca:

- Arapça kelime,
- okunuş,
- Türkçe anlam,
- kök bilgisi,
- Kur’an’daki diğer kullanımlar,
- tek kelime seslendirme

gösterilebilir.

### 8.3 Ses

- birden fazla okuyucu,
- ayet/sayfa/sure oynatma,
- hız,
- A-B tekrar,
- tekrar sayısı,
- otomatik sonraki ayet,
- sure veya paket bazlı offline indirme,
- depolama yönetimi.

### 8.4 Not, yer imi ve geçmiş

Ayet işlemleri:

- Kaydet,
- Not Ekle,
- Etiketle,
- Paylaş,
- Ezbere Ekle,
- Hatime Ekle.

Son 10 okuma konumu tutulur. Günlük/haftalık okuma ve hatim ilerlemesi gösterilebilir.

### 8.5 Arama

Arama şu alanlarda çalışır:

- ayet metni,
- meal,
- konu,
- sure,
- ayet numarası,
- kelime kökü.

Sonuçta eşleşme nedeni gösterilir.

## 9. Hatim ve hedef sistemi

Hazır planlar:

- 30 gün,
- 60 gün,
- 90 gün,
- özel tarih,
- Ramazan hatmi.

Kaçırılan günlerde kalan hedef “kalan günlere yeniden dağıt” ile yeniden hesaplanabilir. Dil suçlayıcı değildir; devamı teşvik eder.

## 10. Tajvid ve ezber

### 10.1 Tajvid

- isteğe bağlı tajvid renkleri,
- renk/kural açıklamaları,
- mahreç görselleri,
- sesli örnekler.

Tajvid Akademisi sırası:

harf mahreçleri → med → nun sakin/tenvin → mim sakin → kalkale → ra/lam hükümleri → vakıf/ibtida.

### 10.2 Ezber modları

- Dinle ve tekrar et.
- Bakmadan oku.
- Kelime gizleme.
- Devamını getir.
- Karışık tekrar.

Ayet durumu:

- öğreniyorum,
- tekrar gerekiyor,
- sağlam.

### 10.3 Sesli ezber yardımcısı

Mikrofon ile ayet üzerinde ilerlemeyi takip etmeye çalışır. Şüpheli kelimeler “kontrol et” olarak işaretlenir. Sistem, özellikle mahreç/tajvid konusunda sonucu kesin doğru/yanlış hükmü olarak sunmaz.

## 11. Çocuk / yeni başlayan Kur’an modu

Büyük yazı, az seçenek ve tek görev yaklaşımı kullanılır. Öğrenme sırası:

harf → ses → ayırt et → kısa alıştırma → kelime → kısa ayet → kısa sure.

## 12. Öğren Akademisi

### 12.1 Öğrenme yolu

Dini sıfırdan öğrenen kullanıcı için önerilen sıra:

İman temelleri → temizlik → abdest → namaz → namazda okunanlar → beş vakit → günlük dualar → Kur’an okumaya giriş → ahlak/günlük hayat → oruç → zekât → hac → ileri ilmihal.

### 12.2 Ders şablonu

Bir ders mümkün olduğunda şu bloklardan oluşur:

- konu nedir,
- neden önemlidir,
- hükmü,
- adım adım uygulama,
- sık yapılan hatalar,
- Hanefî görüşü,
- Şafiî farkı,
- özel durumlar,
- gerçek hayat senaryosu,
- kısa özet,
- kaynaklar,
- mini test.

### 12.3 Namaz Öğreniyorum

Her namaz rekât bazında gösterilir. Kullanıcı şu görünüm seçeneklerine sahip olur:

- Sadece hareketler,
- Sadece okunanlar,
- Tam rehber.

Beş vakte ek olarak vitir, cuma, bayram, cenaze, teravih, teheccüd, istihare, hacet, şükür, seferî, hasta, kaza ve cemaat namazları için dersler bulunur.

### 12.4 Namazda okunanlar

Sübhaneke, Fâtiha, kısa sureler, Ettehiyyatü, Salli-Barik, Rabbena duaları, kunut ve tesbihat için:

- Arapça,
- okunuş,
- anlam,
- ses,
- kelime takibi,
- ezber modu.

### 12.5 Elif-Bâ

Sıra:

harfler → başta/ortada/sonda şekiller → üstün/esre/ötre → cezm → şedde → tenvin → med harfleri → birleşik okuma → kelime → kısa ayet → kısa sure.

### 12.6 İlmihal kütüphanesi

Ana konular:

İman, temizlik, namaz, oruç, zekât, hac, kurban, adak, aile, evlilik, boşanma temel kavramları, helal-haram, yeme-içme, giyim, ticaret, borç, faiz, kul hakkı, mirasın temel prensipleri, cenaze, hastalık, seferîlik, kadınlara özel hükümler, gençlik, sosyal medya ve modern hayat.

### 12.7 Temel paketler

- 32 Farz,
- İmanın 6 şartı,
- İslam’ın 5 şartı,
- Allah’ın sıfatları,
- namazın 12 farzı,
- ef‘âl-i mükellefîn,
- dini terimler sözlüğü.

### 12.8 Gerçek hayat senaryoları

Örnekler:

- iş yerinde namaz,
- okulda namaz,
- uçakta namaz,
- ameliyat sonrası namaz,
- vardiyalı çalışma,
- misafirlikte abdest,
- seyahatte oruç,
- borç verme,
- sosyal medya ahlakı.

## 13. Dua, zikir ve Esmaül Hüsna

### 13.1 Dua Merkezi

Kategoriler arasında sabah-akşam, namaz sonrası, uyku, yolculuk, hastalık, sıkıntı, şükür, aile, çocuk, rızık, borç, korku, cenaze, yağmur, istihare ve Hac/Umre bulunur.

Her duada:

- Arapça,
- okunuş,
- anlam,
- ses,
- kaynak,
- ne zaman okunur

alanları bulunur.

### 13.2 Zikir

- hazır zikirler,
- kullanıcıya özel zikir,
- hedef,
- titreşim,
- geçmiş,
- ekran kapalı kullanım,
- sıralı tesbihat.

### 13.3 Esmaül Hüsna

Her isim için Arapça, okunuş, anlam, açıklama, ilgili Kur’an kullanımı ve ses. Ayrıca ezber/karışık tekrar modu.

## 14. Hadis ve Güvenilir Bilgi Merkezi

### 14.1 Hadis

Hadis etiketi taşıyan her içerikte kaynak zorunludur. Mümkün olduğunda eser, bölüm ve güvenilirlik bilgisi gösterilir. Kaynağı belirsiz sosyal medya sözü “hadis” olarak sunulmaz.

### 14.2 Bilgi Merkezi

Kullanıcı doğal dille soru sorabilir. Yanıt şablonu:

- Kısa cevap,
- Hanefî görüşü,
- Şafiî farkı varsa,
- Dayanak/kaynak,
- Detaylı açıklama,
- Kişisel fetva gerekir mi?

## 15. Kaynaklı dini asistan

Asistanın cevap üretme sırası:

1. doğrulanmış yerel/uzak bilgi tabanında arama,
2. ayet/hadis/ilmihal/fetva kaynağını bulma,
3. mezhep farkını kontrol etme,
4. sade açıklama üretme,
5. kaynakları kullanıcıya gösterme.

Kaynak yoksa asistan bunu açıkça söyler ve hüküm uydurmaz.

Cevap durum etiketleri:

- Kaynaklı,
- Mezhebe göre değişebilir,
- Kişisel fetva gerekir.

“Doğrusunu Kontrol Et” işlevi, kullanıcının internette gördüğü dini sözü ayet/hadis kaynaklarında kontrol etmeye yardımcı olur.

## 16. Ramazan Merkezi

- imsak/iftar ve geri sayım,
- sahur/iftar hatırlatmaları,
- oruç günlüğü,
- mukabele/hatim planı,
- Kur’an hedefi,
- teravih takibi,
- günlük dua ve öğrenme,
- fitre/fidye/zekât için kaynaklı rehber.

Ramazan katmanı ana ekranı tamamen değiştirmez; isteğe bağlı zenginleştirir.

## 17. Hac ve Umre

Adım adım yolculuk rehberi:

ihram hazırlığı → niyet → telbiye → tavaf → tavaf namazı → sa’y → tıraş/kısaltma → ihramdan çıkış; Hac için Mina, Arafat, Müzdelife, taşlama, kurban ve ilgili aşamalar dahil edilir.

Offline Hac/Umre paketi indirilebilir.

## 18. Kadınlara özel ibadet modu

Tamamen isteğe bağlı ve varsayılan olarak cihaz içidir.

- regl döneminde namaz takibini duraklatma,
- oruç/kaza kayıtlarını ayrı tutma,
- hayız, nifas, istihaza, gusül ve ibadet konularında kaynaklı öğrenme.

## 19. Cami ve takvim

### 19.1 Yakındaki camiler

Konum izniyle:

- isim,
- mesafe,
- navigasyon,
- favori

gösterilir. Doğrulanmamış cemaat/etkinlik saatleri resmi bilgi gibi gösterilmez.

### 19.2 Takvim

Hicrî + Miladî takvim, Ramazan, bayramlar, kandiller ve önemli dini günleri gösterir. Bir güne dokununca anlamı ve kaynaklı kısa bilgi gösterilir. Kullanıcı kişisel ibadet hatırlatıcıları ekleyebilir.

## 20. İbadet Yolculuğum

Hazır rotalar:

- Namaza başlamak,
- Kur’an okumayı öğrenmek,
- Ramazan’a hazırlanmak,
- Umre’ye hazırlanmak,
- Temel dini bilgileri öğrenmek.

Günlük küçük görevler ve ilerleme takibiyle modüller ortak bir hedef etrafında birleştirilir.

## 21. Ayarlar ve erişilebilirlik

Ayar bölümleri:

- Namaz ve ezan,
- Kur’an,
- Öğren,
- Görünüm,
- Widget ve kilit ekranı,
- Gizlilik ve veriler,
- Yedekleme,
- Erişilebilirlik.

Görünüm seçenekleri:

- Standart,
- Büyük Yazı,
- Yaşlı Modu,
- Koyu tema.

Arapça ve Türkçe yazı boyutları ayrı ayarlanabilir.

## 22. Gizlilik ve veri ilkeleri

- Hesap zorunlu değildir.
- Reklam SDK’sı kullanılmaz.
- Davranış takibi varsayılan değildir.
- Namaz kayıtları, zikir, notlar, öğrenme ilerlemesi ve kadınlara özel veriler varsayılan olarak cihazda tutulur.
- Konum sadece vakit/kıble/cami ihtiyacında istenir.
- Mikrofon sadece sesli ezber/telaffuz işlevi kullanılırken istenir.

## 23. Yedekleme

Yerel şifreli yedek dosyası oluşturulur. Kullanıcı içeriği seçebilir:

- namaz kayıtları,
- Kur’an yer imleri ve notlar,
- hatim ilerlemesi,
- Akademi ilerlemesi,
- zikirler,
- ayarlar.

Hassas kayıtlar için “yedeklemeye dahil et” ayrı bir seçimdir.

İsteğe bağlı bulut yedekleme daha sonra eklenebilir; hesap zorunluluğu getirmez.

## 24. Widget Stüdyosu ve kilit ekranı

V7’nin 7 ayrı widget’ı korunur. Widget Stüdyosu ek olarak şu parçaları seçilebilir hale getirir:

- sıradaki namaz,
- geri sayım,
- tüm vakitler,
- Hicrî tarih,
- ayet,
- hadis,
- Kur’an hedefi,
- namaz takibi,
- zikir hedefi.

Kullanıcı boyut, yazı büyüklüğü ve açık/koyu/şeffaf arka plan seçer.

Kilit ekranında cihaz native widget destekliyorsa widget kullanılır. Desteklemiyorsa sessiz, public görünür namaz durumu bildirimi kullanılır. Uzun ayet metni kilit ekranına zorla taşınmaz.

## 25. Wear OS

Saat uygulaması sade tutulur:

- sıradaki namaz,
- kalan süre,
- tüm vakitler,
- kıble,
- namaz takibi,
- basit tesbih.

Komplikasyonlarda kısa veri gösterilir, örneğin “Akşam 19:21” veya “1s 12dk”.

## 26. Teknik mimari

V8, mevcut WebView yamalarını büyütmek yerine native ve modüler mimariye taşınır.

Önerilen teknoloji:

- Kotlin,
- Jetpack Compose,
- Room,
- DataStore,
- WorkManager,
- AlarmManager,
- Media3,
- App Widgets/Glance,
- Wear OS companion katmanı.

Önerilen modüller:

- core-prayer
- core-quran
- core-learning
- core-content
- core-settings
- feature-today
- feature-prayer
- feature-quran
- feature-learn
- feature-worship
- feature-tools
- feature-widget
- feature-wear

Tek veri kaynağı ilkesi uygulanır. Örnek:

PrayerRepository → Bugün → Namaz → widget → kilit ekranı → Wear OS.

QuranRepository → okuyucu → arama → ezber → hatim.

Bir modülün çökmesi kritik alarm/ezan katmanını etkilememelidir.

## 27. Offline-first davranış

İnternetsiz durumda temel uygulama kullanılabilir olmalıdır.

Cihazda bulunacak çekirdek veriler:

- namaz vakti cache’i,
- Kur’an metni,
- en az bir temel Türkçe meal,
- Akademi çekirdek dersleri,
- temel dualar,
- Esmaül Hüsna,
- temel ilmihal içeriği.

İnternet ağırlıklı işler:

- ek ses paketleri,
- harita/cami verisi,
- içerik paketi güncellemeleri,
- kaynaklı asistanın güncel sorguları.

## 28. İçerik güncelleme sistemi

Dini içerik paketleri sürümlenir. Örnek:

- İlmihal 2026.09
- Dua 2026.10

Paket güncellemesinde değişiklik notu bulunur. Yanlış veya değişmiş dini bilgi sessizce değiştirilmez.

## 29. Dini içerik doğrulama süreci

Her içerik türü için zorunlu doğrulamalar:

### Kur’an

- sure/ayet numarası,
- Arapça metin bütünlüğü,
- meal kaynağı,
- tefsir kaynağı.

### Hadis

- eser/kaynak,
- mümkünse bölüm/numara,
- güvenilirlik bilgisi mevcutsa gösterim.

### İlmihal/fetva

- kaynak,
- mezhep,
- görüş ayrılığı,
- kişisel fetva gerektiren alanların işaretlenmesi.

Kaynağı belirsiz sosyal medya içerikleri ana bilgi tabanına alınmaz.

## 30. Test stratejisi

### 30.1 Namaz vakti

- farklı şehirler,
- farklı mevsimler,
- gece yarısı,
- saat dilimi değişimi,
- şehir değişimi,
- manuel düzeltme,
- Diyanet karşılaştırması.

### 30.2 Ezan

- uygulama açık,
- uygulama kapalı,
- ekran kilitli,
- cihaz yeniden başlamış,
- pil tasarrufu,
- bildirim izni yok,
- exact alarm kısıtlı.

Bir vakit hatası sonraki vakitlerin zincirini bozmamalıdır.

### 30.3 Kıble

- sensör yok,
- sensör zayıf,
- manyetik bozulma,
- kalibrasyon ihtiyacı.

### 30.4 Kur’an

- mushaf/ayet görünümü,
- arama,
- meal/tefsir,
- ses,
- offline paket,
- yer imi/not,
- hatim,
- ezber,
- son okuma konumu.

### 30.5 Öğren

- ders → alt konu → mini test → kaynak → geri → kaldığın yer,
- Android geri tuşu,
- eski “Öğren ekranından başka yere atma” hatasının regresyon testi.

### 30.6 Widget ve kilit ekranı

- 7 hazır widget,
- Widget Stüdyosu,
- farklı launcher boyutları,
- büyük yazı,
- veri güncelleme,
- keyguard destekli ve bildirime fallback senaryoları.

### 30.7 Performans

- ana ekran ağ beklemeden açılmalı,
- Kur’an sayfaları akıcı olmalı,
- büyük ses paketleri ana APK’ya gömülmemeli,
- kritik servislerin başlangıcı gereksiz gecikmemeli.

### 30.8 Erişilebilirlik

- büyük yazıda kesilme/çakışma olmamalı,
- dokunma alanları yeterli olmalı,
- ekran okuyucu etiketleri bulunmalı,
- renk tek başına bilgi taşımamalı.

## 31. APK kabul kriterleri

V8 APK teslim edilmeden önce aşağıdakilerin tümü doğrulanmalıdır:

- otomatik testler yeşil,
- Android APK gerçekten derlenmiş,
- APK imzası doğrulanmış,
- paket adı ve sürüm doğru,
- Bugün, Namaz, Kur’an, Öğren, İbadet ve Daha Fazla ana akışları mevcut,
- widget provider’ları manifestte kayıtlı,
- alarm/ezan servisleri manifestte kayıtlı,
- çekirdek offline Kur’an/meal/öğrenme paketi mevcut,
- navigasyon ve Android geri regresyonları geçmiş,
- APK arşiv bütünlüğü geçerli.

Fiziksel telefonda üreticiye özel davranışlar kullanıcı veya gerçek cihaz test matrisiyle ayrıca doğrulanır; CI tek başına fiziksel cihaz davranışını garanti etmez.

## 32. Uygulama sırası

V8 tek bir dev değişiklik olarak uygulanmaz. Önerilen fazlar:

1. Native platform ve veri katmanı.
2. Namaz + ezan + Bugün.
3. Kur’an Pro + offline içerik.
4. Öğren Akademisi + Elif-Bâ + ilmihal.
5. Dua/zikir/Esma/hadis/Bilgi Merkezi.
6. Ramazan + Hac/Umre + seyahat + kadınlara özel mod + takvim/camiler.
7. Widget Stüdyosu + kilit ekranı + Wear OS.
8. Kaynaklı asistan.
9. Kalite, performans, erişilebilirlik ve release doğrulaması.

Her faz tamamlandığında önceki çalışan işlevler için regresyon testleri koşulur.

## 33. Kapsam dışı veya sonraya bırakılanlar

İlk V8 release hedefinde zorunlu değildir:

- sosyal ağ/arkadaş ekleme,
- kullanıcıların halka açık dini içerik üretip yayınlaması,
- reklam sistemi,
- zorunlu üyelik,
- finansal ödeme/premium sistemi,
- kullanıcı yerine otomatik fetva veren kesin hüküm motoru,
- sağlık veya hukuki kararları dini asistanla otomatikleştirme.

## 34. Başarı tanımı

V8 başarılı sayılırsa:

- yeni başlayan kullanıcı “nereden başlayacağını” bilir,
- günlük kullanıcı namaz vakitlerine ve ibadet takibine birkaç dokunuşla erişir,
- Kur’an okuyucu tek başına ayrı bir güçlü ürün kalitesine ulaşır,
- dini içerik kaynak/mezhep şeffaflığıyla sunulur,
- internet kesilse bile temel işlevler çalışır,
- reklam veya hesap zorunluluğu ibadet deneyimini bölmez,
- widget/kilit ekranı/Wear OS aynı namaz veri kaynağını kullanır,
- uygulama büyüdükçe modüller birbirini kırmadan geliştirilebilir.
