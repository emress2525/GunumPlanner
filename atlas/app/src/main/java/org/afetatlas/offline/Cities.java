package org.afetatlas.offline;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.content.DialogInterface;
import java.util.Locale;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.ArrayList;

final class Cities {
    static final class Point {
        final String city,district;final double latitude,longitude;
        Point(String c,String d,double la,double lo){city=c;district=d;latitude=la;longitude=lo;}
    }
    interface OnSelect {void onSelected(Point location);}
    static void search(Activity a,OnSelect callback){
        ArrayList<Point> all=new ArrayList<>();
        try(BufferedReader br=new BufferedReader(new InputStreamReader(
            a.getAssets().open("turkiye_iller_ilceler.csv"),"UTF-8"))){
            br.readLine();String line;
            while((line=br.readLine())!=null){
                String[] v=line.split(";",-1);
                if(v.length<5)continue;
                try{all.add(new Point(v[0].trim(),v[1].trim(),
                    Double.parseDouble(v[3]),Double.parseDouble(v[4])));}
                catch(NumberFormatException ignored){}
            }
        }catch(Exception e){
            new AlertDialog.Builder(a).setMessage("İlçe arama verisi okunamadı: "+e.getMessage())
                .setPositiveButton("Tamam",null).show();return;
        }
        LinearLayout panel=new LinearLayout(a);
        panel.setOrientation(LinearLayout.VERTICAL);
        EditText input=new EditText(a);
        input.setSingleLine(true);
        input.setHint("İl, ilçe adı yaz (973 kayıt)");
        panel.addView(input);
        ListView results=new ListView(a);
        panel.addView(results,new LinearLayout.LayoutParams(-1,
            (int)(420*a.getResources().getDisplayMetrics().density+0.5f)));
        ArrayList<Point> matches=new ArrayList<>();
        ArrayList<String> names=new ArrayList<>();
        ArrayAdapter<String> adapter=new ArrayAdapter<>(a,
            android.R.layout.simple_list_item_1,names);
        results.setAdapter(adapter);
        Runnable filter=()->{
            matches.clear();names.clear();
            String query=input.getText().toString().trim().toLowerCase(new Locale("tr","TR"));
            for(Point p:all){
                String text=(p.city+" "+p.district).toLowerCase(new Locale("tr","TR"));
                if(!text.contains(query))continue;
                matches.add(p);names.add(p.city+" / "+p.district);
                if(matches.size()>=60)break;
            }
            adapter.notifyDataSetChanged();
        };
        AlertDialog dialog=new AlertDialog.Builder(a)
            .setTitle("Çevrimdışı il / ilçe arama")
            .setView(panel).setNegativeButton("Kapat",null).create();
        input.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){filter.run();}
            public void afterTextChanged(Editable e){}
        });
        results.setOnItemClickListener((parent,view,pos,id)->{
            if(pos>=0&&pos<matches.size()){
                Point selected=matches.get(pos);dialog.dismiss();callback.onSelected(selected);
            }
        });
        filter.run();dialog.show();
    }
    static void pick(Activity a,OnSelect callback){
        LinkedHashMap<String,ArrayList<Point>> groups=new LinkedHashMap<>();
        try(BufferedReader br=new BufferedReader(new InputStreamReader(
            a.getAssets().open("turkiye_iller_ilceler.csv"),"UTF-8"))){
            String line=br.readLine();
            while((line=br.readLine())!=null){
                String[] parts=line.split(";",-1);
                if(parts.length<5)continue;
                try{
                    Point p=new Point(parts[0].trim(),parts[1].trim(),
                        Double.parseDouble(parts[3]),Double.parseDouble(parts[4]));
                    if(!groups.containsKey(p.city))groups.put(p.city,new ArrayList<>());
                    groups.get(p.city).add(p);
                }catch(NumberFormatException ignored){}
            }
        }catch(Exception e){
            new AlertDialog.Builder(a).setTitle("İlçe verisi açılamadı")
                .setMessage(e.toString()).setPositiveButton("Kapat",null).show();return;
        }
        String[] cities=groups.keySet().toArray(new String[0]);
        new AlertDialog.Builder(a).setTitle("Türkiye: "+cities.length+" il")
           .setItems(cities,(dialog,index)->{
               String city=cities[index];
               ArrayList<Point> districts=groups.get(city);
               String[] names=new String[districts.size()];
               for(int j=0;j<names.length;j++)names[j]=districts.get(j).district;
               new AlertDialog.Builder(a).setTitle(city+" • "+names.length+" ilçe")
                 .setItems(names,(d,idx)->callback.onSelected(districts.get(idx)))
                 .setNegativeButton("Geri",(d,n)->pick(a,callback))
                 .show();
           }).setNegativeButton("Kapat",null).show();
    }
}
