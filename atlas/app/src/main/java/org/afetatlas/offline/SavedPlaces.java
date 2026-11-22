package org.afetatlas.offline;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mapsforge.core.model.LatLong;
import java.util.ArrayList;
import java.util.Locale;

final class SavedPlaces {
    private static final String PREFS="savedPlacesV1";
    interface Selection { void onPoint(LatLong location, String title); }
    private static final class Place {
        String name,comment;double lat,lon;
        Place(String n,String c,double la,double lo){name=n;comment=c;lat=la;lon=lo;}
    }
    private static int dp(Activity a,int x){return (int)(x*a.getResources().getDisplayMetrics().density+0.5f);}
    private static TextView text(Activity a,String v,int size){
        TextView t=new TextView(a);t.setText(v);t.setTextSize(size);t.setTextColor(0xff284556);
        t.setPadding(dp(a,13),dp(a,7),dp(a,13),dp(a,7));return t;
    }
    private static ArrayList<Place> load(Activity a){
        ArrayList<Place> result=new ArrayList<>();
        String json=a.getSharedPreferences(PREFS,Activity.MODE_PRIVATE).getString("places","[]");
        try{
            JSONArray data=new JSONArray(json);
            for(int i=0;i<data.length();i++){
                JSONObject x=data.optJSONObject(i);
                if(x==null)continue;
                double lat=x.optDouble("lat",Double.NaN),lon=x.optDouble("lon",Double.NaN);
                if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)continue;
                result.add(new Place(x.optString("name","Konum"),x.optString("comment",""),lat,lon));
            }
        }catch(Exception ignored){}
        return result;
    }
    private static void persist(Activity a,ArrayList<Place> places){
        JSONArray data=new JSONArray();
        try{
            for(Place p:places){
                JSONObject x=new JSONObject();
                x.put("name",p.name);x.put("comment",p.comment);x.put("lat",p.lat);x.put("lon",p.lon);
                data.put(x);
            }
        }catch(Exception e){
            new AlertDialog.Builder(a).setMessage("Kayıt hazırlanamadı: "+e.getMessage())
                .setPositiveButton("Tamam",null).show();
            return;
        }
        boolean ok=a.getSharedPreferences(PREFS,Activity.MODE_PRIVATE).edit().putString("places",data.toString()).commit();
        if(!ok)new AlertDialog.Builder(a).setMessage("Konum telefona kaydedilemedi.")
                .setPositiveButton("Tamam",null).show();
    }
    static View screen(Activity a, Selection callback){
        ArrayList<Place> places=load(a);
        LinearLayout page=new LinearLayout(a);page.setOrientation(LinearLayout.VERTICAL);
        page.addView(text(a,"Çevrimdışı Kayıtlı Noktalar",20));
        page.addView(text(a,"Konumları telefonunda sakla. Nokta veya kuş uçuşu mesafesi, yol tarifi anlamına gelmez.",13));
        Button add=new Button(a);add.setAllCaps(false);add.setText("＋ Koordinat ekle");
        page.addView(add,new LinearLayout.LayoutParams(-1,dp(a,50)));
        ScrollView scroll=new ScrollView(a);
        LinearLayout list=new LinearLayout(a);list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        Runnable[] refresh={null};
        refresh[0]=()->{
            list.removeAllViews();
            if(places.isEmpty())list.addView(text(a,"Henüz kayıt yok. Bir koordinat ekleyebilirsin.",14));
            for(int i=0;i<places.size();i++){
                final int index=i;
                Place p=places.get(i);
                Button item=new Button(a);item.setAllCaps(false);item.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
                item.setText(p.name+" • "+String.format(Locale.US,"%.5f, %.5f",p.lat,p.lon));
                list.addView(item,new LinearLayout.LayoutParams(-1,dp(a,64)));
                item.setOnClickListener(v->new AlertDialog.Builder(a).setTitle(p.name)
                    .setMessage(p.comment+"\n\nKoordinat: "+String.format(Locale.US,"%.6f, %.6f",p.lat,p.lon))
                    .setPositiveButton("Haritada göster",(d,w)->callback.onPoint(new LatLong(p.lat,p.lon),p.name))
                    .setNeutralButton("Düzenle",(d,w)->edit(a,places,index,refresh[0]))
                    .setNegativeButton("Sil",(d,w)->new AlertDialog.Builder(a)
                        .setTitle("Bu noktayı sil?").setPositiveButton("Sil",(dlg,which)->{
                            places.remove(index);persist(a,places);refresh[0].run();
                        }).setNegativeButton("Vazgeç",null).show())
                    .show());
            }
        };
        add.setOnClickListener(v->edit(a,places,-1,refresh[0]));
        refresh[0].run();return page;
    }
    static void addAt(Activity a,LatLong pos){
        ArrayList<Place> places=load(a);
        editWithCoordinate(a,places,pos);
    }
    private static void editWithCoordinate(Activity a,ArrayList<Place> places,LatLong pos){
        form(a,places,-1,()->{},pos);
    }
    private static void edit(Activity a,ArrayList<Place> list,int index,Runnable refresh){
        form(a,list,index,refresh,null);
    }
    private static void form(Activity a,ArrayList<Place> list,int index,Runnable refresh,LatLong suggestion){
        Place existing=index>=0?list.get(index):null;
        LinearLayout panel=new LinearLayout(a);panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(a,18),dp(a,4),dp(a,18),dp(a,4));
        EditText name=new EditText(a);name.setHint("Konum adı");name.setSingleLine(true);
        name.setText(existing==null?"":existing.name);
        panel.addView(name);
        EditText lat=new EditText(a);lat.setHint("Enlem (39.12345)");
        lat.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
        lat.setSingleLine(true);
        lat.setText(existing!=null?Double.toString(existing.lat):suggestion!=null?Double.toString(suggestion.latitude):"");
        panel.addView(lat);
        EditText lon=new EditText(a);lon.setHint("Boylam (32.12345)");
        lon.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
        lon.setSingleLine(true);
        lon.setText(existing!=null?Double.toString(existing.lon):suggestion!=null?Double.toString(suggestion.longitude):"");
        panel.addView(lon);
        EditText note=new EditText(a);note.setHint("Not (isteğe bağlı)");note.setSingleLine(false);
        note.setText(existing==null?"":existing.comment);
        panel.addView(note);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(index<0?"Yeni konum":"Konumu düzenle")
             .setView(panel).setPositiveButton("Kaydet",null).setNegativeButton("İptal",null).create();
        dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{
                String n=name.getText().toString().trim();
                double la=Double.parseDouble(lat.getText().toString().trim().replace(',','.'));
                double lo=Double.parseDouble(lon.getText().toString().trim().replace(',','.'));
                if(n.isEmpty()||n.length()>120||!Double.isFinite(la)||!Double.isFinite(lo)
                    ||Math.abs(la)>90||Math.abs(lo)>180)throw new IllegalArgumentException("Ad, enlem veya boylam geçersiz.");
                if(list.size()>=500&&index<0)throw new IllegalArgumentException("500 kayıt sınırına ulaşıldı");
                Place value=new Place(n,note.getText().toString().trim(),la,lo);
                if(index<0)list.add(value);else list.set(index,value);
                persist(a,list);refresh.run();dialog.dismiss();
            }catch(Exception e){
                new AlertDialog.Builder(a).setMessage(e.getMessage()).setPositiveButton("Tamam",null).show();
            }
        }));
        dialog.show();
    }
}
