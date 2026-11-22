package org.afetatlas.offline;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.LinkedHashMap;

final class FieldNotebook {
    private static final String[] LABELS = {
        "Aile buluşma noktası","Alternatif toplanma noktası","Acil irtibat kişileri",
        "Kritik ilaçlar / özel ihtiyaçlar","Evcil hayvan veya bakım planı",
        "Su ve gıda stokları","Acil ekipman ve konumları","Önemli koordinatlar",
        "Araç ve ulaşım alternatifleri","Ek saha notları"
    };
    private static TextView info(Activity a,String message,int font){
        TextView t=new TextView(a);t.setText(message);t.setTextSize(font);
        t.setTextColor(0xff243a4b);t.setPadding(20,15,20,15);return t;
    }
    static View screen(Activity activity){
        SharedPreferences prefs=activity.getSharedPreferences("fieldNotebook",Activity.MODE_PRIVATE);
        ScrollView scroll=new ScrollView(activity);
        LinearLayout list=new LinearLayout(activity);list.setOrientation(LinearLayout.VERTICAL);
        list.addView(info(activity,"Çevrimdışı Saha Defteri",21));
        list.addView(info(activity,"Bilgiler bu cihazın uygulama depolamasına yazılır; şifreli değildir. Çok hassas kişisel bilgi veya parola kaydetmeyin. Uygulamayı kaldırmak kayıtları silebilir.",13));
        LinkedHashMap<String,EditText> entries=new LinkedHashMap<>();
        for(String key:LABELS){
            list.addView(info(activity,key,15));
            EditText input=new EditText(activity);
            input.setTextSize(14f);
            input.setSingleLine(false);
            input.setMinLines(2);
            input.setMaxLines(5);
            input.setHint("Buraya yazın...");
            input.setText(prefs.getString(key,""));
            list.addView(input);
            entries.put(key,input);
        }
        Button save=new Button(activity);save.setAllCaps(false);save.setText("Notları kaydet");
        list.addView(save);
        save.setOnClickListener(v->{
            SharedPreferences.Editor e=prefs.edit();
            for(String key:entries.keySet()){
                e.putString(key,entries.get(key).getText().toString());
            }
            e.apply();
            new AlertDialog.Builder(activity).setMessage("Notlar cihazda kaydedildi.")
               .setPositiveButton("Tamam",null).show();
        });
        Button share=new Button(activity);share.setAllCaps(false);share.setText("Yedek metni paylaş");
        list.addView(share);
        share.setOnClickListener(v->{
            StringBuilder text=new StringBuilder("HAYATTA KALMA ATLASI - SAHA DEFTERİ\n\n");
            for(String key:entries.keySet())
                text.append(key).append("\n").append(entries.get(key).getText()).append("\n\n");
            new AlertDialog.Builder(activity)
              .setTitle("Şifresiz dışa aktarım")
              .setMessage("Paylaştığın metin, seçtiğin başka uygulamaya aktarılır ve kişisel bilgilerini içerebilir. Devam edilsin mi?")
              .setPositiveButton("Paylaş",(dialog,which)->{
                  Intent intent=new Intent(Intent.ACTION_SEND);
                  intent.setType("text/plain");
                  intent.putExtra(Intent.EXTRA_TEXT,text.toString());
                  activity.startActivity(Intent.createChooser(intent,"Saha defterini paylaş"));
              }).setNegativeButton("İptal",null).show();
        });
        Button clear=new Button(activity);clear.setAllCaps(false);clear.setText("Tüm notları sil");
        list.addView(clear);
        clear.setOnClickListener(v->new AlertDialog.Builder(activity)
           .setTitle("Notlar kalıcı silinsin mi?")
           .setMessage("Bu işlem geri alınamaz.")
           .setPositiveButton("Sil",(dialog,which)->{
               prefs.edit().clear().apply();
               for(EditText field:entries.values())field.setText("");
           }).setNegativeButton("Vazgeç",null).show());
        scroll.addView(list);return scroll;
    }
}
