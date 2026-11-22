# Hayatta Kalma Atlası – Android 0.2 beta (çevrimdışı)

**Durum:** Android kaynak kodu ve GitHub Actions APK derlemesi. Bu uygulama eski v5 APK'sının birleşimi değildir; kapsamlı bitmiş ürün olarak sunulmamalıdır.

## İşlevler

- 113 Türkçe çevrimdışı bilgi başlığı (ilk yardım, afet, su-gıda, doğada kalma, bitki güvenliği, hazırlık)
- Metin içinde arama, kategori filtreleri, kalıcı favoriler
- Kalıcı 20 maddelik afet hazırlık kontrol listesi
- Türkiye'nin **81 ili ve 973 ilçesi** için veri dosyasından seçip haritaya gitme (ilçe merkezleri yaklaşık)
- Mapsforge ile çevrimdışı Türkiye yolu/patika/yerleşim haritası görüntüleme
- BBBike'dan yaklaşık 450 MB Türkiye haritasını uygulama içinden **tek sefer** indirme ve ZIP'ten açma
- Alternatif: telefonun dosya seçicisiyle Mapsforge .map veya .map içeren ZIP içe aktarma
- GPS ile **harita merkezini** son bilinen veya uydu koordinatına taşıma (işaretçi yok)

## Sınırlar ve test

- Derleme ve APK arşiv kontrolü GitHub Actions'ta yapılır; gerçek cihazda ekran, indirme ve harita içe aktarma testi henüz doğrulanmamıştır.
- Çevrimdışı **yol tarifi, rota hesaplama, dönüş talimatları, yer adı araması** yoktur. İl/ilçe koordinat seçimi, haritada merkezlemeye yarar.
- Tarihsel Atlas v5 içeriklerini taşıma, yüzlerce detaylı tıbbi/bitkisel fotoğraf, görsel ilk yardım animasyonları ve video dersler yoktur.
- Harita dosyası ilk kurulumda internet veya telefon içindeki ZIP gerektirir; **harita dosyası yüklendikten sonra harita ve rehber çevrimdışı çalışır**.
- İlk yardım içeriği genel bilgilendirmedir, hekim incelemesinden geçmiş klinik protokol değildir. Acil durumlarda Türkiye'de 112, AFAD ve diğer yetkili kurum talimatları önceliklidir.
- Harita verisi kullanımı güncellik garantisi vermez; yol kapanışı veya canlı afet uyarısı göstermez.

## Lisans ve veriler

- İl/ilçe koordinatları: AEKHer/il-ilce-koordinat (MIT), lisans metni assets/NOTICE.txt
- Harita verileri: OpenStreetMap katkıda bulunanları (ODbL) ve BBBike
- Mapsforge kütüphanesi (LGPL)
- Uygulama içerikleri otomatik olarak resmî tıbbi kaynaklardan güncellenmez

## Derleme

```bash
gradle -p atlas :app:assembleDebug
```

GitHub Actions, build çıktılarını ZIP bütünlüğü ile doğrulayarak atlas-dist/Afet_Atlasi_Harita_Deneme.apk konumuna kopyalar.
