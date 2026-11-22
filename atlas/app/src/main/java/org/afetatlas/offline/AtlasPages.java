package org.afetatlas.offline;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Locale;

final class AtlasPages {
    private static final String[] CHECKS={
        "Su: kişi başına uygun miktar ve sağlam kaplar", "Dayanıklı gıda, konserve açacağı",
        "İlk yardım çantası ve eldiven", "Kişisel ilaçlar ve reçete kopyaları",
        "El feneri, yedek pil ve düdük", "Şarjlı powerbank ve kablolar",
        "Battaniye, mevsime uygun giysi", "Hijyen malzemeleri ve çöp poşeti",
        "Kimlik / sigorta / kritik belge kopyası", "Nakit, anahtar ve acil iletişim kartı",
        "Bebek, yaşlı ve engelli ihtiyaçları", "Evcil hayvan mama ve taşıma ihtiyacı",
        "AFAD toplanma alanını önceden öğren", "Aile buluşma noktalarını belirle",
        "Gaz ve elektrik kesicilerinin yerini öğren", "Acil tahliye rotalarını belirle",
        "Ağır dolap ve rafları sabitle", "Acil çantanın tarihlerini denetle",
        "Türkiye haritasını önceden yükle", "Uçak modundayken haritayı açıp test et"
    };
    private static int dp(Activity a,int n){return (int)(n*a.getResources().getDisplayMetrics().density+.5f);}
    private static TextView text(Activity a,String s,int size){
        TextView t=new TextView(a);t.setText(s);t.setTextColor(0xff193247);t.setTextSize(size);
        t.setPadding(dp(a,13),dp(a,9),dp(a,13),dp(a,9));return t;
    }
    static View guides(Activity a){
        SharedPreferences prefs=a.getSharedPreferences("atlasPrefs",Activity.MODE_PRIVATE);
        LinearLayout shell=new LinearLayout(a);shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(0xfff5f6f8);
        shell.addView(text(a,"AFET ATLASI · "+Guides.count()+" REHBER",20));
        shell.addView(text(a,"112 acil çağrı. Rehber eğitim içindir; resmî ve tıbbi talimatların yerini tutmaz.",12));
        EditText search=new EditText(a);search.setSingleLine(true);
        search.setHint("Başlık, konu veya içerikte ara");search.setTextSize(15f);
        shell.addView(search);
        TreeSet<String> all=new TreeSet<>();
        for(Guides.Entry e:Guides.all())all.add(e.category);
        ArrayList<String> cats=new ArrayList<>();cats.add("Tüm konular");cats.addAll(all);
        LinearLayout controls=new LinearLayout(a);
        Spinner spinner=new Spinner(a);
        ArrayAdapter<String> ad=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,cats);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);spinner.setAdapter(ad);
        controls.addView(spinner,new LinearLayout.LayoutParams(0,dp(a,50),1));
        Button favorites=new Button(a);favorites.setText("☆ Favoriler");favorites.setAllCaps(false);
        controls.addView(favorites,new LinearLayout.LayoutParams(0,dp(a,50),1));
        shell.addView(controls);
        ScrollView scroll=new ScrollView(a);
        LinearLayout list=new LinearLayout(a);list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        final boolean[] onlyFavorites={false};
        Runnable update=()->render(a,list,search.getText().toString(),
            spinner.getSelectedItem()==null?"Tüm konular":spinner.getSelectedItem().toString(),
            onlyFavorites[0],prefs);
        search.addTextChangedListener(new TextWatcher(){
           public void beforeTextChanged(CharSequence s,int start,int count,int after){}
           public void onTextChanged(CharSequence s,int start,int before,int count){update.run();}
           public void afterTextChanged(Editable e){}
        });
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
           public void onItemSelected(AdapterView<?> parent,View v,int pos,long id){update.run();}
           public void onNothingSelected(AdapterView<?> parent){}
        });
        favorites.setOnClickListener(v->{
            onlyFavorites[0]=!onlyFavorites[0];
            favorites.setText(onlyFavorites[0]?"★ Favoriler açık":"☆ Favoriler");
            update.run();
        });
        update.run();return shell;
    }
    private static void render(Activity a,LinearLayout list,String query,String category,
                               boolean onlyFav,SharedPreferences prefs){
        list.removeAllViews();int found=0;
        String q=query.toLowerCase(new Locale("tr","TR")).trim();
        for(Guides.Entry entry:Guides.all()){
            boolean saved=prefs.getBoolean("f_"+entry.title,false);
            if(onlyFav&&!saved)continue;
            if(!"Tüm konular".equals(category)&&!category.equals(entry.category))continue;
            String data=entry.category+" "+entry.title+" "+entry.body;
            if(!data.toLowerCase(new Locale("tr","TR")).contains(q))continue;
            Button b=new Button(a);b.setAllCaps(false);b.setTextSize(14f);
            b.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
            b.setText((saved?"★ ":"")+"["+entry.category+"] "+entry.title);
            list.addView(b,new LinearLayout.LayoutParams(-1,dp(a,56)));
            b.setOnClickListener(v->{
                boolean old=prefs.getBoolean("f_"+entry.title,false);
                new AlertDialog.Builder(a).setTitle(entry.title)
                    .setMessage(entry.category+"\n\n"+entry.body+
                        "\n\nBu bilgi acil servis, ilk yardım eğitimi veya resmî talimatların yerini tutmaz.")
                    .setPositiveButton("Kapat",null)
                    .setNeutralButton(old?"★ Favoriden çıkar":"☆ Favoriye ekle",
                        (dialog,which)->{
                            prefs.edit().putBoolean("f_"+entry.title,!old).apply();
                            render(a,list,query,category,onlyFav,prefs);
                        }).show();
            });found++;
        }
        if(found==0)list.addView(text(a,"Aramana uyan rehber bulunamadı.",15));
    }
    static View checklist(Activity a){
        SharedPreferences p=a.getSharedPreferences("atlasPrefs",Activity.MODE_PRIVATE);
        ScrollView scroll=new ScrollView(a);
        LinearLayout list=new LinearLayout(a);list.setOrientation(LinearLayout.VERTICAL);
        list.addView(text(a,"Afet Hazırlık Kontrol Listesi",20));
        list.addView(text(a,"İşaretler telefonunda kalıcı olarak saklanır.",12));
        for(int i=0;i<CHECKS.length;i++){
            final int idx=i;
            CheckBox cb=new CheckBox(a);cb.setText(CHECKS[i]);cb.setTextSize(15f);
            cb.setPadding(dp(a,12),dp(a,7),dp(a,12),dp(a,7));
            cb.setChecked(p.getBoolean("check_"+i,false));
            cb.setOnCheckedChangeListener((v,checked)->
               p.edit().putBoolean("check_"+idx,checked).apply());
            list.addView(cb);
        }
        scroll.addView(list);return scroll;
    }
}
