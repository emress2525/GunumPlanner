package org.afetatlas.offline;

import android.app.Activity;
import android.app.AlertDialog;
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
