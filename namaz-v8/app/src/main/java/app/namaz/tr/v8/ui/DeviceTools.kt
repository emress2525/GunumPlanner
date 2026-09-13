package app.namaz.tr.v8.ui

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.namaz.tr.v8.model.AppMode
import app.namaz.tr.v8.prayer.PrayerRuntimeSettings
import app.namaz.tr.v8.widget.CountdownWidgetProvider
import app.namaz.tr.v8.widget.DailyAyahWidgetProvider
import app.namaz.tr.v8.widget.HijriDateWidgetProvider
import app.namaz.tr.v8.widget.NextPrayerWidgetProvider
import app.namaz.tr.v8.widget.PrayerStatusNotifier
import app.namaz.tr.v8.widget.PrayerTimesWidgetProvider
import app.namaz.tr.v8.widget.PrayerTrackerWidgetProvider
import app.namaz.tr.v8.widget.QuranResumeWidgetProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

object QiblaMath {
    private const val KAABA_LAT = 21.4225
    private const val KAABA_LON = 39.8262
    fun bearing(latitude: Double, longitude: Double): Double {
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(KAABA_LAT)
        val dLon = Math.toRadians(KAABA_LON - longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }
}

object BackupManager {
    private val safePrefs = listOf("quran_progress_v8", "learning_progress_v8", "dhikr_v8", "ramadan_v8", "device_prefs_v8", "ui_prefs_v8")

    fun export(context: Context, uri: Uri) {
        val root = JSONObject()
        root.put("format", "namaz-v8-backup-1")
        val allPrefs = JSONObject()
        safePrefs.forEach { name ->
            val out = JSONObject()
            context.getSharedPreferences(name, Context.MODE_PRIVATE).all.forEach { (key, value) ->
                when (value) {
                    is Set<*> -> out.put(key, JSONArray(value.filterIsInstance<String>()))
                    else -> out.put(key, value)
                }
            }
            allPrefs.put(name, out)
        }
        root.put("preferences", allPrefs)
        root.put("note", "private_worship_v8 varsayılan yedeğe dahil değildir")
        context.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(root.toString(2)) }
    }

    fun import(context: Context, uri: Uri) {
        val text = context.contentResolver.openInputStream(uri)!!.use { input -> BufferedReader(InputStreamReader(input)).readText() }
        val root = JSONObject(text)
        require(root.optString("format") == "namaz-v8-backup-1") { "Uyumsuz yedek biçimi" }
        val prefsRoot = root.getJSONObject("preferences")
        safePrefs.forEach { name ->
            if (!prefsRoot.has(name)) return@forEach
            val source = prefsRoot.getJSONObject(name)
            val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
            source.keys().forEach { key ->
                when (val value = source.get(key)) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is JSONArray -> editor.putStringSet(key, (0 until value.length()).map { value.getString(it) }.toSet())
                    else -> editor.putString(key, value.toString())
                }
            }
            editor.apply()
        }
    }
}

