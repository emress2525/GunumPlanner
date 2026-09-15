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
import android.location.Location
import android.location.LocationManager
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import app.namaz.tr.v8.widget.WidgetAppearance
import app.namaz.tr.v8.widget.WidgetAppearanceStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
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

object BackupCrypto {
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    fun encrypt(plainText: String, password: CharArray): JSONObject {
        require(password.size >= 6) { "Yedek parolası en az 6 karakter olmalı" }
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val key = derive(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return JSONObject()
            .put("format", "namaz-v8-encrypted-backup-1")
            .put("kdf", "PBKDF2WithHmacSHA256")
            .put("iterations", ITERATIONS)
            .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            .put("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP))
    }

    fun decrypt(envelope: JSONObject, password: CharArray): String {
        require(envelope.optString("format") == "namaz-v8-encrypted-backup-1") { "Uyumsuz yedek biçimi" }
        require(password.size >= 6) { "Yedek parolası en az 6 karakter olmalı" }
        val salt = Base64.decode(envelope.getString("salt"), Base64.NO_WRAP)
        val iv = Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)
        val encrypted = Base64.decode(envelope.getString("ciphertext"), Base64.NO_WRAP)
        val key = derive(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    private fun derive(password: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val encoded = factory.generateSecret(PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)).encoded
        return SecretKeySpec(encoded, "AES")
    }
}

object BackupManager {
    private val safePrefs = listOf(
        "quran_progress_v8",
        "learning_progress_v8",
        "dhikr_v8",
        "ramadan_v8",
        "device_prefs_v8",
        "ui_prefs_v8",
        "widget_appearance_v8",
    )

    fun export(context: Context, uri: Uri, password: CharArray) {
        val payload = JSONObject()
            .put("format", "namaz-v8-portable-data-1")
            .put("createdAt", System.currentTimeMillis())
            .put("preferences", collectPreferences(context))
            .put("privacy", "private_worship_v8 varsayılan yedeğe dahil değildir")
        val envelope = BackupCrypto.encrypt(payload.toString(), password)
        context.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(envelope.toString(2)) }
    }

    fun import(context: Context, uri: Uri, password: CharArray) {
        val text = context.contentResolver.openInputStream(uri)!!.use { input -> BufferedReader(InputStreamReader(input)).readText() }
        val plain = BackupCrypto.decrypt(JSONObject(text), password)
        val root = JSONObject(plain)
        require(root.optString("format") == "namaz-v8-portable-data-1") { "Yedek içeriği uyumsuz" }
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

    private fun collectPreferences(context: Context): JSONObject {
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
        return allPrefs
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
                        if (enabled && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    })
                }
            }
            Button(onClick = onHealth, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("Bildirim Sağlık Merkezi") }
            ToolCard("Kıble", "Sensör doğruluğu + sayısal kıble derecesi") { screen = "qibla" }
            ToolCard("Widget Stüdyosu", "7 ayrı widget + yazı/ayrıntı ayarı") { screen = "widgets" }
            ToolCard("Yakındaki Camiler", runtime?.location?.city ?: "Seçili şehir") { screen = "mosque" }
            ToolCard("Kaynaklar ve Gizlilik", "Kur’an/meal/tefsir kökeni ve veri politikası") { screen = "sources" }
            ToolCard("Şifreli Yedek", "Yer imi, öğrenme, zikir ve güvenli tercihler") { screen = "backup" }
            ToolCard("Wear OS", "Telefon-saat sınırları açıkça belirtilir") { screen = "wear" }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ToolCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onClick) { Text("Aç") }
        }
    }
}

