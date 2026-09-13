package app.namaz.tr.v8.worship

import android.content.Context
import android.icu.util.IslamicCalendar
import android.os.VibrationEffect
import android.os.Vibrator
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

data class SourcedItem(
    val id: String,
    val title: String,
    val body: String,
    val source: String,
    val tags: List<String> = emptyList(),
    val madhabNote: String? = null,
    val personalFatwaMayBeNeeded: Boolean = false,
)

object WorshipCatalog {
    val divineName = "Allah" to "Bütün kemal sıfatlarını kendinde toplayan özel isim"

    val duas = listOf(
        SourcedItem("rabbena", "Dünya ve ahiret iyiliği", "Rabbenâ âtinâ fid-dünyâ haseneten ve fil-âhireti haseneten ve kınâ azâben-nâr. Rabbimiz! Bize dünyada ve ahirette iyilik ver, bizi ateş azabından koru.", "Kur’an 2:201", listOf("dua", "iyilik")),
        SourcedItem("ilim", "İlim duası", "Rabbi zidnî ilmâ. Rabbim! İlmimi artır.", "Kur’an 20:114", listOf("ilim", "öğrenme")),
        SourcedItem("sabir", "Sabır ve sebat", "Rabbenâ efriğ aleynâ sabran ve sebbit akdâmenâ... Rabbimiz! Üzerimize sabır yağdır, ayaklarımızı sabit kıl.", "Kur’an 2:250", listOf("sabır")),
        SourcedItem("af", "Bağışlanma", "Rabbenâ lâ tuâhiznâ in nesînâ ev ahta'nâ...", "Kur’an 2:286", listOf("bağışlanma")),
        SourcedItem("aile", "Aile huzuru", "Rabbenâ heb lenâ min ezvâcinâ ve zürriyyâtinâ kurrete a'yun...", "Kur’an 25:74", listOf("aile")),
        SourcedItem("yol", "Yolculuk duası", "Sübhânellezî sehhara lenâ hâzâ ve mâ kunnâ lehû mukrinîn...", "Kur’an 43:13-14; Müslim, Hac", listOf("yolculuk")),
        SourcedItem("sikinti", "Sıkıntıda teslimiyet", "Hasbunallâhu ve ni'mel vekîl. Allah bize yeter, O ne güzel vekildir.", "Kur’an 3:173", listOf("sıkıntı")),
        SourcedItem("annebaba", "Anne-baba için", "Rabbirhamhumâ kemâ rabbeyânî sağîrâ. Rabbim! Küçüklüğümde beni yetiştirdikleri gibi onlara merhamet et.", "Kur’an 17:24", listOf("aile", "anne baba")),
    )

    val knowledge = listOf(
        SourcedItem("abdest", "Abdestin temel farzları", "Kur’an 5:6 abdestte yüzün, kolların, başın meshi ve ayaklarla ilgili temel çerçeveyi verir. Mezhepler niyet, tertip ve mesh miktarı gibi ayrıntılarda farklılaşır.", "Kur’an 5:6; Diyanet İlmihal I", listOf("abdest", "temizlik"), "Hanefî ve Şafiî ayrıntıları farklıdır."),
        SourcedItem("sefer", "Seferîlik", "Seferîlik yalnız şehir değişti diye otomatik oluşmaz; mesafe, yolculuk niyeti ve kalış süresi gibi şartlar mezhebe göre değerlendirilir.", "Diyanet İlmihal I — Yolculukta Namaz", listOf("sefer", "seyahat"), personalFatwaMayBeNeeded = true),
        SourcedItem("kaza", "Kaza namazı", "Vaktinde kılınamayan farz namazların telafisiyle ilgili hükümler vardır. Uygulama geçmişten otomatik borç hesabı üretmez; kullanıcı kendi kaydını girer.", "Diyanet İlmihal I — Namaz", listOf("kaza", "namaz"), personalFatwaMayBeNeeded = true),
        SourcedItem("oruc", "Ramazan orucu", "Oruç Bakara 2:183-185'te emredilir. Hastalık ve yolculuk gibi mazeretlerde ayrıntılı hükümler bulunur.", "Kur’an 2:183-185; Diyanet İlmihal I", listOf("oruç", "ramazan"), personalFatwaMayBeNeeded = true),
        SourcedItem("zekat", "Zekât", "Zekât hesabı mal türü, nisap, borçlar ve üzerinden yıl geçmesi gibi şartlara göre değişebilir. Hesap aracı yalnız yardımcıdır.", "Kur’an 2:43; Diyanet İlmihal I", listOf("zekât", "para"), personalFatwaMayBeNeeded = true),
        SourcedItem("cuma", "Cuma namazı", "Cuma çağrısı geldiğinde Allah'ın zikrine yönelmek Kur’an 62:9'da vurgulanır; şartlar ilmihalde ayrıntılıdır.", "Kur’an 62:9; Diyanet İlmihal I", listOf("cuma", "namaz")),
        SourcedItem("kulhakki", "Kul hakkı", "Emaneti sahibine vermek, haksızlıktan ve gıybetten sakınmak Kur’an'ın temel ahlak emirlerindendir.", "Kur’an 4:58; 49:11-12", listOf("kul hakkı", "ahlak")),
        SourcedItem("kible", "Kıble", "Namazda kıbleye yönelmek temel şartlardandır; uygulamadaki pusula yalnız sensör yardımcısıdır, düşük doğrulukta kalibrasyon gerekir.", "Kur’an 2:144; Diyanet İlmihal I", listOf("kıble", "namaz")),
    )

