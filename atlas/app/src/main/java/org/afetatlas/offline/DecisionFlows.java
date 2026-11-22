package org.afetatlas.offline;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

final class DecisionFlows {
    private static final class Flow {
        final String title,question,urgent,urgentHelp,calmHelp;
        Flow(String title,String question,String urgent,String urgentHelp,String calmHelp){
            this.title=title;this.question=question;this.urgent=urgent;this.urgentHelp=urgentHelp;this.calmHelp=calmHelp;
        }
    }
    private static final Flow[] FLOWS={
        new Flow("Şiddetli kanama","Kanama doğrudan baskıya rağmen yoğun devam ediyor mu?",
            "Yaşamı tehdit eden kanama şüphesi",
            "112'yi hemen ara. Temiz bezle doğrudan ve sürekli baskıya devam et. Turnike yalnızca uygun eğitim / 112 yönlendirmesiyle uygulanmalıdır. Yaraya bitki veya toz sürme.",
            "Yarayı temiz bezle kapatıp baskıyı sürdür. Kanama yeniden başlarsa veya kişi halsizleşirse 112'yi ara. Geniş veya derin yaralar için sağlık değerlendirmesi al."),
        new Flow("Bilinç kaybı / solunum","Kişi bilinçsiz ve normal nefes almıyor mu?",
            "Solunum yok veya anormal",
            "Hemen 112'yi ara. Telefonda operatör yönlendirmesiyle kalp masajına başla. AED varsa aç ve cihazın talimatlarını izle.",
            "Kişi nefes alıyor ancak tepkisi yoksa 112'yi ara, solunumunu sürekli izle; omurga travması yoksa hava yolunu koruyacak pozisyonu değerlendir."),
        new Flow("Hava yolu tıkanması","Kişi konuşamıyor veya etkili öksüremiyor mu?",
            "Tam tıkanma ihtimali",
            "112'yi ara. Kişinin yaşına ve durumuna uygun ilk yardım manevralarını eğitim aldıysan uygula; bebek ve hamilelerde teknik farklıdır. Bayılırsa CPR yönlendirmesine geç.",
            "Kişi güçlü öksürebiliyorsa öksürmeye devam etmesini teşvik et. Belirtiler sürer veya nefes bozulursa 112."),
        new Flow("Yanık","Yanık geniş, derin, elektrik/kimyasal kaynaklı veya yüz-el-genital bölgesinde mi?",
            "Ciddi yanık şüphesi",
            "112'yi ara. Güvenli ise yanığı yaklaşık 20 dakika serin akan suyla soğut. Yapışmış kıyafeti çekme; yağ, buz, bitki veya macun sürme.",
            "Yanığı serin akan suyla yaklaşık 20 dakika soğut, temiz yapışmayan örtüyle koru. İyileşmeyen, kabarcıklı veya ağrılı yanıkta tıbbi yardım al."),
        new Flow("Karbonmonoksit","Ortamda kombi, soba veya jeneratör kaynaklı gaz maruziyeti şüphesi var mı?",
            "CO maruziyeti şüphesi",
            "İçeri girip kurtarmaya çalışma. Mümkünse hemen temiz havaya çık, 112'yi ara. Ortama yetkili ekip olmadan geri dönme.",
            "CO renksiz ve kokusuzdur. Baş ağrısı ve bulantı oluşursa temiz havaya çık ve acil değerlendirme al. Dedektör ve cihaz bakımı önemlidir."),
        new Flow("Sel","Sel suyu bulunduğun yere yükseliyor veya tahliye yolunu kapatıyor mu?",
            "Yükselen sel suyu",
            "Yüksek güvenli yere çık. Akıntıya girme, araçla geçme. 112'yi ara ve bulunduğun konumu bildir.",
            "Resmî uyarıları izle; bodrum ve su birikiminden uzak dur. Tahliye çantanı hazır tut, tehlikeli geçiş yapma."),
        new Flow("Gaz kaçağı","Gaz kokusu, tıslama ya da şüpheli kaçak belirtisi var mı?",
            "Şüpheli yanıcı gaz",
            "Elektrik düğmelerini, telefonu veya kıvılcım çıkarabilecek aletleri gaz bulunan alanda kullanma. Güvenle dışarı çık, uzaktan 112 ve gaz acil hattını ara.",
            "Cihaz ve bağlantıları kontrol için yetkili servise başvur. Kaçak şüphesini küçümseme, kapalı ortamda kendin onarım deneme."),
        new Flow("Hipotermi","Kişinin bilinci değişmiş, koordinasyonu bozulmuş veya ciddi soğuk maruziyeti var mı?",
            "Ağır hipotermi şüphesi",
            "112'yi ara. Kişiyi nazikçe, mümkün olduğunca az hareket ettir; rüzgâr ve ıslaklıktan koru, gövdeyi yavaş ısıt.",
            "Islak giysiyi kuru giysiyle değiştir, zemin ve rüzgârdan yalıt, sıcak bir ortama geç. Kötüleşirse acil yardım al."),
        new Flow("İnme","Yüz kayması, tek kolda güçsüzlük veya konuşma bozulması var mı?",
            "İnme belirtisi olabilir",
            "Hemen 112'yi ara, belirtinin başlangıç saatini not et. Yiyecek, içecek veya aspirin verme. Belirtiler geçse bile bekleme.",
            "Belirtiler net olmasa da ani denge, görme veya bilinç değişikliği varsa 112'yi ara. İnmede gecikme tehlikelidir."),
        new Flow("Nöbet","Nöbet beş dakikadan uzun sürdü mü, art arda oldu mu veya yaralanma var mı?",
            "Acil nöbet durumu",
            "112'yi ara. Kişiyi çevredeki sert cisimlerden koru, tutup bastırma; ağzına bir şey sokma. Solunumu izle.",
            "Süreyi takip et ve kişinin yanında kal. İlk nöbet, gebelik, solunum sorunu veya hastalık şüphesinde sağlık değerlendirmesi gerekir."),
        new Flow("Kimyasal sızıntı","Kimyasal buhar, döküntü ya da maruziyet belirtisi var mı?",
            "Tehlikeli madde riski",
            "Temas etme; rüzgâr yönü ve yetkililerin sığınma/tahliye talimatına göre güvenli alana geç. 112'yi ara ve maddenin adını bildir.",
            "Ambalaj ve uyarı levhalarını güvenli mesafeden incele. Yetkili kurum talimatı olmadan döküntüyü temizleme."),
        new Flow("Orman yangını","Duman veya alev bulunduğun bölgeyi tehdit ediyor mu?",
            "Yakın yangın tehdidi",
            "112'yi ara. Yetkili tahliyesine uy ve güvenli yoldan uzaklaş. Duman koridorundan, kuru ot ve ağaçlık geçişten kaçın.",
            "Duyuruları izle, acil çantanı hazırla. Yangın riski olan bölgede ateş yakma, tahliye rotanı önceden belirle."),
        new Flow("Zehirlenme","Kişi madde yuttu mu, soludu mu veya bilinç/solunum bozukluğu var mı?",
            "Olası zehirlenme",
            "112'yi ara. Kusturma, ev yapımı panzehir verme. Ambalajı, madde adını ve maruziyet zamanını sağlık ekibine bildir. Önce kendi güvenliğini sağla.",
            "Maddeye temas etme; güvenlik bilgi formunu ve yerel sağlık yönlendirmesini izle. Belirti çıkmasını beklemeden danış."),
        new Flow("Nükleer serpinti","Dışarıda olma veya radyoaktif tozla temas ihtimali var mı?",
            "Olası radyoaktif serpinti",
            "Mümkünse sağlam binaya gir, penceresiz iç oda veya bodrumda kal. Dış kıyafeti çıkarıp ayrı tut, cildi yıka. Resmî ölçüm ve tahliye talimatlarını izle; kendi başına iyot kullanma.",
            "Güvenli iç mekânda kalıp yetkili duyurularını izle. Kaynatmanın radyoaktif kirlenmeyi giderdiğini varsayma."),
        new Flow("Sıcak çarpması","Bilinç değişikliği, bayılma veya ciddi aşırı ısınma var mı?",
            "Sıcak çarpması acili",
            "112'yi ara. Kişiyi serin yere al ve mümkün olan hızlı güvenli soğutmayı başlat. Bilinçsiz kişiye su verme.",
            "Gölgeye geç, serinle, güvenilir su tüket. Baş dönmesi ya da kötüleşme varsa tıbbi yardım al."),
        new Flow("Deprem / hasarlı bina","Bulunduğun bina hasarlı mı, yangın/gaz riski ya da artçı tehlikesi var mı?",
            "Yapısal veya ikincil tehlike",
            "Güvenliyse hasarlı binadan uzaklaş. Asansör kullanma. Toplanma alanına git, 112 veya yetkililere bilgi ver; binaya geri girme.",
            "Artçı sarsıntılara hazır ol. Ağır eşyalar ve camlardan uzak dur, binanın güvenliği hakkında resmî/uzman değerlendirmeyi esas al.")
    };
    private static TextView text(Activity a,String value,int size){
       TextView t=new TextView(a);t.setText(value);t.setTextSize(size);
       t.setTextColor(0xff243b4d);t.setPadding(20,18,20,18);return t;
    }
    private static Button button(Activity a,String label){
       Button b=new Button(a);b.setAllCaps(false);b.setText(label);b.setTextSize(15f);return b;
    }
    static View menu(Activity a){
       ScrollView scroll=new ScrollView(a);
       LinearLayout list=new LinearLayout(a);list.setOrientation(LinearLayout.VERTICAL);
       list.addView(text(a,"16 ACİL DURUM KARAR AKIŞI",20));
       list.addView(text(a,"Önce kendi güvenliğini sağla. Acil durumda 112. Bunlar tanı veya sertifikalı ilk yardım eğitimi değildir.",13));
       for(Flow f:FLOWS){Button b=button(a,"➜ "+f.title);
           list.addView(b,new LinearLayout.LayoutParams(-1,64));
           b.setOnClickListener(v->show(a,f,0));
       }
       scroll.addView(list);return scroll;
    }
    private static void show(Activity a,Flow f,int stage){
       final String title=stage==0?f.title:stage==1?"ACİL YARDIM":"YAPILACAKLAR";
       final String body=stage==0?f.question:stage==1?f.urgentHelp:f.calmHelp;
       android.app.AlertDialog.Builder dlg=new android.app.AlertDialog.Builder(a)
          .setTitle(title).setMessage(body);
       if(stage==0){
          dlg.setPositiveButton("EVET",(dialog,which)->show(a,f,1))
             .setNegativeButton("HAYIR",(dialog,which)->show(a,f,2))
             .setNeutralButton("Kapat",null);
       }else {
          dlg.setPositiveButton("Başa dön",(dialog,which)->show(a,f,0))
             .setNegativeButton("Kapat",null);
       }
       dlg.show();
    }
}
