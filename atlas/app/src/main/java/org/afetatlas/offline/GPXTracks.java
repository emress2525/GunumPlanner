package org.afetatlas.offline;

import android.content.Context;
import android.graphics.Color;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.overlay.Polyline;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

/** Pre-planned GPX track viewer. Tracks are not computed navigable routes. */
final class GPXTracks {
    static final int MAX_POINTS=30000;
    static final long MAX_BYTES=20L*1024*1024;
    static final class Track {
        final ArrayList<LatLong> points;
        final double lengthKm;
        Track(ArrayList<LatLong> p,double length){points=p;lengthKm=length;}
    }
    static Track parse(File file) throws Exception{
        if(!file.isFile()||file.length()>MAX_BYTES||file.length()<=0)
            throw new IllegalArgumentException("GPX dosyası bulunamadı veya 20 MB sınırını aşıyor.");
        XmlPullParserFactory factory=XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser=factory.newPullParser();
        try {parser.setFeature("http://xmlpull.org/v1/doc/features.html#process-docdecl",false);}
        catch(Exception ignored){}
        ArrayList<LatLong> coords=new ArrayList<>();double length=0;LatLong prev=null;
        try(InputStream in=new FileInputStream(file)){
            parser.setInput(in,"UTF-8");
            int event=parser.getEventType();
            while(event!=XmlPullParser.END_DOCUMENT){
                if(event==XmlPullParser.START_TAG){
                    String tag=parser.getName();
                    if("trkpt".equals(tag)||"rtept".equals(tag)){
                        String la=parser.getAttributeValue(null,"lat");
                        String lo=parser.getAttributeValue(null,"lon");
                        if(la!=null&&lo!=null){
                            double lat=Double.parseDouble(la),lon=Double.parseDouble(lo);
                            if(Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180){
                                if(coords.size()>=MAX_POINTS)throw new IllegalArgumentException("GPX en fazla 30.000 nokta içerebilir.");
                                LatLong pos=new LatLong(lat,lon);
                                if(prev!=null)length+=haversine(prev,pos)/1000.0;
                                coords.add(pos);prev=pos;
                            }
                        }
                    }
                }
                event=parser.next();
            }
        }
        if(coords.size()<2)throw new IllegalArgumentException("GPX içinde en az iki rota/iz noktası olmalı.");
        return new Track(coords,length);
    }
    static Polyline overlay(MapView map,Track track){
        Paint paint=AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setColor(Color.rgb(40,106,217));
        paint.setStyle(org.mapsforge.core.graphics.Style.STROKE);
        paint.setStrokeWidth(8);
        Polyline polyline=new Polyline(paint,AndroidGraphicFactory.INSTANCE);
        polyline.addPoints(track.points);
        map.getLayerManager().getLayers().add(polyline);
        return polyline;
    }
    private static double haversine(LatLong a,LatLong b){
        double r=6371008.8;
        double p1=Math.toRadians(a.latitude),p2=Math.toRadians(b.latitude);
        double dp=Math.toRadians(b.latitude-a.latitude);
        double dl=Math.toRadians(b.longitude-a.longitude);
        double x=Math.sin(dp/2)*Math.sin(dp/2)+Math.cos(p1)*Math.cos(p2)*Math.sin(dl/2)*Math.sin(dl/2);
        return 2*r*Math.asin(Math.min(1,Math.sqrt(x)));
    }
    static File target(Context ctx){return new File(ctx.getFilesDir(),"imported_track.gpx");}
}