    private val esmaRaw = """
Er-Rahmân|Rahmeti bütün varlıkları kuşatan
Er-Rahîm|Çok merhamet eden
El-Melik|Mülkün gerçek sahibi
El-Kuddûs|Her noksanlıktan uzak
Es-Selâm|Esenlik veren
El-Mü'min|Güven veren
El-Müheymin|Gözetip koruyan
El-Azîz|Mutlak üstün
El-Cebbâr|Kudreti her şeye yeten
El-Mütekebbir|Büyüklükte eşsiz
El-Hâlık|Yaratan
El-Bâri'|Kusursuz var eden
El-Musavvir|Şekil veren
El-Gaffâr|Çok bağışlayan
El-Kahhâr|Her şeye galip
El-Vehhâb|Karşılıksız veren
Er-Rezzâk|Rızık veren
El-Fettâh|Hayır kapılarını açan
El-Alîm|Her şeyi bilen
El-Kâbıd|Daraltan
El-Bâsıt|Genişleten
El-Hâfıd|Alçaltan
Er-Râfi'|Yükselten
El-Muiz|İzzet veren
El-Müzil|Zillete düşüren
Es-Semî'|Her şeyi işiten
El-Basîr|Her şeyi gören
El-Hakem|Hükmeden
El-Adl|Mutlak adalet sahibi
El-Latîf|Lütfu ince ve gizli
El-Habîr|Her şeyden haberdar
El-Halîm|Cezada acele etmeyen
El-Azîm|Pek yüce
El-Gafûr|Çok bağışlayan
Eş-Şekûr|Az amele çok karşılık veren
El-Aliyy|Çok yüce
El-Kebîr|Büyüklüğü sınırsız
El-Hafîz|Koruyan
El-Mukît|Rızık ve güç veren
El-Hasîb|Hesap gören
El-Celîl|Azamet sahibi
El-Kerîm|Çok cömert
Er-Rakîb|Gözeten
El-Mucîb|Dualara cevap veren
El-Vâsi'|Rahmeti ve ilmi geniş
El-Hakîm|Her işi hikmetli
El-Vedûd|Seven ve sevilen
El-Mecîd|Şanı yüce
El-Bâis|Dirilten
Eş-Şehîd|Her şeye şahit
El-Hakk|Varlığı ve sözü gerçek
El-Vekîl|Kendisine güvenilip dayanılan
El-Kaviyy|Çok güçlü
El-Metîn|Kuvveti sarsılmaz
El-Veliyy|Dost ve yardımcı
El-Hamîd|Övgüye layık
El-Muhsî|Her şeyi sayan
El-Mübdi'|İlk defa yaratan
El-Muîd|Yeniden yaratan
El-Muhyî|Hayat veren
El-Mümît|Ölümü yaratan
El-Hayy|Diri
El-Kayyûm|Her şeyi ayakta tutan
El-Vâcid|İstediğini bulan
El-Mâcid|Şanı ve cömertliği büyük
El-Vâhid|Tek
El-Ehad|Bir ve eşsiz
Es-Samed|Her şeyin muhtaç olduğu
El-Kâdir|Gücü yeten
El-Muktedir|Kudreti sonsuz
El-Mukaddim|Öne alan
El-Muahhir|Geri bırakan
El-Evvel|İlk
El-Âhir|Son
Ez-Zâhir|Varlığı açık
El-Bâtın|Mahiyeti idrak edilemeyen
El-Vâlî|Kâinatı yöneten
El-Müteâlî|Çok yüce
El-Berr|İyiliği bol
Et-Tevvâb|Tövbeleri kabul eden
El-Müntekim|Adaletle karşılık veren
El-Afüvv|Affeden
Er-Raûf|Çok şefkatli
Mâlikü'l-Mülk|Mülkün gerçek sahibi
Zü'l-Celâli ve'l-İkrâm|Celal ve ikram sahibi
El-Muksit|Adaletle hükmeden
El-Câmi'|Toplayan
El-Ganiyy|Hiçbir şeye muhtaç olmayan
El-Muğnî|Zengin eden
El-Mâni'|Dilediğini engelleyen
Ed-Dârr|Zararı hikmetle yaratan
En-Nâfi'|Fayda veren
En-Nûr|Nurlandıran
El-Hâdî|Hidayet veren
El-Bedî'|Eşsiz yaratan
El-Bâkî|Varlığının sonu olmayan
El-Vâris|Her şeyin gerçek varisi
Er-Reşîd|Doğru yolu gösteren
Es-Sabûr|Cezada acele etmeyen
""".trimIndent()

