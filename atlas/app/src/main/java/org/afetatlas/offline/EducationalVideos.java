package org.afetatlas.offline;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.MediaController;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;

final class EducationalVideos {
    private static final String[][] CLIPS={
        {"acil_cagri","112 Acil Çağrı"},
        {"kanama","Şiddetli kanama"},
        {"yanik","Yanık"},
        {"cpr","Bilinç ve solunum"},
        {"deprem","Deprem"},
        {"sel","Sel"},
        {"yangin","Yangın"},
        {"serpinti","Nükleer serpinti"},
        {"su","Su güvenliği"},
        {"hipotermi","Hipotermi"}
    };
    private static int dp(Context ctx,int size){return (int)(size*ctx.getResources().getDisplayMetrics().density+0.5f);}
    private static TextView text(Activity a,String body,int size){
        TextView tv=new TextView(a);tv.setText(body);tv.setTextSize(size);
        tv.setTextColor(0xff20465e);tv.setPadding(dp(a,12),dp(a,8),dp(a,12),dp(a,8));return tv;
    }
    static View screen(Activity a){
        ScrollView scroll=new ScrollView(a);
        LinearLayout list=new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        list.addView(text(a,"10 Çevrimdışı Eğitim Videosu",20));
        list.addView(text(a,"Özgün, sessiz ve yazılı eğitim slaytlarıdır; uygulamalı ilk yardım çekimi, sertifikalı eğitim veya sağlık personeli yerine geçmez. Görseller görsel rehberdeki şemalardan bağımsızdır.",13));
        for(String[] clip:CLIPS){
            Button play=new Button(a);play.setAllCaps(false);
            play.setText("▶ "+clip[1]+" · 32 sn");
            list.addView(play,new LinearLayout.LayoutParams(-1,dp(a,55)));
            play.setOnClickListener(v->play(a,clip[0],clip[1]));
        }
        scroll.addView(list);return scroll;
    }
    private static File makeVideoFile(Activity a,String basename)throws Exception{
        if(!basename.matches("[a-z_]+"))throw new IllegalArgumentException("Geçersiz video adı");
        File outDir=new File(a.getFilesDir(),"educational_videos");
        if(!outDir.isDirectory()&&!outDir.mkdirs())
            throw new IllegalStateException("Video klasörü oluşturulamadı");
        File out=new File(outDir,basename+".mp4");
        if(out.exists()&&out.length()>1000)return out;
        File part=new File(outDir,basename+".part");
        try(InputStream in=a.getAssets().open("educational_videos/"+basename+".mp4");
            OutputStream stream=new FileOutputStream(part)){
            byte[] buff=new byte[32768];int count;
            while((count=in.read(buff))!=-1)stream.write(buff,0,count);
        }
        if(part.length()<1000)throw new IllegalStateException("Videonun içeriği eksik");
        if(out.exists())out.delete();
        if(!part.renameTo(out))throw new IllegalStateException("Video kaydedilemedi");
        return out;
    }
    private static void play(Activity a,String basename,String title){
        try {
            File file=makeVideoFile(a,basename);
            LinearLayout column=new LinearLayout(a);column.setOrientation(LinearLayout.VERTICAL);
            VideoView player=new VideoView(a);
            column.addView(player,new LinearLayout.LayoutParams(-1,dp(a,240)));
            column.addView(text(a,"Sessiz yazılı slayt videosu. Acil olaylarda 112 ve resmî yönlendirmeler önceliklidir.",12));
            AlertDialog dialog=new AlertDialog.Builder(a).setTitle(title)
                 .setView(column).setPositiveButton("Kapat",null).create();
            dialog.setOnShowListener(v->{
                player.setVideoPath(file.getAbsolutePath());
                MediaController controls=new MediaController(a);
                controls.setAnchorView(player);
                player.setMediaController(controls);
                player.setOnPreparedListener(mp->{
                    mp.setLooping(false);
                    player.start();
                });
                player.setOnErrorListener((mp,what,extra)->{
                    new AlertDialog.Builder(a).setMessage("Video oynatılamadı; cihaz codec desteğini kontrol edin.")
                        .setPositiveButton("Tamam",null).show();
                    return true;
                });
            });
            dialog.setOnDismissListener(v->player.stopPlayback());
            dialog.show();
        }catch(Exception e){
            new AlertDialog.Builder(a).setTitle("Video açılamadı")
               .setMessage(e.getMessage()).setPositiveButton("Tamam",null).show();
        }
    }
}