@Composable
fun MoreHubScreen(
    appMode: AppMode,
    onModeChange: (AppMode) -> Unit,
    runtime: PrayerRuntimeSettings?,
    fontScale: Float,
    onFontScale: (Float) -> Unit,
    onHealth: () -> Unit,
) {
    var screen by rememberSaveable { mutableStateOf<String?>(null) }
    when (screen) {
        "qibla" -> return QiblaScreen(runtime?.location?.latitude ?: 39.9334, runtime?.location?.longitude ?: 32.8597) { screen = null }
        "widgets" -> return WidgetCenterScreen { screen = null }
        "mosque" -> return MosqueScreen(runtime?.location?.city ?: "Ankara") { screen = null }
        "sources" -> return SourcesPrivacyScreen { screen = null }
        "backup" -> return BackupScreen { screen = null }
        "wear" -> return WearInfoScreen { screen = null }
    }
    val context = LocalContext.current
    val notifyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var lockStatus by remember { mutableStateOf(PrayerStatusNotifier.isEnabled(context)) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Daha Fazla", style = MaterialTheme.typography.headlineMedium)
            Text("Görünüm, cihaz araçları, gizlilik ve teknik kontroller", modifier = Modifier.padding(bottom = 10.dp))
            ToolCard("Görünüm", if (appMode == AppMode.SIMPLE) "Basit Mod" else "Tam Mod") {
                onModeChange(if (appMode == AppMode.SIMPLE) AppMode.FULL else AppMode.SIMPLE)
            }
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Yazı boyutu", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1f to "Standart", 1.15f to "Büyük", 1.3f to "Yaşlı").forEach { (scale, label) ->
                            OutlinedButton(onClick = { onFontScale(scale) }) { Text(if (fontScale == scale) "✓ $label" else label) }
                        }
                    }
                }
            }
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Kilit ekranı namaz durumu", fontWeight = FontWeight.Bold)
                        Text("Widget desteklenmezse sessiz, sürekli durum bildirimi kullanır.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(lockStatus, { enabled ->
                        lockStatus = enabled
                        PrayerStatusNotifier.setEnabled(context, enabled)
                        if (enabled && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    })
                }
            }
            Button(onClick = onHealth, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("Bildirim Sağlık Merkezi") }
            ToolCard("Kıble", "Sensör destekli yön ve sayısal derece") { screen = "qibla" }
            ToolCard("Widget Merkezi", "7 ayrı ana ekran/kilit ekranı widget’ı") { screen = "widgets" }
            ToolCard("Yakındaki Camiler", runtime?.location?.city ?: "Seçili şehir") { screen = "mosque" }
            ToolCard("Kaynaklar ve Gizlilik", "İçerik kökeni, veri politikası ve sınırlar") { screen = "sources" }
            ToolCard("Yedekle / Geri Yükle", "Yer imi, öğrenme, zikir ve güvenli tercihler") { screen = "backup" }
            ToolCard("Wear OS", "Telefon-saat sınırları ve paylaşılabilir durum") { screen = "wear" }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ToolCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(subtitle, style = MaterialTheme.typography.bodySmall) }
            OutlinedButton(onClick = onClick) { Text("Aç") }
        }
    }
}

@Composable
private fun BackHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = onBack) { Text("←") }; Text(title, style = MaterialTheme.typography.headlineSmall) }
}

@Composable
private fun QiblaScreen(latitude: Double, longitude: Double, onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    var heading by remember { mutableStateOf<Float?>(null) }
    DisposableEffect(sensor) {
        if (sensor == null) return@DisposableEffect onDispose { }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val matrix = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                SensorManager.getOrientation(matrix, orientation)
                heading = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { manager.unregisterListener(listener) }
    }
    val qibla = QiblaMath.bearing(latitude, longitude)
    val delta = heading?.let { ((qibla - it + 540.0) % 360.0) - 180.0 }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackHeader("Kıble", onBack)
        Text("Kâbe yönü", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp))
        Text("${qibla.roundToInt()}°", style = MaterialTheme.typography.displayMedium)
        if (heading == null) {
            Text("Bu cihazda uygun yön sensörü bulunamadı veya henüz veri gelmedi. Sayısal kıble derecesini kullanabilirsin.")
        } else {
            Text("Telefon yönü: ${heading!!.roundToInt()}°")
            Text(if (kotlin.math.abs(delta!!) < 3) "✓ Kıbleye hizalandın" else "Telefonu yaklaşık ${kotlin.math.abs(delta).roundToInt()}° ${if (delta > 0) "sağa" else "sola"} çevir.", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            Text("Manyetik sensörler metal, mıknatıs ve elektronik cihazlardan etkilenebilir. Şüphede telefonu sekiz çizerek kalibre et.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun WidgetCenterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = AppWidgetManager.getInstance(context)
    val widgets = listOf(
        "Sıradaki Namaz 1×2" to NextPrayerWidgetProvider::class.java,
        "Büyük Geri Sayım 2×2" to CountdownWidgetProvider::class.java,
        "Tüm Vakitler 4×2" to PrayerTimesWidgetProvider::class.java,
        "Namaz Takibi 2×2" to PrayerTrackerWidgetProvider::class.java,
        "Günün Ayeti 4×2" to DailyAyahWidgetProvider::class.java,
        "Hicrî Tarih 2×2" to HijriDateWidgetProvider::class.java,
        "Kur’an’da Kaldığın Yer 2×2" to QuranResumeWidgetProvider::class.java,
    )
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { BackHeader("Widget Merkezi", onBack); Text("Her widget ayrı seçilir. Kilit ekranı widget desteği telefon/launcher üreticisine bağlıdır.", modifier = Modifier.padding(vertical = 8.dp)) }
        items(widgets) { (title, cls) ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        if (Build.VERSION.SDK_INT >= 26 && manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(ComponentName(context, cls), null, null)
                    }) { Text("Ekle") }
                }
            }
        }
    }
}

