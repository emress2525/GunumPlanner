package org.afetatlas.offline;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

final class Plants {
  static final class Species {
    final String name,scientific,overview,warning;
    final int risk;
    Species(String n,String s,String o,String w,int r){name=n;scientific=s;overview=o;warning=w;risk=r;}
  }
  static final Species[] DATA={
    new Species("Zakkum", "Nerium oleander", "Her dem yeşil çalı; dar, derimsi yapraklar ve gösterişli çiçekler.", "Zehirlidir; yaprak, çiçek ve diğer kısımları yenmez. Kalp ritmini etkileyebilir.", 2),
    new Species("Baldıran", "Conium maculatum", "Maydanozgillerden, beyaz şemsiyemsi çiçekli; yenilebilir türlerle karışabilir.", "Çok zehirlidir. Görsel benzerlikle yabani şemsiyegil tüketme.", 2),
    new Species("Güzelavrat otu", "Atropa belladonna", "Çan biçimli çiçek ve koyu renkli meyveler görülebilir.", "Meyve ve diğer bölümleri zehirlidir; çocuklar açısından yüksek risklidir.", 2),
    new Species("Porsuk", "Taxus baccata", "İğne yapraklı ağaç, kırmızı etli tohum kılıfı görülebilir.", "Tohum ve çok sayıda bitki bölümü ciddi zehirlenme yapabilir.", 2),
    new Species("Yüksük otu", "Digitalis spp.", "Boru biçimli, yüksük görünüşlü gösterişli çiçekler.", "Kalp glikozitleri nedeniyle tıbbi gözetim olmadan kullanılmaz.", 2),
    new Species("Kaplanboğan", "Aconitum spp.", "Mor-mavi çiçekli, bazı türleri yüksek rakımlarda yetişen cins.", "Çok zehirlidir; dokunma ve yanlış tüketim ciddi risk yaratabilir.", 2),
    new Species("Acı çiğdem", "Colchicum spp.", "Çiğdem benzeri çiçek, bazı türlerde yaprak ve çiçek farklı dönemde görünür.", "Kolşisin alkaloidi içerebilir; yabani soğan/yaprak diye tüketilmez.", 2),
    new Species("Boru çiçeği", "Datura stramonium", "Büyük borumsu çiçek ve dikenli tohum kapsülü.", "Tropan alkaloitleri nedeniyle zehirlidir; çay veya tütsü amacıyla kullanılmaz.", 2),
    new Species("Hint yağı bitkisi", "Ricinus communis", "İri elsi yaprak, kapsül ve desenli tohumlar.", "Tohumları ricin içerebilir; yenmesi hayati tehlike taşır.", 2),
    new Species("Banotu", "Hyoscyamus niger", "Yapışkan tüylü yapraklar ve damarlı sarımsı çiçekler.", "Zehirli alkaloitler içerebilir; tüketilmez.", 2),
    new Species("Yılanyastığı", "Arum maculatum", "Ok biçimli yaprak ve koçan benzeri çiçek yapısı.", "Çiğ dokuları tahriş edici ve toksik olabilir; yabani sebze olarak toplanmaz.", 2),
    new Species("Çöpleme", "Veratrum album", "Geniş damarlı yaprakları başka yabani otlarla karışabilir.", "Çok zehirli alkaloitler taşır; tüketilmez.", 2),
    new Species("Adam otu", "Mandragora officinarum", "Geniş yaprak rozetleri, morumsu çiçek veya meyveler.", "Zehirli tropan alkaloitleri içerebilir; tüketim ve özüt hazırlamadan kaçın.", 2),
    new Species("Sarmaşık", "Hedera helix", "Tırmanıcı, çoğu zaman parçalı her dem yeşil yapraklı.", "Meyvelerini tüketme; bazı kişilerde temas tahriş yapabilir.", 2),
    new Species("Sütleğen", "Euphorbia spp.", "Kırıldığında çoğu türde sütlü özsu çıkar; türler çok çeşitlidir.", "Özsu cilt ve göz için tahriş edici olabilir; göze temasında acil durulama gerekir.", 2),
    new Species("Siklamen", "Cyclamen spp.", "Kalp şeklinde yaprak ve geriye kıvrık taç yapraklar.", "Yumru ve diğer kısımları zehirli olabilir; yenmez.", 2),
    new Species("Sarı salkım", "Laburnum anagyroides", "Sarı, salkım hâlinde çiçek açan süs ağacı.", "Tohum ve diğer bölümleri toksiktir; yenmez.", 2),
    new Species("Acı tatlı patlıcan", "Solanum dulcamara", "Mor çiçek ve kırmızımsı meyveler görülebilir.", "Meyve ve bitki bölümleri zehirli olabilir; yenmez.", 2),
    new Species("Defne mezereon", "Daphne mezereum", "Kokulu pembe çiçek ve kırmızı meyve gösteren çalı.", "Meyve ve kabuk zehirlidir; temas da tahriş yapabilir.", 2),
    new Species("Noel gülü", "Helleborus spp.", "Kış sonu/ilkbaharda çiçeklenebilen otsu bitkiler.", "Toksik bileşikler içerebilir; yenmez, özüt hazırlama.", 2),
    new Species("Isırgan", "Urtica dioica", "Dişli karşılıklı yapraklar ve yakıcı tüyler.", "Dokunmak tahriş yapabilir. Türü kesinleştirmeden toplayıp tüketme.", 1),
    new Species("Kuşburnu", "Rosa canina", "Dikenli çalı, kırmızı-turuncu kuşburnu meyvesi.", "Diken, kirlenme ve benzer türlere dikkat; tıbbi tedavi yerine kullanma.", 1),
    new Species("Yabani kekik", "Thymus spp.", "Küçük aromatik yapraklı cüce çalılar; çok sayıda benzer tür vardır.", "Koku tür teşhisi değildir. Yoğun uçucu yağlar tahriş edebilir.", 1),
    new Species("Mercanköşk", "Origanum vulgare", "Aromatik yapraklar ve küçük pembe-mor çiçekler.", "Tür karışıklığı ve alerji riski vardır; tıbbi doz önermeyin.", 1),
    new Species("Yabani nane", "Mentha spp.", "Karşılıklı aromatik yaprak, küçük çiçek kurulları.", "Benzer türlerle karışabilir. Uçucu yağları ağızdan tedavi amaçlı alma.", 1),
    new Species("Adaçayı", "Salvia spp.", "Tüylü/aromatik yaprakları olan pek çok tür içerir.", "Bazı türlerin uçucu yağları riskli bileşikler içerebilir.", 1),
    new Species("Papatya", "Matricaria chamomilla", "Beyaz dilsi ve sarı tüpsü çiçekli papatyamsı bitki.", "Çok sayıda benzer tür vardır; alerji yapabilir, kesin tanı olmadan kullanma.", 1),
    new Species("Civanperçemi", "Achillea millefolium", "İnce parçalı yapraklar, beyaz çiçek kümeleri.", "Benzer türlerle karışır; gebelik ve alerjide risk değerlendirmesi gerekir.", 1),
    new Species("Karahindiba", "Taraxacum officinale agg.", "Dişsiz/dilimli yaprak rozetleri ve sarı çiçek başları.", "Benzer rozetli otlarla karışabilir; yol kenarı ve pestisit kirliliğine dikkat.", 1),
    new Species("Ebegümeci", "Malva sylvestris", "Yuvarlak loplu yaprak ve mor damarlı çiçekler.", "Yabani bitki toplamak için tek başına görsel benzerlik yeterli değildir.", 1),
    new Species("Sinirli ot", "Plantago major", "Geniş yaprak rozetleri ve belirgin paralel damarlar.", "Yara üzerine ham bitki sürmek enfeksiyon ve alerji riski yaratabilir.", 1),
    new Species("Sarı kantaron", "Hypericum perforatum", "Sarı çiçekli, ışığa karşı noktacıklı yapraklar görülebilir.", "Birçok ilaçla ciddi etkileşim oluşturabilir; kendi kendine tedavide kullanma.", 1),
    new Species("Ihlamur", "Tilia spp.", "Kalp biçimli yapraklar ve çiçeklerin yanında kanat biçimli brakte.", "Alerjik reaksiyon ve kirli çevrede toplanma riskini unutma.", 1),
    new Species("Kara mürver", "Sambucus nigra", "Beyaz çiçek kümeleri ve koyu meyve salkımları.", "Ham meyve ve bitkinin bazı bölümleri mide-bağırsak sorunu yapabilir.", 1),
    new Species("Böğürtlen", "Rubus fruticosus agg.", "Dikenli sürgünler ve çok taneli koyu meyve kümeleri.", "Yabani meyveyi kesin teşhis et; yıkama ve pestisit/kirlilik riskini değerlendir.", 1),
    new Species("Dut", "Morus alba", "Ağaç; farklı biçimli yapraklar ve dut benzeri çoklu meyveler.", "Benzer meyvelere yalnız görünümle güvenme; yıkama ve kirlenmeye dikkat.", 1),
    new Species("Kızılcık", "Cornus mas", "Sarı erken ilkbahar çiçekleri, uzunca kırmızı drup meyveler.", "Kesin teşhis ve hijyen önemlidir; tıbbi tedavinin yerine geçmez.", 1),
    new Species("Alıç", "Crataegus monogyna", "Dikenli çalı/ağaç, beyaz çiçek ve küçük kırmızı meyveler.", "Kalp ilaçlarıyla etkileşim riski nedeniyle bitkisel özütleri tedavi amaçlı alma.", 1),
    new Species("Nar", "Punica granatum", "Kırmızı-turuncu çiçekli çalı/ağaç, iri kabuklu meyveler.", "Yabani/ilaç etkileşimi konuları ayrı değerlendirilmelidir; kabuk özütlerinden kaçın.", 1),
    new Species("Akdeniz defnesi", "Laurus nobilis", "Derimsi aromatik yapraklı her dem yeşil ağaç.", "Zakkum ve başka süs çalılarıyla karıştırma; kesin teşhis olmadan yaprak kullanma.", 1),
    new Species("Ayı sarımsağı", "Allium ursinum", "Geniş aromatik yapraklar ve beyaz yıldız biçimli çiçekler.", "Acı çiğdem ve inci çiçeği gibi zehirli benzerleri vardır; koku yeterli değildir.", 2),
    new Species("Kuzukulağı", "Rumex acetosa", "Ok biçimli ekşimsi yapraklar olabilir.", "Oksalat içerir; böbrek hastalığında risk oluşturabilir. Tür tanısı şarttır.", 1),
    new Species("Rezene", "Foeniculum vulgare", "İnce ipliksi yapraklar, sarı şemsiyemsi çiçekler.", "Baldıranla aynı ailedeki türler çok benzer olabilir; kesin tanısız yabani toplama.", 2),
    new Species("Ardıç", "Juniperus communis", "İğne yapraklı, mavimsi kozalakçık taşıyan çalı/ağaç.", "Ardıç türleri karışabilir; bazıları toksiktir, tıbbi kullanım güvenli değildir.", 1),
    new Species("Kızılçam", "Pinus brutia", "Uzun iğne yaprak ve odunsu kozalaklarla tanınan çam türü.", "Çam reçinesi/gövdesi doğrudan yara tedavisi değildir; yangın riskini unutma.", 1),
    new Species("Laden", "Cistus creticus", "Pembe kırışık taç yapraklı, reçineli yapraklı çalı.", "Benzer türler olabilir; reçineyi cilde veya yaraya tıbbi amaçla sürme.", 1),
    new Species("Dağ çayı", "Sideritis spp.", "Tüylü, bazen sarımsı çiçekli çok sayıda dağ çayı türü.", "Tür ve koruma statüsü farklı olabilir; doğadan bilinçsiz toplama.", 1)
  };
  static View screen(Activity a){
    LinearLayout holder=new LinearLayout(a);holder.setOrientation(LinearLayout.VERTICAL);
    TextView title=new TextView(a);title.setText("47 Bitki Güvenliği Profili");title.setTextSize(20);title.setTextColor(0xff214b3f);title.setPadding(16,18,16,12);holder.addView(title);
    TextView warn=new TextView(a);warn.setText("BU KATALOG YENİLEBİLİRLİK KILAVUZU DEĞİLDİR. Fotoğraf ve açıklamayla güvenli teşhis yapılamaz. Zehirli benzerler için uzmana başvur. Şüpheli tüketimde 112.");warn.setTextColor(0xffa13022);warn.setTextSize(14);warn.setPadding(16,8,16,14);holder.addView(warn);
    EditText search=new EditText(a);search.setHint("Tür veya bilimsel ad ara...");search.setSingleLine(true);holder.addView(search);
    ScrollView scroll=new ScrollView(a);LinearLayout list=new LinearLayout(a);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);holder.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    Runnable update=()->render(a,list,search.getText().toString());
    search.addTextChangedListener(new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int st,int count,int after){}
      public void onTextChanged(CharSequence s,int st,int before,int count){update.run();}
      public void afterTextChanged(Editable e){}
    });
    update.run();return holder;
  }
  private static void render(Activity a,LinearLayout list,String query){
    list.removeAllViews();String q=query.toLowerCase(new Locale("tr","TR")).trim();int shown=0;
    for(Species sp:DATA){
       if(!(sp.name+" "+sp.scientific+" "+sp.overview).toLowerCase(new Locale("tr","TR")).contains(q))continue;
       Button item=new Button(a);item.setAllCaps(false);item.setTextSize(14);
       item.setText((sp.risk==2?"☠ ":"⚠ ")+sp.name+"  /  "+sp.scientific);
       list.addView(item);shown++;
       item.setOnClickListener(v->new AlertDialog.Builder(a).setTitle(sp.name+" ("+sp.scientific+")")
         .setMessage("Görünüm / genel bilgi:\n"+sp.overview+"\n\nUyarı:\n"+sp.warning+"\n\nÇevrimdışı bilgi notudur; kesin tür teşhisi değildir. Tüketme, yaraya sürme ya da tedavi amacıyla kullanma.").setPositiveButton("Kapat",null).show());
    }
    if(shown==0){TextView none=new TextView(a);none.setText("Aramaya uygun bitki bulunamadı.");list.addView(none);}
  }
}