@Composable
private fun BackHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onBack) { Text("←") }
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun QiblaScreen(latitude: Double, longitude: Double, onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    var heading by remember { mutableStateOf<Float?>(null) }
    var accuracy by remember { mutableStateOf(SensorManager.SENSOR_STATUS_UNRELIABLE) }

    DisposableEffect(sensor) {
        if (sensor == null) return@DisposableEffect onDispose { }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val matrix = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                SensorManager.getOrientation(matrix, orientation)
                heading = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                accuracy = event.accuracy
            }
            override fun onAccuracyChanged(sensor: Sensor?, value: Int) { accuracy = value }
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { manager.unregisterListener(listener) }
    }

    val qibla = QiblaMath.bearing(latitude, longitude)
    val delta = heading?.let { ((qibla - it + 540.0) % 360.0) - 180.0 }
    val reliable = accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackHeader("Kıble", onBack)
        Text("Kâbe yönü", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp))
        Text("${qibla.roundToInt()}°", style = MaterialTheme.typography.displayMedium)
        if (heading == null) {
            Text("Bu cihazda uygun yön sensörü bulunamadı veya henüz veri gelmedi. Sayısal kıble derecesini kullanabilirsin.")
        } else {
            Text("Telefon yönü: ${heading!!.roundToInt()}°")
            Text("Sensör doğruluğu: ${accuracyLabel(accuracy)}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            if (!reliable) {
                Text("⚠ Sensör doğruluğu düşük. Telefonu metal ve mıknatıslardan uzaklaştırıp sekiz çizerek kalibre et; hizalandı onayı bu durumda verilmez.")
            } else {
                Text(
                    if (kotlin.math.abs(delta!!) < 3) "✓ Kıbleye hizalandın" else "Telefonu yaklaşık ${kotlin.math.abs(delta).roundToInt()}° ${if (delta > 0) "sağa" else "sola"} çevir.",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Text("Pusula; metal, mıknatıs, araç içi ve elektronik cihazlardan etkilenebilir.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

private fun accuracyLabel(value: Int): String = when (value) {
    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "Yüksek"
    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Orta"
    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Düşük"
    else -> "Güvenilmez"
}

@Composable
private fun WidgetCenterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = AppWidgetManager.getInstance(context)
    var appearance by remember { mutableStateOf(WidgetAppearanceStore.load(context)) }
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
        item {
            BackHeader("Widget Stüdyosu", onBack)
            Text("Her widget ayrı seçilir. Kilit ekranı widget desteği telefon/launcher üreticisine bağlıdır.", modifier = Modifier.padding(vertical = 8.dp))
            Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Widget yazı boyutu", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0.9f, 1f, 1.2f, 1.4f).forEach { scale ->
                            OutlinedButton(onClick = {
                                appearance = appearance.copy(textScale = scale)
                                WidgetAppearanceStore.save(context, appearance)
                            }) { Text(if (appearance.textScale == scale) "✓ ${scale}×" else "${scale}×") }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Alt bilgi satırını göster", modifier = Modifier.weight(1f))
                        Switch(appearance.showFooter, {
                            appearance = appearance.copy(showFooter = it)
                            WidgetAppearanceStore.save(context, appearance)
                        })
                    }
                }
            }
        }
        items(widgets) { (title, cls) ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        if (Build.VERSION.SDK_INT >= 26 && manager.isRequestPinAppWidgetSupported) {
                            manager.requestPinAppWidget(ComponentName(context, cls), null, null)
                        }
                    }) { Text("Ekle") }
                }
            }
        }
    }
}

