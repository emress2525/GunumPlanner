package org.afetatlas.offline;
import java.util.ArrayList;
import java.util.List;
final class FinalGuides {
 static List<Guides.Entry> all(){
  List<Guides.Entry> out=new ArrayList<>();
  out.add(new Guides.Entry("Deprem","Asansörde mahsur kalma","Asansörde sıkışırsan kapıyı zorlamadan acil çağrı düğmesini kullan. Kabinin kat arasında olabileceğini unutma. Yetkili ekip gelmeden çıkmaya çalışma."));
  out.add(new Guides.Entry("Ulaşım güvenliği","Bisikletle afet güzergâhı","Bisiklet rotası kapalı, enkazlı veya yoğun araç trafiği altında olabilir. Kask, görünürlük ve fren durumunu kontrol et. Otoyol veya bisiklete yasak yola yönlendiren harita verisine uyma; güvenli ve yasal alternatif bul."));
  out.add(new Guides.Entry("Ulaşım güvenliği","Araba ile tahliye","Yakıt durumunu ve resmî yol kapanışlarını kontrol et. Sel suyuna, dumanın yoğun olduğu alana veya kapalı yola girme. Tahliye yollarını kurtarma araçları için açık bırak."));
  out.add(new Guides.Entry("Ulaşım güvenliği","Toplu taşımada acil çıkış","Tren ve otobüste personel yönlendirmesine uy. Elektrik, ray veya karşı şerit tehlikesi olmadan rastgele araçtan çıkma. Acil cam ve kapı mekanizmasının yerini önceden öğren."));
  out.add(new Guides.Entry("Sağlık ve hijyen","Solunum yolu duman maruziyeti","Duman altında kalmış kişi güvenli temiz havaya alınmalıdır; kendi güvenliğini riske atma. Solunum sıkıntısı, ses kısıklığı, öksürük veya bilinç değişikliğinde 112'yi ara. Solunum şikâyeti sonradan kötüleşebilir."));
  out.add(new Guides.Entry("Kırsal güvenlik","Hayvan barınaklarında afet","Ahır ve hayvan barınaklarında devrilme, elektrik ve panikleyen hayvan tehlikesi olabilir. Kurtarma planını eğitimli personelle koordine et. İnsan güvenliği sağlanmadan riskli yapıya girme."));
  out.add(new Guides.Entry("Kırsal güvenlik","Tarım ilacı döküntüsü","Etiketi bilinmeyen veya dökülmüş pestisite çıplak elle dokunma. Su kaynağına karışmış olabileceğini düşün; kaynatma güvenli çözüm değildir. Yetkili zehir danışma ve acil ekipten yönlendirme al."));
  out.add(new Guides.Entry("Çocuk güvenliği","Çocuğun kaybolmasını önleme","Kalabalık tahliyede çocuğa güvenilir buluşma ve iletişim noktasını öğret. Mahremiyeti koruyan irtibat kartını çantasında bulundur. Ayrılık halinde yetkililere son konumu ve zamanı hızla bildir."));
  out.add(new Guides.Entry("Haberleşme","Telefon şebekesi yoksa","112 araması bazı durumlarda başka şebekeden yapılabilir ama garanti değildir. Güvenli alanda konum ve sinyal dene; pil tüketimini kontrol et. Birine hareket planını bildirmeden riskli araziye girme."));
  out.add(new Guides.Entry("Afet sonrası güvenlik","Patlama sonrası ikinci tehlike","Patlama sonrasında yangın, elektrik, gaz veya yapı çökmesi devam edebilir. Olay yerine yaklaşma; güvenli mesafeden 112'yi ara. Yetkililer izin vermedikçe geri dönme."));
  return out;
 }
}