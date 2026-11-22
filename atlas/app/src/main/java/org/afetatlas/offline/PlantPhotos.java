package org.afetatlas.offline;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONObject;
import java.io.InputStream;

final class PlantPhotos {
    static JSONObject metadata(Activity a,String scientific){
        try(InputStream in=a.getAssets().open("plant_photos/photo_metadata.json")){
            byte[] bytes=new byte[in.available()];
            int off=0,n;
            while(off<bytes.length&&(n=in.read(bytes,off,bytes.length-off))>0)off+=n;
            return new JSONObject(new String(bytes,0,off,"UTF-8")).optJSONObject(scientific);
        }catch(Exception ignored){return null;}
    }
    private static TextView label(Activity a,String text,int size,int color){
        TextView tv=new TextView(a);tv.setText(text);tv.setTextSize(size);
        tv.setTextColor(color);tv.setPadding(20,16,20,16);return tv;
    }
    static void show(Activity a,Plants.Species species){
        ScrollView scroller=new ScrollView(a);
        LinearLayout layout=new LinearLayout(a);
        layout.setOrientation(LinearLayout.VERTICAL);
        scroller.addView(layout);
        JSONObject metadata=metadata(a,species.scientific);
        if(metadata!=null){
            String file=metadata.optString("filename");
            try(InputStream in=a.getAssets().open("plant_photos/"+file)){
                Bitmap image=BitmapFactory.decodeStream(in);
                if(image!=null){
                    ImageView view=new ImageView(a);
                    view.setAdjustViewBounds(true);
                    view.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    view.setImageBitmap(image);
                    layout.addView(view,new LinearLayout.LayoutParams(-1,-2));
                }
            }catch(Exception ignored){}
            layout.addView(label(a,"Fotoğraf: "+metadata.optString("author","")+
                "\nLisans: "+metadata.optString("license","")+
                "\nKaynak: "+metadata.optString("source","")+
                "\nLisans URL: "+metadata.optString("license_url",""),
                11,0xff555555));
        }else{
            layout.addView(label(a,"Bu tür için lisansı ve kaynak adı doğrulanmış çevrimdışı fotoğraf bulunamadı.",13,0xff855127));
        }
        layout.addView(label(a,"Görünüm / genel bilgi\n"+species.overview,15,0xff244b3a));
        layout.addView(label(a,"Uyarı\n"+species.warning,15,0xffaa392e));
        layout.addView(label(a,"ÖNEMLİ: Fotoğraf ve açıklamalar kesin tür tanısı sağlamaz. Yabani bitki veya mantarı yalnızca bu katalogla teşhis ederek tüketme. Şüpheli tüketimde 112'yi ara.",13,0xff9a2b2b));
        new AlertDialog.Builder(a).setTitle(species.name+" — "+species.scientific)
            .setView(scroller).setPositiveButton("Kapat",null).show();
    }
}
