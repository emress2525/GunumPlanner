package org.afetatlas.offline;

import android.content.Context;
import org.mapsforge.core.model.LatLong;
import btools.router.RoutingContext;
import btools.router.RoutingEngine;
import btools.router.OsmNodeNamed;
import btools.router.OsmTrack;
import btools.router.FormatGpx;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.List;
import java.util.Set;

/** Locally embedded BRouter 1.7.10, MIT license, rd5 data format with lookup version 11.
 *  Core computes a candidate route on OSM roads/paths after routing segments are downloaded.
 *  A Mapsforge .map rendering file alone is NOT sufficient for offline routing.
 */
final class OfflineRouter {
    static final String SOURCE="https://brouter.de/brouter/segments4/";
    static final String[] PROFILES={"trekking","hiking-mountain","car-vario"};
    interface Progress { void message(String msg); }
    static File base(Context ctx){ return new File(ctx.getFilesDir(),"brouter"); }
    static File segments(Context ctx){ return new File(base(ctx),"segments4"); }
    static File profiles(Context ctx){ return new File(base(ctx),"profiles2"); }

    static void setup(Context ctx) throws Exception {
        File dir=profiles(ctx);
        if(!dir.isDirectory()&&!dir.mkdirs())throw new IllegalStateException("Rota profil klasörü oluşturulamadı");
        File seg=segments(ctx);
        if(!seg.isDirectory()&&!seg.mkdirs())throw new IllegalStateException("Rota veri klasörü oluşturulamadı");
        for(String name:new String[]{"lookups.dat","trekking.brf","hiking-mountain.brf","car-vario.brf"}){
            File dst=new File(dir,name);
            if(dst.exists()&&dst.length()>100)continue;
            try(InputStream in=ctx.getAssets().open("brouter/"+name);
                OutputStream out=new FileOutputStream(dst)){
                byte[] buf=new byte[32768];int n;
                while((n=in.read(buf))!=-1)out.write(buf,0,n);
            }
        }
        try(BufferedReader r=new BufferedReader(new InputStreamReader(new FileInputStream(new File(dir,"lookups.dat")),StandardCharsets.UTF_8))){
            if(!"---lookupversion:11".equals(r.readLine()))
                throw new IllegalStateException("Rota verisi sürümü uyumsuz: BRouter lookup v11 bekleniyor");
        }
    }
    static List<String> allTurkeyTiles(){
        ArrayList<String> names=new ArrayList<>();
        // Turkey bbox, including adjacent-edge tiles for northern/eastern routing.
        for(int lat=35;lat<=40;lat+=5)
          for(int lon=25;lon<=45;lon+=5) names.add(name(lon,lat));
        return names;
    }
    static List<String> aroundRoute(LatLong start,LatLong end){
        if(!valid(start)||!valid(end))throw new IllegalArgumentException("Geçersiz rota koordinatı");
        // Select the contiguous grid covering start to end, with a small corridor.
        double laMin=Math.min(start.latitude,end.latitude)-0.12;
        double laMax=Math.max(start.latitude,end.latitude)+0.12;
        double loMin=Math.min(start.longitude,end.longitude)-0.12;
        double loMax=Math.max(start.longitude,end.longitude)+0.12;
        if(laMin<34||laMax>44||loMin<24||loMax>47)
            throw new IllegalArgumentException("Şu anki rota veri paketi yalnızca Türkiye çevresini kapsıyor");
        int minLat=(int)Math.floor(laMin/5.0)*5,maxLat=(int)Math.floor(laMax/5.0)*5;
        int minLon=(int)Math.floor(loMin/5.0)*5,maxLon=(int)Math.floor(loMax/5.0)*5;
        Set<String> names=new LinkedHashSet<>();
        for(int lat=minLat;lat<=maxLat;lat+=5)
          for(int lon=minLon;lon<=maxLon;lon+=5) names.add(name(lon,lat));
        if(names.size()>22)throw new IllegalArgumentException("Rota çok uzun; parçalar hâlinde hesapla");
        return new ArrayList<>(names);
    }
    private static boolean valid(LatLong p){return p!=null&&Double.isFinite(p.latitude)&&Double.isFinite(p.longitude)
        &&Math.abs(p.latitude)<=90&&Math.abs(p.longitude)<=180;}
    private static String name(int lon,int lat){
        return (lon<0?"W"+(-lon):"E"+lon)+"_"+(lat<0?"S"+(-lat):"N"+lat)+".rd5";
    }
    static List<String> missing(Context ctx,List<String> names){
        ArrayList<String> need=new ArrayList<>();
        for(String item:names){
            File target=new File(segments(ctx),item);
            if(!target.isFile()||target.length()<1024)need.add(item);
        }
        return need;
    }
    static void download(Context ctx,List<String> names,Progress progress) throws Exception {
        setup(ctx);
        List<String> todo=missing(ctx,names);
        if(todo.isEmpty()){progress.message("Rota verisi zaten telefonda.");return;}
        int total=todo.size(),current=0;
        for(String item:todo){
            current++;
            progress.message("Rota verisi indiriliyor ("+current+"/"+total+"): "+item);
            if(!item.matches("[EW][0-9]{1,3}_[NS][0-9]{1,2}\\.rd5"))
                throw new IllegalArgumentException("Geçersiz rota dosyası adı");
            File dst=new File(segments(ctx),item),part=new File(segments(ctx),item+".part");
            if(ctx.getFilesDir().getUsableSpace()<60L*1024L*1024L)
                throw new IllegalStateException("Rota verisi için boş depolama alanı yetersiz");
            if(part.exists())part.delete();
            HttpURLConnection c=null;
            try {
                c=(HttpURLConnection)new URL(SOURCE+item).openConnection();
                c.setConnectTimeout(30000);c.setReadTimeout(45000);
                c.setInstanceFollowRedirects(true);
                if(c.getResponseCode()!=200)throw new IllegalStateException(item+" indirilemedi: HTTP "+c.getResponseCode());
                long size=c.getContentLengthLong();
                if(size>180L*1024L*1024L)throw new IllegalStateException(item+" 180 MB dosya sınırını aşıyor");
                try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(part)){
                    byte[] buffer=new byte[128*1024];int n;long written=0;
                    while((n=in.read(buffer))!=-1){
                        written+=n;
                        if(written>180L*1024L*1024L)throw new IllegalStateException("Rota parçası aşırı büyük");
                        out.write(buffer,0,n);
                    }
                }
                if(part.length()<1024)throw new IllegalStateException("Boş veya eksik veri: "+item);
                if(dst.exists()&&!dst.delete())throw new IllegalStateException("Eski parça silinemedi: "+item);
                if(!part.renameTo(dst))throw new IllegalStateException("Rota parçası saklanamadı");
            }catch(Exception e){
                part.delete();throw e;
            }finally {if(c!=null)c.disconnect();}
        }
        progress.message("Rota verisi indirildi. Artık bu bölgede internetsiz rota hesaplanabilir.");
    }
    static GPXTracks.Track calculate(Context ctx,LatLong start,LatLong dest,String profile,Progress progress) throws Exception {
        if(!valid(start)||!valid(dest))throw new IllegalArgumentException("Rota başlangıcı veya hedefi yok");
        if(!"trekking".equals(profile)&&!"hiking-mountain".equals(profile)&&!"car-vario".equals(profile))
            throw new IllegalArgumentException("Bilinmeyen rota profili");
        setup(ctx);
        List<String> needed=aroundRoute(start,dest),missing=missing(ctx,needed);
        if(!missing.isEmpty())throw new IllegalStateException("Önce rota verilerini indir: "+String.join(", ",missing));
        progress.message("BRouter ile çevrimdışı rota hesaplanıyor...");
        RoutingContext rc=new RoutingContext();
        rc.localFunction=new File(profiles(ctx),profile+".brf").getAbsolutePath();
        rc.rawTrackPath=new File(base(ctx),"last_raw_track.dat").getAbsolutePath();
        rc.memoryclass=64;
        List<OsmNodeNamed> waypoints=new ArrayList<>();
        OsmNodeNamed p1=new OsmNodeNamed();
        p1.name="from";
        p1.ilon=(int)((start.longitude+180.0)*1000000+0.5);
        p1.ilat=(int)((start.latitude+90.0)*1000000+0.5);
        OsmNodeNamed p2=new OsmNodeNamed();
        p2.name="to";
        p2.ilon=(int)((dest.longitude+180.0)*1000000+0.5);
        p2.ilat=(int)((dest.latitude+90.0)*1000000+0.5);
        waypoints.add(p1);waypoints.add(p2);
        RoutingEngine engine=new RoutingEngine(null,null,segments(ctx),waypoints,rc);
        engine.quite=true;
        engine.doRun(90000);
        if(engine.getErrorMessage()!=null)
            throw new IllegalStateException("Rota hesaplanamadı: "+engine.getErrorMessage());
        OsmTrack route=engine.getFoundTrack();
        if(route==null)throw new IllegalStateException("BRouter bu iki nokta arasında rota bulamadı.");
        String gpx=new FormatGpx(rc).format(route);
        if(gpx==null||gpx.length()>GPXTracks.MAX_BYTES)
            throw new IllegalStateException("Rota GPX çıktısı uygun değil");
        File tmp=new File(ctx.getFilesDir(),"computed_route.gpx.part");
        try(OutputStream out=new FileOutputStream(tmp)){
            out.write(gpx.getBytes(StandardCharsets.UTF_8));
        }
        GPXTracks.Track parsed=GPXTracks.parse(tmp);
        File destFile=GPXTracks.target(ctx);
        if(destFile.exists()&&!destFile.delete())throw new IllegalStateException("Eski rota kaldırılamadı");
        if(!tmp.renameTo(destFile))throw new IllegalStateException("Yeni rota kaydedilemedi");
        return parsed;
    }
}
