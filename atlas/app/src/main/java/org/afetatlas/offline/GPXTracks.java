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

/** GPX geometry and optional independently authored turn instructions.
 *  Rtept and trkpt MUST NOT be concatenated into one route, because BRouter GPX
 *  contains both and that would double-count and distort the route length.
 */
final class GPXTracks {
    static final int MAX_POINTS=30000;
    static final int MAX_TURNS=2000;
    static final long MAX_BYTES=20L*1024*1024;
    static final class Turn{
        final LatLong pos;
        final String description,code;
        Turn(LatLong pos,String desc,String code){this.pos=pos;this.description=desc;this.code=code;}
    }
    static final class Track{
        final ArrayList<LatLong> points;
        final double lengthKm;
        final ArrayList<Turn> turns;
        Track(ArrayList<LatLong> p,double length,ArrayList<Turn> turns){
            points=p;lengthKm=length;this.turns=turns;
        }
    }
    static Track parse(File file) throws Exception{
        if(!file.isFile()||file.length()>MAX_BYTES||file.length()<=0)
            throw new IllegalArgumentException("GPX dosyası bulunamadı veya 20 MB sınırını aşıyor.");
        XmlPullParserFactory factory=XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser=factory.newPullParser();
        try{parser.setFeature("http://xmlpull.org/v1/doc/features.html#process-docdecl",false);}
        catch(Exception ignored){}
        ArrayList<LatLong> trk=new ArrayList<>(),rte=new ArrayList<>();
        ArrayList<Turn> instructions=new ArrayList<>();
        LatLong currentTurn=null;
        String desc=null,code=null;
        try(InputStream in=new FileInputStream(file)){
            parser.setInput(in,"UTF-8");
            int event=parser.getEventType();
            while(event!=XmlPullParser.END_DOCUMENT){
                if(event==XmlPullParser.START_TAG){
                    String tag=parser.getName();
                    if("trkpt".equals(tag)||"rtept".equals(tag)){
                        String la=parser.getAttributeValue(null,"lat"),lo=parser.getAttributeValue(null,"lon");
                        if(la!=null&&lo!=null){
                            double lat=Double.parseDouble(la),lon=Double.parseDouble(lo);
                            if(Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180){
                                if(trk.size()+rte.size()>=2*MAX_POINTS)
                                    throw new IllegalArgumentException("GPX fazla koordinat içeriyor");
                                LatLong point=new LatLong(lat,lon);
                                if("trkpt".equals(tag)){if(trk.size()>=MAX_POINTS)throw new IllegalArgumentException("GPX çok fazla iz noktası");trk.add(point);}
                                else {rte.add(point);currentTurn=point;desc=null;code=null;}
                            }
                        }
                    }else if(currentTurn!=null&&("desc".equals(tag)||"turn".equals(tag))){
                        String val=parser.nextText();
                        if("desc".equals(tag))desc=val;
                        else code=val;
                    }
                }else if(event==XmlPullParser.END_TAG&&"rtept".equals(parser.getName())){
                    if(currentTurn!=null){
                        if(instructions.size()>=MAX_TURNS)throw new IllegalArgumentException("GPX 2000 dönemeç sınırını aşıyor");
                        if(desc!=null&&!desc.trim().isEmpty())
                            instructions.add(new Turn(currentTurn,desc.trim(),code==null?"":code.trim()));
                    }
                    currentTurn=null;desc=null;code=null;
                }
                event=parser.next();
            }
        }
        ArrayList<LatLong> coords=trk.size()>=2?trk:rte;
        if(coords.size()<2)throw new IllegalArgumentException("GPX içinde en az iki rota/iz noktası olmalı.");
        LatLong prev=null;double length=0;
        for(LatLong pos:coords){
            if(prev!=null)length+=haversine(prev,pos)/1000;
            prev=pos;
        }
        return new Track(coords,length,instructions);
    }
    static Polyline overlay(MapView map,Track track){
        Paint paint=AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setColor(Color.rgb(40,106,217));
        paint.setStyle(org.mapsforge.core.graphics.Style.STROKE);
        paint.setStrokeWidth(8);
        Polyline line=new Polyline(paint,AndroidGraphicFactory.INSTANCE);
        line.addPoints(track.points);
        map.getLayerManager().getLayers().add(line);
        return line;
    }
    static double haversine(LatLong a,LatLong b){
        double r=6371008.8,p1=Math.toRadians(a.latitude),p2=Math.toRadians(b.latitude);
        double dp=Math.toRadians(b.latitude-a.latitude);
        double dl=Math.toRadians(b.longitude-a.longitude);
        double t=Math.sin(dp/2)*Math.sin(dp/2)+Math.cos(p1)*Math.cos(p2)*Math.sin(dl/2)*Math.sin(dl/2);
        return 2*r*Math.asin(Math.min(1,Math.sqrt(t)));
    }
    static File target(Context ctx){return new File(ctx.getFilesDir(),"imported_track.gpx");}
}
