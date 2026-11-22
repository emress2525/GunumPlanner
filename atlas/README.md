# Afet Atlası – Çevrimdışı Harita Derleme Denemesi

Bu, **önceki Atlas v5 APK'sının güncellenmiş hâli değildir**. Ayrı bir Android uygulamasıdır ve yerel harita okuma özelliğinin çalıştığını test etmek için hazırlanmıştır.

- Türkiye Mapsforge `.map` veya `.map` içeren ZIP'i Android dosya seçiciden içe aktarır.
- ZIP açma ve harita kopyalama arka planda gerçekleştirilir.
- Harita uygulamanın özel depolama alanından internet olmadan görüntülenir.
- Acil durumlar için temel kısa rehberler vardır. Kapsamlı bir afet veya tıp ansiklopedisi değildir.
- Harita verisi OSM katkıda bulunanları / BBBike. LGPL Mapsforge kütüphanesi.

GitHub Actions'ta APK oluşturma: `gradle -p atlas :app:assembleDebug`.