@Composable
private fun MosqueScreen(city: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    fun refreshLastLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        lastLocation = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) refreshLastLocation()
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackHeader("Yakındaki Camiler", onBack)
        Text("Uygulama cami listesi uydurmaz; cihazındaki harita sağlayıcısında arama açar.", modifier = Modifier.padding(vertical = 12.dp))
        Button(onClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) refreshLastLocation()
            else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }, modifier = Modifier.fillMaxWidth()) { Text("Mevcut konumu kullan") }
        OutlinedButton(onClick = {
            val location = lastLocation
            val uri = if (location != null) {
                Uri.parse("geo:${location.latitude},${location.longitude}?q=${Uri.encode("cami")}")
            } else {
                Uri.parse("geo:0,0?q=${Uri.encode("cami $city")}")
            }
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(if (lastLocation != null) "Konumumun çevresindeki camileri aç" else "$city için camileri haritada ara")
        }
        Text("Konum izni isteğe bağlıdır; izin vermezsen manuel şehir seçimi çalışmaya devam eder.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun SourcesPrivacyScreen(onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            BackHeader("Kaynaklar ve Gizlilik", onBack)
            Section("Kur’an Arapça", "Tanzil Project Uthmani v1.1 metni kullanılır. Lisans: CC BY 3.0. Metin değiştirilmeden paketlenir ve Tanzil kaynak/izin bildirimi APK içinde tutulur.")
            Section("Türkçe meal", "Rowad Tercüme Merkezi Türkçe tercümesi QuranEnc.com API’sinden alınır. QuranEnc yeniden yayıma; metni değiştirmeme, kaynak/yayıncı ve sürüm bilgisini koruma şartlarıyla izin verir. Build sırasında sürüm metadata’sı APK’ya kaydedilir.")
            Section("Tefsir", "Elmalılı Muhammed Hamdi Yazır’ın Hak Dini Kur'an Dili metni, sabitlenmiş kurancilar/json dijital kaynağından çevrimdışı paketlenir; dijital kaynak MIT lisans bildirimiyle kayıtlıdır.")
            Section("Namaz vakti", "Adhan hesap algoritması kullanılır. Türkiye seçeneği Diyanet’e yaklaşık hesap olarak etiketlenir; resmî Diyanet API verisi olduğu iddia edilmez. Hanefî/Şafiî ikindi ve manuel dakika ayarı kullanıcı kontrolündedir.")
            Section("Dini bilgi", "Akademi ve İbadet merkezinde ayet/hadis/ilmihal kaynakları görünürdür. Yerel bilgi yardımcısı kaynak bulamazsa cevap üretmez; kişisel fetva gereken konularda yetkili uzmana yönlendirir.")
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
    var password by rememberSaveable { mutableStateOf("") }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            message = runCatching {
                BackupManager.export(context, uri, password.toCharArray())
                "Şifreli yedek oluşturuldu. Parolanı unutursan dosya açılamaz."
            }.getOrElse { "Yedek hatası: ${it.message}" }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            message = runCatching {
                BackupManager.import(context, uri, password.toCharArray())
                "Yedek geri yüklendi. Ekranları yenilemek için uygulamayı yeniden aç."
            }.getOrElse { "Geri yükleme hatası veya yanlış parola: ${it.message ?: "şifre çözülemedi"}" }
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackHeader("Şifreli Yedek", onBack)
        Text("Yer imleri, Kur’an/öğrenme ilerlemesi, zikir/Ramazan ve güvenli görünüm/widget tercihleri AES-GCM ile şifrelenerek dışa aktarılır. Kadınlara özel kayıt varsayılan olarak dahil edilmez.", modifier = Modifier.padding(vertical = 12.dp))
        OutlinedTextField(
            password,
            { password = it },
            label = { Text("Yedek parolası (en az 6 karakter)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { exportLauncher.launch("Namaz-V8-Sifreli-Yedek.json") }, enabled = password.length >= 6, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Şifreli yedek oluştur") }
        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }, enabled = password.length >= 6, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Şifreli yedekten geri yükle") }
        if (message.isNotBlank()) Text(message, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun WearInfoScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackHeader("Wear OS", onBack)
        Text("Namaz V8 telefon uygulaması sıradaki namaz, vakit ve takip durumunu saat entegrasyonu için paylaşılabilir bir snapshot modelinde tutar.", modifier = Modifier.padding(top = 16.dp))
        Text("Modern Wear OS’ta telefon APK’sı her saatte otomatik olarak saat uygulaması kuramaz. Tam saat arayüzü ayrı wearable modülü/Play dağıtımı gerektirir; bu tek telefon APK’sı bunu varmış gibi göstermiyor.", modifier = Modifier.padding(top = 12.dp))
    }
}
