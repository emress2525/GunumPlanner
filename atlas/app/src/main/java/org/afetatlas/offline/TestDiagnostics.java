package org.afetatlas.offline;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.util.Locale;

/** Manual phone-test checklist and copyable, privacy-conscious diagnostics.
 * Does not collect GPS coordinates, contacts, IMEI or any identifying user data.
 */
final class TestDiagnostics {
    private static final String[] CHECKS={
        "Uygulama açılıyor / çökme yok",
        "273 rehber açılıyor / metin araması çalışıyor",
        "47 bitki sayfası ve lisanslı görseller açılıyor",
        "10 videodan birkaçını internetsiz oynatabildim",
        "81 il ve ilçe araması / koordinat seçimi çalışıyor",
        "Türkiye haritası indirildi ve açıldı",
        "Telefonu uçak moduna alınca harita çalışıyor",
        "GPS izni / konum işaretçisi çalışıyor",
        "Nokta kaydetme, düzenleme ve yeniden açma çalışıyor",
        "Rota ağı indirildi, yürüyüş rotası hesaplandı",
        "Bisiklet ve araç rota hesaplandı",
        "Dönüş talimatları / Türkçe ses çalışıyor",
        "Kontrol listesi ve saha defteri kalıcı kaydediyor"
    };
    private static int dp(Activity a,int px){return (int)(px*a.getResources().getDisplayMetrics().density+.5f);}
    private static TextView text(Activity a,String value,int size){
        TextView t=new TextView(a);t.setText(value);t.setTextSize(size);t.setTextColor(0xff244258);
        t.setPadding(dp(a,14),dp(a,8),dp(a,14),dp(a,8));return t;
    }
    private static String report(Activity a){
        StringBuilder out=new StringBuilder();
        out.append("HAYATTA KALMA ATLASI — TEST RAPORU\n");
        out.append("Android: ").append(Build.VERSION.SDK_INT).append("\n");
        out.append("Cihaz: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        try{
            String version=a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName;
            out.append("Atlas sürümü: ").append(version).append("\n");
        }catch(Exception ignored){out.append("Atlas sürümü: ?\n");}
        File map=new File(a.getFilesDir(),"turkiye.map");
        out.append("Mapsforge haritası: ").append(map.exists()?"var, "+(map.length()/1048576)+" MiB":"yüklenmedi").append("\n");
        File seg=OfflineRouter.segments(a);
        File[] tiles=seg.listFiles((dir,name)->name.endsWith(".rd5"));
        out.append("Rota ağı .rd5 dosyaları: ").append(tiles==null?0:tiles.length).append("\n");
        File gpx=GPXTracks.target(a);
        out.append("GPX izi: ").append(gpx.isFile()?"var":"yok").append("\n");
        out.append("Rehber sayısı: ").append(Guides.count()).append("\n");
        out.append("Bitki profili: ").append(Plants.DATA.length).append("\n");
        try{
            int count=0;
            for(String name:a.getAssets().list("plant_photos"))if(name.endsWith(".jpg")||name.endsWith(".png"))count++;
            out.append("Fotoğraf dosyası: ").append(count).append("\n");
        }catch(Exception ex){out.append("Fotoğraf dosyası: okunamadı\n");}
        try{
            int count=0;
            for(String name:a.getAssets().list("educational_videos"))if(name.endsWith(".mp4"))count++;
            out.append("Video dosyası: ").append(count).append("\n");
        }catch(Exception ex){out.append("Video dosyası: okunamadı\n");}
        out.append("GPS izni: ").append(a.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED?"verilmiş":"verilmemiş").append("\n");
        out.append("Boş uygulama depolaması: ").append(a.getFilesDir().getUsableSpace()/1048576).append(" MiB\n");
        SharedPreferences prefs=a.getSharedPreferences("atlasTestChecklist",Activity.MODE_PRIVATE);
        out.append("\nTELEFONDA DENEME SONUÇLARI\n");
        for(int i=0;i<CHECKS.length;i++)
            out.append(prefs.getBoolean("c"+i,false)?"[OK] ":"[ ] ").append(CHECKS[i]).append("\n");
        out.append("\nHata gözlemi: (buraya hata metnini ve tekrarlama adımlarını yaz)\n");
        return out.toString();
    }
    static View screen(Activity a){
        ScrollView scroll=new ScrollView(a);
        LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body);
        body.addView(text(a,"TELEFONDA DENEME / HATA RAPORU",19));
        body.addView(text(a,
            "Bu listeyi sırayla test et. Çalışanları işaretle; en alttan raporu kopyalayıp sohbetimize yapıştır. Rapora GPS koordinatın, telefon numaran veya kişiler eklenmez.",
            13));
        SharedPreferences prefs=a.getSharedPreferences("atlasTestChecklist",Activity.MODE_PRIVATE);
        for(int i=0;i<CHECKS.length;i++){
            final int pos=i;
            CheckBox cb=new CheckBox(a);
            cb.setText((i+1)+". "+CHECKS[i]);
            cb.setTextSize(14f);
            cb.setPadding(dp(a,12),dp(a,6),dp(a,10),dp(a,6));
            cb.setChecked(prefs.getBoolean("c"+i,false));
            cb.setOnCheckedChangeListener((v,checked)->prefs.edit().putBoolean("c"+pos,checked).apply());
            body.addView(cb);
        }
        Button copy=new Button(a);copy.setAllCaps(false);
        copy.setText("Tanılama raporunu kopyala");
        body.addView(copy);
        copy.setOnClickListener(v->{
            ClipboardManager clipboard=(ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Atlas Test Raporu",report(a)));
            new AlertDialog.Builder(a).setMessage("Rapor kopyalandı. ChatGPT sohbetine yapıştırabilirsin.")
                 .setPositiveButton("Tamam",null).show();
        });
        Button share=new Button(a);share.setAllCaps(false);share.setText("Raporu başka uygulamayla paylaş");
        body.addView(share);
        share.setOnClickListener(v->{
            Intent intent=new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT,report(a));
            a.startActivity(Intent.createChooser(intent,"Test raporu paylaş"));
        });
        Button reset=new Button(a);reset.setAllCaps(false);reset.setText("İşaretlenen testleri sıfırla");
        body.addView(reset);
        reset.setOnClickListener(v->new AlertDialog.Builder(a)
            .setMessage("Deneme işaretlerini sıfırlamak istiyor musun?")
            .setPositiveButton("Sıfırla",(dialog,which)->{
                prefs.edit().clear().apply();
                for(int i=0;i<body.getChildCount();i++){
                    View child=body.getChildAt(i);
                    if(child instanceof CheckBox)((CheckBox)child).setChecked(false);
                }
            }).setNegativeButton("İptal",null).show());
        return scroll;
    }
}
