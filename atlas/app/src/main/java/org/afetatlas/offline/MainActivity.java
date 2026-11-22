package org.afetatlas.offline;

import android.app.Activity;
import android.app.AlertDialog;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {
    private static final int SELECT_MAP = 101;
    private final String[] topics = {
        "Şiddetli kanama", "Bilinç kaybı ve solunum", "Yanık", "Kırık şüphesi",
        "Deprem", "Sel", "Orman yangını", "Hipotermi", "Sıcak çarpması",
        "Su güvenliği", "Nükleer serpinti", "Afet çantası"
    };
    private final String[] descriptions = {
        "112'yi ara. Yaraya temiz bezle doğrudan ve kesintisiz baskı uygula. Yaşamı tehdit eden kol/bacak kanamalarında uygun turnike kullanımı için eğitimli yardım al. Kanamayı durdurmak için bitki veya toz koyma.",
        "112'yi ara. Kişi tepki vermiyorsa ve normal nefes almıyorsa telefonla gelen yönlendirmeye göre kalp masajına başla; AED varsa talimatları uygula.",
        "Yanık bölgesini yaklaşık 20 dakika serin akan suyla soğut. Yapışmış kıyafetleri çıkarma. Buz, yağ, diş macunu sürme. Ciddi yanıklarda 112'yi ara.",
        "Hareket ettirme; yaralı bölgeyi destekle. Açık yarayı temiz bir örtüyle kapat. Omurga veya ciddi travma şüphesinde 112'yi ara.",
        "Sarsıntıda ÇÖK-KAPAN-TUTUN. Pencerelerden ve devrilebilecek eşyalardan uzak dur. Sarsıntı bitince güvenli tahliye yolunu takip et.",
        "Sel suyuna yaya veya araçla girme. Yüksek noktaya çık. Elektrikli ekipmanlara ve kopuk hatlara yaklaşma. Yetkili duyurularını izle.",
        "Duman ve alevlerden uzaklaş; yetkililerin tahliye talimatlarına uy. Rüzgâr yönündeki duman koridorundan kaçın. Tehlikedeysen 112'yi ara.",
        "Kişiyi rüzgâr ve sudan koru, ıslak kıyafetleri değiştir, yavaşça ısıt. Bilinç bozukluğu veya ağır titremede 112'yi ara.",
        "Kişiyi serin gölgeye al, giysileri gevşet ve serinlet. Bilinç bozukluğu varsa 112'yi ara. Bilinci kapalı kişiye içecek verme.",
        "Şüpheli suyu doğrudan içme. Mümkünse güvenilir kaynaktan su kullan. Kaynatma mikropları azaltır ama kimyasal ve radyoaktif kirleticileri ortadan kaldırmaz.",
        "Yetkililer aksini söylemedikçe sağlam bina içinde kal; mümkünse bodrum veya iç odaya geç. Dış kıyafetini çıkarıp ayrı tut, cildi suyla temizle. Resmî talimat olmadan iyot tableti alma.",
        "Su, dayanıklı gıda, ilk yardım malzemesi, fener, pil, düdük, ilaçlar, kimlik kopyası ve şarj ekipmanı bulundur. İçeriği düzenli kontrol et."
    };
    private LinearLayout root;
    private FrameLayout content;
    private TextView status;
    private MapView mapView;
    private Marker positionMarker;
    private boolean showingMap;
    private File mapFile;
    private Cities.Point targetDistrict;
    private volatile boolean mapDownloadBusy=false;
    private static final int LOCATION_PERMISSION=105;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        AndroidGraphicFactory.createInstance(getApplication());
        mapFile = new File(getFilesDir(), "turkiye.map");
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xfff5f6f8);
        LinearLayout top = new LinearLayout(this);
        top.setPadding(dp(10), dp(12), dp(10), dp(10));
        top.setBackgroundColor(0xff12283c);
        Button guide = button("Rehber");
        Button map = button("Türkiye Haritası");
        top.addView(guide, new LinearLayout.LayoutParams(0, dp(48), 1));
        top.addView(map, new LinearLayout.LayoutParams(0, dp(48), 1.2f));
        guide.setOnClickListener(v -> showGuides());
        map.setOnClickListener(v -> showMapOrImport());
        root.addView(top);
        LinearLayout extras=new LinearLayout(this);
        extras.setBackgroundColor(0xffdce7ec);
        Button places=button("81 İl / İlçe");
        Button gps=button("GPS");
        Button check=button("Kontrol Listesi");
        extras.addView(places,new LinearLayout.LayoutParams(0,dp(50),1.2f));
        extras.addView(gps,new LinearLayout.LayoutParams(0,dp(50),0.8f));
        extras.addView(check,new LinearLayout.LayoutParams(0,dp(50),1.3f));
        root.addView(extras);
        LinearLayout extraTools=new LinearLayout(this);
        extraTools.setBackgroundColor(0xffe4edf1);
        Button flows=button("Acil Akışlar");
        Button notebook=button("Saha Defteri");
        Button visuals=button("Görsel Rehber");
        extraTools.addView(flows,new LinearLayout.LayoutParams(0,dp(49),1));
        extraTools.addView(notebook,new LinearLayout.LayoutParams(0,dp(49),1));
        extraTools.addView(visuals,new LinearLayout.LayoutParams(0,dp(49),1));
        root.addView(extraTools);
        flows.setOnClickListener(v->showExtra(DecisionFlows.menu(this)));
        notebook.setOnClickListener(v->showExtra(FieldNotebook.screen(this)));
        visuals.setOnClickListener(v->showExtra(VisualGuides.screen(this)));
        places.setOnClickListener(v->Cities.pick(this,p->{
            targetDistrict=p;
            if(!mapFile.exists()){
                setStatus(p.city+" / "+p.district+" seçildi. Önce harita ZIP'ini yükle.");
                showMapOrImport();
            }else showMapOrImport();
        }));
        gps.setOnClickListener(v->locate());
        check.setOnClickListener(v->showChecklist());
        status = new TextView(this);
        status.setTextColor(0xff244055);
        status.setPadding(dp(14), dp(8), dp(14), dp(8));
        status.setTextSize(13f);
        root.addView(status);
        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        setStatus(mapFile.exists() ? "Harita hazır. Türkiye Haritası sekmesine dokun." : "Türkiye Haritası: Wi-Fi ile indir veya ZIP dosyası seç.");
        showGuides();
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setAllCaps(false); b.setText(text); b.setTextSize(14f);
        return b;
    }
    private int dp(int x) { return (int)(x * getResources().getDisplayMetrics().density + .5f); }
    private void setStatus(String s) { status.setText(s); }
    private TextView paragraph(String text, int size, int color) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextSize(size); t.setTextColor(color);
        t.setPadding(dp(15), dp(10), dp(15), dp(10));
        return t;
    }
    private void closeMap() {
        if (mapView != null) {
            content.removeView(mapView);
            mapView.destroyAll();
            mapView = null;
        }
        showingMap = false;
        positionMarker=null;
    }
    private void showPin(double lat,double lon){
        if(mapView==null)return;
        if(positionMarker!=null)mapView.getLayerManager().getLayers().remove(positionMarker);
        android.graphics.drawable.Drawable icon=getResources().getDrawable(R.drawable.atlas_pin);
        org.mapsforge.core.graphics.Bitmap bm=AndroidGraphicFactory.convertToBitmap(icon);
        positionMarker=new Marker(new LatLong(lat,lon),bm,0,-bm.getHeight()/2);
        mapView.getLayerManager().getLayers().add(positionMarker);
    }
    private void showGuides() {
        closeMap();
        content.removeAllViews();
        content.addView(AtlasPages.guides(this));
    }
    private void showExtra(View page){
        closeMap();content.removeAllViews();content.addView(page);
    }
    private void showChecklist(){
        closeMap();
        content.removeAllViews();
        content.addView(AtlasPages.checklist(this));
    }
    private void locate(){
        if(!mapFile.exists()){
            setStatus("GPS için önce çevrimdışı haritayı yükle.");
            showMapOrImport(); return;
        }
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_PERMISSION);
            return;
        }
        try{
            LocationManager lm=(LocationManager)getSystemService(LOCATION_SERVICE);
            Location location=lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(location==null)location=lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if(location!=null){goToLocation(location,"Son bilinen konum");return;}
            if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                setStatus("Telefonun konum hizmetlerini aç.");return;
            }
            setStatus("GPS sinyali bekleniyor...");
            lm.requestSingleUpdate(LocationManager.GPS_PROVIDER,new LocationListener(){
                @Override public void onLocationChanged(Location location){goToLocation(location,"GPS konumu");}
                @Override public void onStatusChanged(String provider,int status,Bundle extra){}
                @Override public void onProviderEnabled(String provider){}
                @Override public void onProviderDisabled(String provider){setStatus("GPS kapalı");}
            },null);
        }catch(Exception e){setStatus("GPS hatası: "+e.getMessage());}
    }
    private void goToLocation(Location location,String info){
        targetDistrict=null;
        if(mapView==null)showMapOrImport();
        if(mapView!=null){
            mapView.setCenter(new LatLong(location.getLatitude(),location.getLongitude()));
            mapView.setZoomLevel((byte)15);
            showPin(location.getLatitude(),location.getLongitude());
            setStatus(info+": "+String.format(Locale.US,"%.5f, %.5f",location.getLatitude(),location.getLongitude())+
                " (haritada işaretlendi)");
        }
    }
    @Override public void onRequestPermissionsResult(int req,String[] perms,int[] grants){
        super.onRequestPermissionsResult(req,perms,grants);
        if(req==LOCATION_PERMISSION){
            if(grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)locate();
            else setStatus("Konum izni verilmedi.");
        }
    }
    private void showMapOrImport() {
        if (!mapFile.exists() || mapFile.length() < 100000) {
            showMapSetup();
            return;
        }
        try {
            closeMap(); content.removeAllViews();
            mapView = new MapView(this);
            content.addView(mapView, new FrameLayout.LayoutParams(-1, -1));
            mapView.getMapScaleBar().setVisible(true);
            mapView.setBuiltInZoomControls(true);
            TileCache cache = AndroidUtil.createTileCache(this, "atlasmapcache",
                    mapView.getModel().displayModel.getTileSize(), 1f,
                    mapView.getModel().frameBufferModel.getOverdrawFactor());
            MapDataStore store = new MapFile(mapFile);
            TileRendererLayer layer = new TileRendererLayer(cache, store,
                    mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE);
            layer.setXmlRenderTheme(MapsforgeThemes.MOTORIDER);
            mapView.getLayerManager().getLayers().add(layer);
            mapView.setCenter(new LatLong(39.0, 35.0));
            mapView.setZoomLevel((byte) 6);
            if(targetDistrict!=null){
                mapView.setCenter(new LatLong(targetDistrict.latitude,targetDistrict.longitude));
                mapView.setZoomLevel((byte)11);
                showPin(targetDistrict.latitude,targetDistrict.longitude);
            }
            showingMap = true;
            setStatus(targetDistrict==null?"Türkiye haritası cihazdan yükleniyor; internet gerekli değil.":targetDistrict.city+" / "+targetDistrict.district+" • yaklaşık ilçe koordinatı");
        } catch (Exception e) {
            closeMap(); content.removeAllViews();
            setStatus("Harita açılamadı: " + e.getMessage());
            showGuides();
        }
    }
    private void showMapSetup(){
        if(mapDownloadBusy){setStatus("Harita indiriliyor veya çıkarılıyor. Lütfen bekle.");return;}
        new AlertDialog.Builder(this)
         .setTitle("Türkiye çevrimdışı haritası")
         .setMessage("Harita yaklaşık 450 MB'tır. Bir kez indirildikten sonra internet olmadan çalışır. Mobil veri yerine Wi-Fi önerilir. Telefonda yaklaşık 1 GB boş yer ayır.")
         .setPositiveButton("Haritayı indir",(dialog,which)->downloadMap())
         .setNeutralButton("Telefondan ZIP / .map seç",(dialog,which)->pickMap())
         .setNegativeButton("İptal",null).show();
    }
    private void downloadMap(){
        if(mapDownloadBusy)return;
        if(getFilesDir().getUsableSpace()<550L*1024*1024){
            setStatus("Yeterli alan yok: harita için en az 550 MB boş alan gerekiyor.");
            return;
        }
        mapDownloadBusy=true;
        setStatus("BBBike Türkiye haritasına bağlanılıyor; indirme büyük olduğu için bekle...");
        new Thread(()->{
            File tmp=new File(getFilesDir(),"turkiye.map.part");
            HttpURLConnection connection=null;
            try{
                if(tmp.exists()&&!tmp.delete())throw new IllegalStateException("Geçici harita temizlenemedi");
                URL link=new URL("https://data.bbbike.org/osm/mapsforge/region/europe/turkey/turkey.osm.mapsforge-osm.zip");
                connection=(HttpURLConnection)link.openConnection();
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(45000);
                connection.setInstanceFollowRedirects(true);
                int code=connection.getResponseCode();
                if(code!=200)throw new IllegalStateException("Sunucu yanıtı: HTTP "+code);
                boolean found=false;
                try(ZipInputStream zis=new ZipInputStream(new BufferedInputStream(connection.getInputStream(),128*1024))){
                    ZipEntry entry;
                    while((entry=zis.getNextEntry())!=null){
                        if(!entry.isDirectory()&&entry.getName().toLowerCase(Locale.ROOT).endsWith(".map")){
                            copy(zis,tmp);found=true;break;
                        }
                        zis.closeEntry();
                    }
                }
                if(!found||tmp.length()<100000)throw new IllegalStateException("İndirilen ZIP içinde kullanılabilir .map yok");
                if(mapFile.exists()&&!mapFile.delete())throw new IllegalStateException("Eski harita temizlenemedi");
                if(!tmp.renameTo(mapFile))throw new IllegalStateException("Harita kaydedilemedi");
                runOnUiThread(()->{
                    setStatus("Türkiye haritası indirildi ve çevrimdışı kullanıma hazır.");
                    showMapOrImport();
                });
            }catch(Exception e){
                tmp.delete();
                runOnUiThread(()->setStatus("Harita indirilemedi: "+e.getMessage()+" • ZIP dosyasını seçerek de yükleyebilirsin."));
            }finally{
                if(connection!=null)connection.disconnect();
                mapDownloadBusy=false;
            }
        }).start();
    }
    private void pickMap() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, SELECT_MAP);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_MAP && resultCode == RESULT_OK && data != null && data.getData() != null) {
            final Uri uri = data.getData();
            setStatus("Dosya işleniyor. ZIP büyükse birkaç dakika sürebilir…");
            new Thread(() -> importMap(uri)).start();
        }
    }
    private void importMap(Uri uri) {
        File tmp = new File(getFilesDir(), "turkiye.map.part");
        try {
            String name = uri.getLastPathSegment();
            if (name == null) name = "";
            boolean zip = name.toLowerCase(Locale.ROOT).endsWith(".zip");
            try (InputStream inputOriginal = getContentResolver().openInputStream(uri)) {
                if(inputOriginal==null)throw new IllegalStateException("Dosya okunamadı");
                BufferedInputStream input=new BufferedInputStream(inputOriginal,128*1024);
                input.mark(4);
                int marker1=input.read(),marker2=input.read();
                input.reset();
                zip=zip||(marker1=='P'&&marker2=='K');
                if (zip) {
                    try (ZipInputStream zis = new ZipInputStream(input)) {
                        ZipEntry entry;
                        boolean found = false;
                        while ((entry = zis.getNextEntry()) != null) {
                            if (!entry.isDirectory() && entry.getName().toLowerCase(Locale.ROOT).endsWith(".map")) {
                                copy(zis, tmp);
                                found = true; break;
                            }
                            zis.closeEntry();
                        }
                        if (!found) throw new IllegalArgumentException("ZIP içinde .map bulunamadı");
                    }
                } else {
                    copy(input, tmp);
                }
            }
            if (tmp.length() < 100000) throw new IllegalArgumentException("Harita dosyası çok küçük");
            if (mapFile.exists() && !mapFile.delete()) throw new IllegalStateException("Eski harita silinemedi");
            if (!tmp.renameTo(mapFile)) throw new IllegalStateException("Harita kaydedilemedi");
            runOnUiThread(() -> { setStatus("Harita içe aktarıldı."); showMapOrImport(); });
        } catch (Exception ex) {
            tmp.delete();
            runOnUiThread(() -> setStatus("İçe aktarma hatası: " + ex.getMessage()));
        }
    }
    private void copy(InputStream in, File file) throws Exception {
        byte[] buffer = new byte[128 * 1024];
        long written = 0, update = 0;
        try (OutputStream out = new FileOutputStream(file)) {
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n); written += n;
                if (written - update > 32L * 1024 * 1024) {
                    update = written;
                    final long mb = written / (1024 * 1024);
                    runOnUiThread(() -> setStatus("Harita aktarılıyor: " + mb + " MB"));
                }
            }
            out.flush();
        }
    }
    @Override public void onBackPressed() {
        if (showingMap) showGuides(); else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        closeMap();
        AndroidGraphicFactory.clearResourceMemoryCache();
        super.onDestroy();
    }
}
