package org.afetatlas.offline;

import android.app.Activity;
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
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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
    private boolean showingMap;
    private File mapFile;

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
        status = new TextView(this);
        status.setTextColor(0xff244055);
        status.setPadding(dp(14), dp(8), dp(14), dp(8));
        status.setTextSize(13f);
        root.addView(status);
        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        setStatus(mapFile.exists() ? "Harita hazır. Harita sekmesine dokun." : "Harita ZIP dosyasını Harita sekmesinden seç.");
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
    }
    private void showGuides() {
        closeMap(); content.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.addView(paragraph("AFET ATLASI | Çevrimdışı Rehber", 20, 0xff12283c));
        list.addView(paragraph("Bu sürüm harita altyapısının ilk bağımsız derlemesidir. Resmî acil yardım hattı: 112. Bilgiler eğitim amaçlıdır; profesyonel yardımın yerini almaz.", 14, 0xff555555));
        for (int i=0; i<topics.length; i++) {
            TextView heading = paragraph(topics[i], 17, 0xff123e55);
            heading.setTypeface(null, 1);
            list.addView(heading);
            list.addView(paragraph(descriptions[i], 15, 0xff232323));
        }
        scroll.addView(list);
        content.addView(scroll);
    }
    private void showMapOrImport() {
        if (!mapFile.exists() || mapFile.length() < 100000) {
            pickMap();
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
            showingMap = true;
            setStatus("Türkiye haritası cihazdan yükleniyor; internet gerekli değil.");
        } catch (Exception e) {
            closeMap(); content.removeAllViews();
            setStatus("Harita açılamadı: " + e.getMessage());
            showGuides();
        }
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
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                if (input == null) throw new IllegalStateException("Dosya okunamadı");
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
