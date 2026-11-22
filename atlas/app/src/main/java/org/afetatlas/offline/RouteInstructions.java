package org.afetatlas.offline;

import android.app.Activity;
import android.app.AlertDialog;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.mapsforge.core.model.LatLong;
import java.util.Locale;

/** Offline map route turn list; speech is strictly opt-in and device voice data dependent. */
final class RouteInstructions {
    interface OnSelect {void center(LatLong point,String description);}
    static String turkish(String english){
        String s=english.trim().toLowerCase(Locale.US);
        if(s.equals("start"))return "Rota başlangıcı";
        if(s.equals("destination")||s.equals("end"))return "Hedef noktaya ulaş";
        if(s.contains("sharp left"))return "Keskin sola dön";
        if(s.contains("sharp right"))return "Keskin sağa dön";
        if(s.contains("slight left"))return "Hafif sola dön";
        if(s.contains("slight right"))return "Hafif sağa dön";
        if(s.contains("keep left"))return "Soldan devam et";
        if(s.contains("keep right"))return "Sağdan devam et";
        if(s.contains("u-turn"))return "U dönüşü";
        if(s.contains("exit left"))return "Soldaki çıkışı kullan";
        if(s.contains("exit right"))return "Sağdaki çıkışı kullan";
        if(s.contains("left"))return "Sola dön";
        if(s.contains("right"))return "Sağa dön";
        if(s.contains("straight")||s.contains("continue"))return "Düz devam et";
        if(s.startsWith("take exit"))return "Çıkışı kullan: "+english;
        if(s.contains("roundabout"))return "Dönel kavşak: "+english;
        return english;
    }
    static View screen(Activity a, OnSelect select){
        LinearLayout outer=new LinearLayout(a);outer.setOrientation(LinearLayout.VERTICAL);
        TextView intro=new TextView(a);
        intro.setText("ROTA TALİMATLARI\nYalnızca hesaplanmış GPX rotasındaki dönüşler. Trafik, afet, yol kapanışı ve yasal geçiş kontrolü içermez. Otomatik sesli takip ve yeniden rota oluşturma bu sürümde yoktur.");
        intro.setTextColor(0xff273b4d);intro.setTextSize(15f);intro.setPadding(16,16,16,12);
        outer.addView(intro);
        ScrollView scroll=new ScrollView(a);
        LinearLayout list=new LinearLayout(a);list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        GPXTracks.Track track=null;
        try {track=GPXTracks.parse(GPXTracks.target(a));}
        catch(Exception ex){
            TextView text=new TextView(a);text.setText("Önce çevrimdışı rota hesapla veya talimat içeren GPX yükle.\n"+ex.getMessage());
            text.setPadding(18,15,18,15);list.addView(text);
            return outer;
        }
        if(track.turns.isEmpty()){
            TextView text=new TextView(a);
            text.setText("Bu GPX dosyasında dönüş talimatı yok. Önceden kaydedilmiş izler sadece çizgi olarak gösterilir.");
            text.setPadding(18,15,18,15);list.addView(text);
            return outer;
        }
        final TextToSpeech[] tts={null};
        final boolean[] ready={false};
        tts[0]=new TextToSpeech(a,status->{
            if(status==TextToSpeech.SUCCESS&&tts[0]!=null){
                int language=tts[0].setLanguage(Locale.forLanguageTag("tr-TR"));
                ready[0]=language>=TextToSpeech.LANG_AVAILABLE;
            }
        });
        outer.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            public void onViewAttachedToWindow(View view){}
            public void onViewDetachedFromWindow(View view){
                if(tts[0]!=null){tts[0].stop();tts[0].shutdown();tts[0]=null;}
            }
        });
        int count=0;
        for(GPXTracks.Turn turn:track.turns){
            final String instruction=turkish(turn.description);
            LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);
            Button focus=new Button(a);focus.setAllCaps(false);focus.setText((++count)+". "+instruction);
            focus.setTextSize(13f);
            row.addView(focus,new LinearLayout.LayoutParams(0,60,3));
            Button speak=new Button(a);speak.setAllCaps(false);speak.setText("Dinle");
            row.addView(speak,new LinearLayout.LayoutParams(0,60,1));
            list.addView(row);
            focus.setOnClickListener(v->select.center(turn.pos,instruction));
            speak.setOnClickListener(v->{
                if(tts[0]==null||!ready[0]){
                    new AlertDialog.Builder(a).setMessage("Bu telefonda Türkçe çevrimdışı ses paketi hazır değil. Yazılı yönlendirmeleri kullan.")
                        .setPositiveButton("Tamam",null).show();
                }else tts[0].speak(instruction,TextToSpeech.QUEUE_FLUSH,null,"atlas-turn-"+System.currentTimeMillis());
            });
        }
        return outer;
    }
}
