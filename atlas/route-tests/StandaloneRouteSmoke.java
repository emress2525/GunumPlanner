import btools.router.OsmNodeNamed;
import btools.router.OsmTrack;
import btools.router.RoutingContext;
import btools.router.RoutingEngine;
import btools.router.FormatGpx;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/** JVM end-to-end test of the same vendored BRouter engine used inside Android. */
public final class StandaloneRouteSmoke {
    private static OsmNodeNamed point(String name,double latitude,double longitude){
        OsmNodeNamed node=new OsmNodeNamed();
        node.name=name;
        node.ilon=(int)((longitude+180.0)*1000000.0+0.5);
        node.ilat=(int)((latitude+90.0)*1000000.0+0.5);
        return node;
    }
    public static void main(String[] args) throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("args: segmentsFolder profileFolder");
        File segments=new File(args[0]),profiles=new File(args[1]);
        if(!new File(segments,"E30_N35.rd5").isFile())
            throw new IllegalStateException("Route segment fixture missing");
        // Two ordinary public road locations in central Ankara (same E30_N35 segment).
        List<OsmNodeNamed> wp=Arrays.asList(
           point("from",39.9207,32.8541),
           point("to",39.9246,32.8501)
        );
        for(String profile:new String[]{"trekking","car-vario","hiking-mountain"}){
            RoutingContext context=new RoutingContext();
            context.localFunction=new File(profiles,profile+".brf").getAbsolutePath();
            context.memoryclass=64;
            context.turnInstructionMode=3; // OsmAnd GPX turn-instruction mode
            context.rawTrackPath=new File(segments,"test-"+profile+".dat").getAbsolutePath();
            RoutingEngine engine=new RoutingEngine(null,null,segments,wp,context);
            engine.quite=true;
            engine.doRun(90000);
            if(engine.getErrorMessage()!=null)
                throw new IllegalStateException(profile+": "+engine.getErrorMessage());
            OsmTrack track=engine.getFoundTrack();
            if(track==null)throw new IllegalStateException(profile+" returned null track");
            String gpx=new FormatGpx(context).format(track);
            if(!gpx.contains("<trkpt"))
                throw new IllegalStateException(profile+" returned no track geometry");
            if(!gpx.contains("<rtept")||!gpx.contains("<desc>"))
                throw new IllegalStateException(profile+" emitted no local turn-instruction routepoints");
            System.out.println("ROUTING_OK profile="+profile+" gpxChars="+gpx.length());
        }
    }
}
