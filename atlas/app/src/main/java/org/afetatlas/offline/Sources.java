package org.afetatlas.offline;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class Sources {
    static final String[][] SOURCES={
    {"Deprem","Deprem Öncesi, Anı ve Sonrası Önlemler","AFAD","https://afad.gov.tr/deprem-oncesi-ani-ve-sonrasi-alabileceginiz-onlemleri-biliyor-musunuz","Çök–Kapan–Tutun, bina güvenliği ve afet sonrası adımlar"},
    {"Afet hazırlığı","Afet ve Acil Durum Çantası Nasıl Hazırlanmalı?","AFAD","https://www.afad.gov.tr/afet-ve-acil-durum-cantasi-nasil-hazirlanmali","Kişiye ve aileye uygun afet hazırlığı"},
    {"İlk yardım","2024 AHA and American Red Cross Guidelines for First Aid","American Heart Association / Red Cross","https://cpr.heart.org/en/resuscitation-science/2024-first-aid-guidelines","Kanama, inme, yanık ve ilk yardım genel ilkeleri"},
    {"İlk yardım","2024 First Aid Guidelines — Top Things to Know","American Heart Association","https://professional.heart.org/en/science-news/2024-aha-and-american-red-cross-guidelines-for-first-aid/top-things-to-know","İlk yardım bulgularının profesyonel özeti"},
    {"İçme suyu","How to Make Water Safe in an Emergency","CDC","https://www.cdc.gov/water-emergency/about/","Mikrobiyal arıtma, kimyasal kirlenmede sınırlar"},
    {"İçme suyu","Household Water Treatment","CDC","https://www.cdc.gov/global-water-sanitation-hygiene/about/about-household-water-treatment.html","Kaynatma, filtreleme ve dezenfeksiyonun sınırları"},
    {"Radyasyon","What to Do in Radiation Emergencies","CDC","https://www.cdc.gov/radiation-emergencies/response/","Sağlam binaya girme, içeride kalma, yetkilileri takip etme"},
    {"Radyasyon","Potassium Iodide","CDC","https://www.cdc.gov/radiation-emergencies/treatment/potassium-iodide.html","Potasyum iyodür yalnızca ilgili yetkililerin yönlendirmesiyle"},
    {"Su güvenliği","Drinking Water Advisories: An Overview","CDC","https://www.cdc.gov/water-emergency/about/drinking-water-advisories-an-overview.html","Su kirlenmesi uyarılarının türüne göre hareket etme"},
    {"Harita","OpenStreetMap Contributors and ODbL","OpenStreetMap","https://www.openstreetmap.org/copyright","Harita veri kaynağı, atıf ve ODbL"},
    {"Harita","BBBike OpenStreetMap Extracts","BBBike","https://data.bbbike.org/osm/region/europe/turkey/","Türkiye Mapsforge harita veri paketi"},
    {"Rota","BRouter","BRouter Contributors","https://github.com/abrensch/brouter","Yerel rota motoru, lisans ve profil sınırlamaları"}
    };
    static View screen(Activity a){
       ScrollView scroll=new ScrollView(a);
       LinearLayout body=new LinearLayout(a);
       body.setOrientation(LinearLayout.VERTICAL);
       TextView intro=new TextView(a);
       intro.setText("KAYNAKÇA VE GÜVENİLİRLİK SINIRLARI\nBu sayfa çevrimdışı okunabilir. Linkler yalnızca internet varsa açılır. Kaynaklar genel referans niteliğindedir: 273 rehberin klinik, botanik veya teknik uzman onayından geçtiği anlamına gelmez. Hayati tehlikede 112; yerel resmî uyarılar daima önceliklidir.");
       intro.setTextSize(15f);intro.setTextColor(0xff173c56);
       intro.setPadding(20,18,20,16);body.addView(intro);
       for(String[] e:SOURCES){
          TextView tv=new TextView(a);
          tv.setText(e[0]+" — "+e[1]+"\n"+e[2]+"\n"+e[4]+"\n"+e[3]);
          tv.setAutoLinkMask(android.text.util.Linkify.WEB_URLS);
          tv.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
          tv.setTextSize(14f);tv.setTextColor(0xff24465e);
          tv.setPadding(20,10,20,14);body.addView(tv);
       }
       scroll.addView(body);return scroll;
    }
}