    val esma: List<Pair<String, String>> = esmaRaw.lines().filter { it.isNotBlank() }.map { line ->
        val parts = line.split('|', limit = 2)
        parts[0] to parts[1]
    }

    fun validate(): List<String> = (duas + knowledge)
        .filter { it.id.isBlank() || it.title.isBlank() || it.source.isBlank() }
        .map { it.id }
}

object KnowledgeAssistant {
    fun search(query: String): List<SourcedItem> {
        val q = query.trim().lowercase(Locale("tr"))
        if (q.length < 2) return emptyList()
        return (WorshipCatalog.knowledge + WorshipCatalog.duas).filter { item ->
            item.title.lowercase(Locale("tr")).contains(q) ||
                item.body.lowercase(Locale("tr")).contains(q) ||
                item.tags.any { it.lowercase(Locale("tr")).contains(q) }
        }
    }
}

object ZakatCalculator {
    fun calculate(assets: Double, debts: Double, nisab: Double): Double {
        val net = (assets - debts).coerceAtLeast(0.0)
        return if (nisab > 0 && net >= nisab) net * 0.025 else 0.0
    }
}

@Composable
fun WorshipScreen() {
    var tool by rememberSaveable { mutableStateOf<String?>(null) }
    when (tool) {
        "dhikr" -> return DhikrScreen { tool = null }
        "duas" -> return SourcedListScreen("Dualar", WorshipCatalog.duas) { tool = null }
        "esma" -> return EsmaScreen { tool = null }
        "knowledge" -> return KnowledgeScreen { tool = null }
        "ramadan" -> return RamadanScreen { tool = null }
        "hajj" -> return HajjScreen { tool = null }
        "women" -> return WomenScreen { tool = null }
        "travel" -> return TravelScreen { tool = null }
        "calendar" -> return CalendarScreen { tool = null }
        "zakat" -> return ZakatScreen { tool = null }
    }
    val tools = listOf(
        "dhikr" to "Zikir Sayacı", "duas" to "Dua Kütüphanesi", "esma" to "Esmaül Hüsna",
        "knowledge" to "Güvenilir Bilgi", "ramadan" to "Ramazan", "hajj" to "Hac ve Umre",
        "women" to "Kadınlara Özel Kayıt", "travel" to "Seyahat Rehberi",
        "calendar" to "İslami Takvim", "zakat" to "Zekât Hesap Yardımcısı",
    )
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("İbadet", style = MaterialTheme.typography.headlineMedium)
            Text("Araçlar yerel çalışır; dini metinlerde kaynak görünür.", modifier = Modifier.padding(bottom = 8.dp))
        }
        items(tools) { (id, title) ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { tool = id }) { Text("Aç") }
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onBack) { Text("←") }
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun DhikrScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dhikr_v8", Context.MODE_PRIVATE) }
    var count by rememberSaveable { mutableIntStateOf(prefs.getInt("count", 0)) }
    var target by rememberSaveable { mutableIntStateOf(prefs.getInt("target", 33)) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Header("Zikir Sayacı", onBack)
        Text("$count / $target", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(vertical = 24.dp))
        Button(onClick = {
            count++
            prefs.edit().putInt("count", count).apply()
            (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
                ?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        }, modifier = Modifier.fillMaxWidth()) { Text("+1") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            listOf(33, 99, 100).forEach { n -> OutlinedButton(onClick = { target = n; prefs.edit().putInt("target", n).apply() }) { Text("$n") } }
        }
        OutlinedButton(onClick = { count = 0; prefs.edit().putInt("count", 0).apply() }, modifier = Modifier.padding(top = 8.dp)) { Text("Sıfırla") }
        Text("Sayaç kişisel takip aracıdır; tek başına dini hüküm üretmez.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun SourcedListScreen(title: String, data: List<SourcedItem>, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { Header(title, onBack) }
        items(data) { item ->
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(item.title, fontWeight = FontWeight.Bold)
                    Text(item.body)
                    Text("Kaynak: ${item.source}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun EsmaScreen(onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Header("Esmaül Hüsna", onBack)
            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(WorshipCatalog.divineName.first, fontWeight = FontWeight.Bold)
                    Text(WorshipCatalog.divineName.second)
                    Text("Özel isim — 99’lu numaralandırmanın dışında ayrıca gösterilir.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("99 Esmâ", style = MaterialTheme.typography.titleLarge)
        }
        items(WorshipCatalog.esma) { (name, meaning) ->
            Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Column(Modifier.padding(10.dp)) { Text(name, fontWeight = FontWeight.Bold); Text(meaning) }
            }
        }
        item { Text("Kaynak çerçevesi: Diyanet Din İşleri Yüksek Kurulu — Allah’ın 99 ismi; anlamlar uygulama için kısa özetlenmiştir.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp)) }
    }
}

@Composable
private fun KnowledgeScreen(onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SourcedItem>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Header("Güvenilir Bilgi", onBack)
        Text("Yalnız yerel ve kaynaklı maddeleri getirir; kaynak bulamazsa cevap uydurmaz.")
        OutlinedTextField(query, { query = it }, label = { Text("Örn. abdest, sefer, zekât") }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        Button(onClick = { results = KnowledgeAssistant.search(query); searched = true }) { Text("Kaynaklarda ara") }
        if (searched && results.isEmpty()) Text("Doğrulanmış yerel kaynak bulamadım. Kişisel fetva gerekiyorsa ehil bir din görevlisine danış.", modifier = Modifier.padding(top = 16.dp))
        LazyColumn {
            items(results) { item ->
                Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(item.title, fontWeight = FontWeight.Bold)
                        Text(item.body)
                        item.madhabNote?.let { Text("Mezhep notu: $it") }
                        if (item.personalFatwaMayBeNeeded) Text("Bu konu kişisel şartlara göre fetva gerektirebilir.", fontWeight = FontWeight.Bold)
                        Text("Kaynak: ${item.source}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RamadanScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ramadan_v8", Context.MODE_PRIVATE) }
    var fasted by rememberSaveable { mutableIntStateOf(prefs.getInt("fasted", 0)) }
    var pages by rememberSaveable { mutableIntStateOf(prefs.getInt("pages", 0)) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Header("Ramazan", onBack)
        Text("Oruç kaydı: $fasted gün", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { fasted++; prefs.edit().putInt("fasted", fasted).apply() }) { Text("Bugün tuttum") }
            OutlinedButton(onClick = { if (fasted > 0) fasted--; prefs.edit().putInt("fasted", fasted).apply() }) { Text("-1") }
        }
        Text("Mukabele: $pages sayfa", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        Button(onClick = { pages++; prefs.edit().putInt("pages", pages).apply() }) { Text("+1 sayfa") }
        Text("İmsak/iftar saatini Bugün ekranındaki namaz motorundan takip et. Fitre/fidye parasal tutarı yıla göre değiştiği için uygulama sabit rakam uydurmaz.", modifier = Modifier.padding(top = 16.dp))
        Text("Kaynak: Kur’an 2:183-185; Diyanet Ramazan rehberleri", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HajjScreen(onBack: () -> Unit) {
    val steps = listOf("İhram ve niyet", "Telbiye", "Tavaf", "Sa'y", "Arafat vakfesi", "Müzdelife", "Mina ve cemreler", "Kurban/saç tıraşı ilgili hac türüne göre", "Veda tavafı")
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { Header("Hac ve Umre", onBack); Text("Genel öğrenme sırasıdır; hac türü ve kişisel durum ayrıntılarını resmî rehberle kontrol et.") }
        items(steps) { step ->
            var checked by rememberSaveable(step) { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Checkbox(checked, { checked = it }); Text(step, modifier = Modifier.padding(top = 12.dp)) }
        }
        item { Text("Kaynak: Kur’an 2:196-203; Diyanet Hac ve Umre Rehberi", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun WomenScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("private_worship_v8", Context.MODE_PRIVATE) }
    var paused by rememberSaveable { mutableStateOf(prefs.getBoolean("prayer_pause", false)) }
    var qadaFast by rememberSaveable { mutableIntStateOf(prefs.getInt("qada_fast", 0)) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Header("Kadınlara Özel Kayıt", onBack)
        Text("Bu bölüm yalnız sen açarsan kullanılır; veriler cihazda tutulur ve varsayılan yedeğe dahil edilmez.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Namaz takibini geçici duraklat", modifier = Modifier.weight(1f))
            Switch(paused, { paused = it; prefs.edit().putBoolean("prayer_pause", it).apply() })
        }
        Text("Oruç kaza kaydı: $qadaFast", modifier = Modifier.padding(top = 16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { qadaFast++; prefs.edit().putInt("qada_fast", qadaFast).apply() }) { Text("+1") }
            OutlinedButton(onClick = { if (qadaFast > 0) qadaFast--; prefs.edit().putInt("qada_fast", qadaFast).apply() }) { Text("-1") }
        }
        Text("Özel durumlarda sağlık ve fıkhî ayrıntı için güvenilir uzmanlara danış.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun TravelScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Header("Seyahat Rehberi", onBack)
        Text("Şehir değişikliğinde vakitleri güncelle. Şehir değişmesi seni otomatik olarak seferî yapmaz.", modifier = Modifier.padding(top = 12.dp))
        Text("Kontrol et:", fontWeight = FontWeight.Bold)
        listOf("Yolculuğun mesafesi", "Yolculuk niyeti", "Gidilen yerde kalış süresi", "Mezhep uygulaman", "Kasr/cem ayrıntıları").forEach { Text("• $it") }
        Text("Kaynak: Diyanet İlmihal I — Yolculukta Namaz. Kişisel durum için ehil din görevlisine danış.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun CalendarScreen(onBack: () -> Unit) {
    val cal = remember { IslamicCalendar() }
    val months = listOf("Muharrem", "Safer", "Rebiülevvel", "Rebiülahir", "Cemaziyelevvel", "Cemaziyelahir", "Recep", "Şaban", "Ramazan", "Şevval", "Zilkade", "Zilhicce")
    val hijri = "${cal.get(IslamicCalendar.DAY_OF_MONTH)} ${months.getOrElse(cal.get(IslamicCalendar.MONTH)) { "" }} ${cal.get(IslamicCalendar.YEAR)}"
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Header("İslami Takvim", onBack)
        Text("Bugün", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        Text(hijri, style = MaterialTheme.typography.headlineMedium)
        Text("Hicrî hesap gözlem/resmî ilana göre bir gün farklılık gösterebilir. Ramazan ve bayram başlangıcında yerel resmî duyuruyu esas al.", modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun ZakatScreen(onBack: () -> Unit) {
    var assets by rememberSaveable { mutableStateOf("") }
    var debts by rememberSaveable { mutableStateOf("") }
    var nisab by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<Double?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Header("Zekât Hesap Yardımcısı", onBack)
        Text("Fetva değil, yaklaşık matematik yardımcısıdır. Mal türü ve borç hükümlerini ayrıca kontrol et.")
        OutlinedTextField(assets, { assets = it }, label = { Text("Zekâta tabi varlık toplamı") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        OutlinedTextField(debts, { debts = it }, label = { Text("Düşülebilecek borçlar") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(nisab, { nisab = it }, label = { Text("Güncel nisap parasal karşılığı") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { result = ZakatCalculator.calculate(assets.toDoubleOrNull() ?: 0.0, debts.toDoubleOrNull() ?: 0.0, nisab.toDoubleOrNull() ?: 0.0) }, modifier = Modifier.padding(top = 8.dp)) { Text("Hesapla") }
        result?.let { value -> Text(if (value > 0) "Yaklaşık zekât: %.2f".format(value) else "Girilen değerlere göre yaklaşık zekât çıkmıyor; fıkhî şartları ayrıca kontrol et.", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
        Text("Kaynak çerçevesi: Diyanet İlmihal I — Zekât", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
    }
}