@Composable
private fun MosqueScreen(city: String, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackHeader("Yakındaki Camiler", onBack)
        Text("Uygulama cami listesi uydurmaz; seçili şehir için cihazındaki harita sağlayıcısında arama açar.", modifier = Modifier.padding(vertical = 12.dp))
        Button(onClick = {
            val uri = Uri.parse("geo:0,0?q=${Uri.encode("cami $city")}")
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        }, modifier = Modifier.fillMaxWidth()) { Text("$city için camileri haritada ara") }
    }
}

@Composable
private fun SourcesPrivacyScreen(onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            BackHeader("Kaynaklar ve Gizlilik", onBack)
            Section("Kur’an", "Arapça metin, sure verileri ve Elmalılı tefsir paketi build sırasında sabitlenmiş kurancilar/json kaynağından APK’ya alınır. Türkçe meal alanında kaynak atfı görünür tutulur. Ses dosyaları isteğe bağlı internet akışıdır; tam tilavet APK’ya gömülmez.")
            Section("Namaz vakti", "Adhan hesap algoritması kullanılır. Türkiye seçeneği Diyanet’e yaklaşık hesap olarak etiketlenir; resmî Diyanet API verisi olduğu iddia edilmez. Hanefî/Şafiî ikindi ve manuel dakika ayarı kullanıcı kontrolündedir.")
            Section("Dini bilgi", "Akademi ve İbadet merkezinde Kur’an, Diyanet İlmihal ve belirtilen temel kaynaklar görünürdür. Yerel bilgi yardımcısı kaynak bulamazsa cevap üretmez; kişisel fetva gereken konularda yetkili uzmana yönlendirir.")
            Section("Gizlilik", "Hesap zorunluluğu ve reklam yoktur. Namaz takibi, yer imleri, öğrenme ilerlemesi, zikir ve özel kayıtlar varsayılan olarak cihazda tutulur. Kadınlara özel kayıtlar varsayılan yedeğe dahil edilmez.")
            Section("Platform sınırları", "Exact alarm, pil optimizasyonu, DND, kilit ekranı widget ve arka plan davranışı Android sürümü ve üreticiye göre değişebilir. Bildirim Sağlık Merkezi bu izinleri teşhis eder.")
        }
    }
}

@Composable
private fun Section(title: String, text: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
    Text(text)
}

@Composable
private fun BackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) message = runCatching { BackupManager.export(context, uri); "Yedek oluşturuldu." }.getOrElse { "Yedek hatası: ${it.message}" }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) message = runCatching { BackupManager.import(context, uri); "Yedek geri yüklendi. Bazı ekranları yenilemek için uygulamayı yeniden aç." }.getOrElse { "Geri yükleme hatası: ${it.message}" }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackHeader("Yedekle / Geri Yükle", onBack)
        Text("Yer imleri, Kur’an/öğrenme ilerlemesi, zikir/Ramazan ve güvenli cihaz tercihleri JSON olarak dışa aktarılır. Kadınlara özel kayıt varsayılan olarak dahil edilmez.", modifier = Modifier.padding(vertical = 12.dp))
        Button(onClick = { exportLauncher.launch("Namaz-V8-Yedek.json") }, modifier = Modifier.fillMaxWidth()) { Text("Yedek oluştur") }
        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Yedekten geri yükle") }
        if (message.isNotBlank()) Text(message, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun WearInfoScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackHeader("Wear OS", onBack)
        Text("Namaz V8 telefon uygulaması sıradaki namaz, vakit ve takip durumunu saat entegrasyonu için paylaşılabilir bir modelde tutar.", modifier = Modifier.padding(top = 16.dp))
        Text("Modern Wear OS’ta telefon APK’sı her saatte otomatik olarak saat uygulaması kuramaz. Tam saat arayüzü ayrı wearable modülü/Play dağıtımı gerektirir; bu telefon APK’sı bunu varmış gibi göstermiyor.", modifier = Modifier.padding(top = 12.dp))
    }
}
