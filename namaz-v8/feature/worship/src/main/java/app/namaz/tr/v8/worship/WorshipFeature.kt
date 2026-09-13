package app.namaz.tr.v8.worship

import android.content.Context
import android.icu.text.SimpleDateFormat
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

data class SourcedItem(val id: String, val title: String, val body: String, val source: String, val tags: List<String> = emptyList(), val madhabNote: String? = null, val personalFatwaMayBeNeeded: Boolean = false)

object WorshipCatalog {
    val duas = listOf(
        SourcedItem("dua-rabbena", "Dünya ve ahiret iyiliği", "Rabbenâ âtinâ fid-dünyâ haseneten ve fil-âhireti haseneten ve kınâ azâben-nâr. — Rabbimiz! Bize dünyada ve ahirette iyilik ver, bizi ateş azabından koru.", "Kur’an 2:201", listOf("dua","iyilik")),
        SourcedItem("dua-ilim", "İlim duası", "Rabbi zidnî ilmâ. — Rabbim! İlmimi artır.", "Kur’an 20:114", listOf("ilim","öğrenme")),
        SourcedItem("dua-sabir", "Sabır ve sebat", "Rabbenâ efriğ aleynâ sabran ve sebbit akdâmenâ... — Rabbimiz! Üzerimize sabır yağdır, ayaklarımızı sabit kıl.", "Kur’an 2:250", listOf("sabır")),
        SourcedItem("dua-af", "Bağışlanma", "Rabbenâ lâ tuâhiznâ in nesînâ ev ahta'nâ...", "Kur’an 2:286", listOf("bağışlanma")),
        SourcedItem("dua-aile", "Aile huzuru", "Rabbenâ heb lenâ min ezvâcinâ ve zürriyyâtinâ kurrete a'yun...", "Kur’an 25:74", listOf("aile")),
        SourcedItem("dua-yol", "Yolculuk duası", "Sübhânellezî sehhara lenâ hâzâ ve mâ kunnâ lehû mukrinîn...", "Kur’an 43:13-14; Müslim, Hac", listOf("yolculuk")),
        SourcedItem("dua-sikinti", "Sıkıntıda teslimiyet", "Hasbunallâhu ve ni'mel vekîl. — Allah bize yeter, O ne güzel vekildir.", "Kur’an 3:173", listOf("sıkıntı")),
        SourcedItem("dua-annebaba", "Anne-baba için", "Rabbirhamhumâ kemâ rabbeyânî sağîrâ. — Rabbim! Küçüklüğümde beni yetiştirdikleri gibi onlara merhamet et.", "Kur’an 17:24", listOf("aile","anne baba"))
    )

    val knowledge = listOf(
        SourcedItem("k-abdest", "Abdestin temel farzları", "Kur’an 5:6 abdestte yüzün, kolların, başın meshi ve ayaklarla ilgili temel çerçeveyi verir. Mezhepler niyet, tertip ve mesh miktarı gibi ayrıntılarda farklılaşır.", "Kur’an 5:6; Diyanet İlmihal I", listOf("abdest","temizlik"), "Hanefî ve Şafiî ayrıntıları farklıdır."),
        SourcedItem("k-sefer", "Seferîlik", "Seferîlik yalnız şehir değişti diye otomatik oluşmaz; mesafe, yolculuk niyeti ve kalış süresi gibi şartlar mezhebe göre değerlendirilir.", "Diyanet İlmihal I — Yolculukta Namaz", listOf("sefer","seyahat"), personalFatwaMayBeNeeded = true),
        SourcedItem("k-kaza", "Kaza namazı", "Vaktinde kılınamayan farz namazların telafisiyle ilgili hükümler vardır. Uygulama geçmişten otomatik borç hesabı üretmez; kullanıcı kendi kaydını girer.", "Diyanet İlmihal I — Namaz", listOf("kaza","namaz"), personalFatwaMayBeNeeded = true),
        SourcedItem("k-oruc", "Ramazan orucu", "Oruç Bakara 2:183-185'te emredilir. Hastalık ve yolculuk gibi mazeretlerde ayrıntılı hükümler bulunur.", "Kur’an 2:183-185; Diyanet İlmihal I", listOf("oruç","ramazan"), personalFatwaMayBeNeeded = true),
        SourcedItem("k-zekat", "Zekât", "Zekât hesabı mal türü, nisap, borçlar ve üzerinden yıl geçmesi gibi şartlara göre değişebilir. Hesap aracı yalnız yardımcıdır.", "Kur’an 2:43; Diyanet İlmihal I", listOf("zekât","para"), personalFatwaMayBeNeeded = true),
        SourcedItem("k-cuma", "Cuma namazı", "Cuma çağrısı geldiğinde Allah'ın zikrine yönelmek Kur’an 62:9'da vurgulanır; cuma namazının şartları ilmihalde ayrıntılıdır.", "Kur’an 62:9; Diyanet İlmihal I", listOf("cuma","namaz")),
        SourcedItem("k-kulhakki", "Kul hakkı", "Emaneti sahibine vermek, haksızlıktan ve gıybetten sakınmak Kur’an'ın temel ahlak emirlerindendir.", "Kur’an 4:58; 49:11-12", listOf("kul hakkı","ahlak")),
        SourcedItem("k-kible", "Kıble", "Namazda kıbleye yönelmek temel şartlardandır; uygulamadaki pusula yalnız sensör yardımcısıdır, düşük doğrulukta kalibrasyon gerekir.", "Kur’an 2:144; Diyanet İlmihal I", listOf("kıble","namaz"))
    )

    val esma: List<Pair<String,String>> = listOf(
        "Allah" to "Bütün kemal sıfatlarını kendinde toplayan özel isim", "Er-Rahmân" to "Rahmeti bütün varlıkları kuşatan", "Er-Rahîm" to "Çok merhamet eden", "El-Melik" to "Mülkün gerçek sahibi", "El-Kuddûs" to "Her noksanlıktan uzak", "Es-Selâm" to "Esenlik veren", "El-Mü'min" to "Güven veren", "El-Müheymin" to "Gözetip koruyan", "El-Azîz" to "Mutlak üstün", "El-Cebbâr" to "Kudreti her şeye yeten", "El-Mütekebbir" to "Büyüklükte eşsiz", "El-Hâlık" to "Yaratan", "El-Bâri'" to "Kusursuz var eden", "El-Musavvir" to "Şekil veren", "El-Gaffâr" to "Çok bağışlayan", "El-Kahhâr" to "Her şeye galip", "El-Vehhâb" to "Karşılıksız veren", "Er-Rezzâk" to "Rızık veren", "El-Fettâh" to "Hayır kapılarını açan", "El-Alîm" to "Her şeyi bilen", "El-Kâbıd" to "Daraltan", "El-Bâsıt" to "Genişleten", "El-Hâfıd" to "Alçaltan", "Er-Râfi'" to "Yükselten", "El-Muiz" to "İzzet veren", "El-Müzil" to "Zillete düşüren", "Es-Semî'" to "Her şeyi işiten", "El-Basîr" to "Her şeyi gören", "El-Hakem" to "Hükmeden", "El-Adl" to "Mutlak adalet sahibi", "El-Latîf" to "Lütfu ince ve gizli", "El-Habîr" to "Her şeyden haberdar", "El-Halîm" to "Cezada acele etmeyen", "El-Azîm" to "Pek yüce", "El-Gafûr" to "Çok bağışlayan", "Eş-Şekûr" to "Az amele çok karşılık veren", "El-Aliyy" to "Çok yüce", "El-Kebîr" to "Büyüklüğü sınırsız", "El-Hafîz" to "Koruyan", "El-Mukît" to "Rızık ve güç veren", "El-Hasîb" to "Hesap gören", "El-Celîl" to "Azamet sahibi", "El-Kerîm" to "Çok cömert", "Er-Rakîb" to "Gözeten", "El-Mucîb" to "Dualara cevap veren", "El-Vâsi'" to "Rahmeti ve ilmi geniş", "El-Hakîm" to "Her işi hikmetli", "El-Vedûd" to "Seven ve sevilen", "El-Mecîd" to "Şanı yüce", "El-Bâis" to "Dirilten", "Eş-Şehîd" to "Her şeye şahit", "El-Hakk" to "Varlığı ve sözü gerçek", "El-Vekîl" to "Kendisine güvenilip dayanılan", "El-Kaviyy" to "Çok güçlü", "El-Metîn" to "Kuvveti sarsılmaz", "El-Veliyy" to "Dost ve yardımcı", "El-Hamîd" to "Övgüye layık", "El-Muhsî" to "Her şeyi sayan", "El-Mübdi'" to "İlk defa yaratan", "El-Muîd" to "Yeniden yaratan", "El-Muhyî" to "Hayat veren", "El-Mümît" to "Ölümü yaratan", "El-Hayy" to "Diri", "El-Kayyûm" to "Her şeyi ayakta tutan", "El-Vâcid" to "İstediğini bulan", "El-Mâcid" to "Şanı ve cömertliği büyük", "El-Vâhid" to "Tek", "El-Ehad" to "Bir ve eşsiz", "Es-Samed" to "Her şeyin muhtaç olduğu", "El-Kâdir" to "Gücü yeten", "El-Muktedir" to "Kudreti sonsuz", "El-Mukaddim" to "Öne alan", "El-Muahhir" to "Geri bırakan", "El-Evvel" to "İlk", "El-Âhir" to "Son", "Ez-Zâhir" to "Varlığı açık", "El-Bâtın" to "Mahiyeti idrak edilemeyen", "El-Vâlî" to "Kâinatı yöneten", "El-Müteâlî" to "Çok yüce", "El-Berr" to "İyiliği bol", "Et-Tevvâb" to "Tövbeleri kabul eden", "El-Müntekim" to "Adaletle karşılık veren", "El-Afüvv" to "Affeden", "Er-Raûf" to "Çok şefkatli", "Mâlikü'l-Mülk" to "Mülkün gerçek sahibi", "Zü'l-Celâli ve'l-İkrâm" to "Celal ve ikram sahibi", "El-Muksit" to "Adaletle hükmeden", "El-Câmi'" to "Toplayan", "El-Ganiyy" to "Hiçbir şeye muhtaç olmayan", "El-Muğnî" to "Zengin eden", "El-Mâni'" to "Dilediğini engelleyen", "Ed-Dârr" to "Zararı hikmetle yaratan", "En-Nâfi'" to "Fayda veren", "En-Nûr" to "Nurlandıran", "El-Hâdî" to "Hidayet veren", "El-Bedî'" to "Eşsiz yaratan", "El-Bâkî" to "Varlığının sonu olmayan", "El-Vâris" to "Her şeyin gerçek varisi", "Er-Reşîd" to "Doğru yolu gösteren", "Es-Sabûr" to "Cezada acele etmeyen"
    )

    fun validate(): List<String> = (duas + knowledge).filter { it.id.isBlank() || it.title.isBlank() || it.source.isBlank() }.map { it.id }
}

object KnowledgeAssistant {
    fun search(query: String): List<SourcedItem> {
        val q = query.trim().lowercase(Locale("tr"))
        if (q.length < 2) return emptyList()
        return (WorshipCatalog.knowledge + WorshipCatalog.duas).filter {
            it.title.lowercase(Locale("tr")).contains(q) || it.body.lowercase(Locale("tr")).contains(q) || it.tags.any { tag -> tag.lowercase(Locale("tr")).contains(q) }
        }
    }
}

object ZakatCalculator {
    fun calculate(assets: Double, debts: Double, nisab: Double): Double {
        val net = (assets - debts).coerceAtLeast(0.0)
        return if (net >= nisab && nisab > 0) net * 0.025 else 0.0
    }
}

@Composable
fun WorshipScreen() {
    var tool by rememberSaveable { mutableStateOf<String?>(null) }
    if (tool != null) {
        when (tool) {
            "dhikr" -> DhikrScreen { tool = null }
            "duas" -> SourcedListScreen("Dualar", WorshipCatalog.duas) { tool = null }
            "esma" -> EsmaScreen { tool = null }
            "knowledge" -> KnowledgeScreen { tool = null }
            "ramadan" -> RamadanScreen { tool = null }
            "hajj" -> HajjScreen { tool = null }
            "women" -> WomenScreen { tool = null }
            "travel" -> TravelScreen { tool = null }
            "calendar" -> CalendarScreen { tool = null }
            "zakat" -> ZakatScreen { tool = null }
        }
        return
    }
    val tools = listOf("dhikr" to "Zikir Sayacı", "duas" to "Dua Kütüphanesi", "esma" to "Esmaül Hüsna", "knowledge" to "Güvenilir Bilgi", "ramadan" to "Ramazan", "hajj" to "Hac ve Umre", "women" to "Kadınlara Özel Kayıt", "travel" to "Seyahat Rehberi", "calendar" to "İslami Takvim", "zakat" to "Zekât Hesap Yardımcısı")
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("İbadet", style = MaterialTheme.typography.headlineMedium); Text("Araçlar çevrimdışı çalışacak şekilde yerel tutulur; dini metinlerde kaynak gösterilir.", modifier = Modifier.padding(bottom = 8.dp)) }
        items(tools) { pair -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(pair.second, style = MaterialTheme.typography.titleMedium); OutlinedButton(onClick = { tool = pair.first }) { Text("Aç") } } } }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable private fun BackTitle(title: String, onBack: () -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = onBack) { Text("←") }; Text(title, style = MaterialTheme.typography.headlineSmall) } }

@Composable
private fun DhikrScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dhikr_v8", Context.MODE_PRIVATE) }
    var count by rememberSaveable { mutableIntStateOf(prefs.getInt("count", 0)) }
    var target by rememberSaveable { mutableIntStateOf(prefs.getInt("target", 33)) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackTitle("Zikir Sayacı", onBack)
        Text("$count / $target", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(vertical = 24.dp))
        Button(onClick = { count++; prefs.edit().putInt("count", count).apply(); (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)) }, modifier = Modifier.fillMaxWidth()) { Text("+1") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) { listOf(33,99,100).forEach { n -> OutlinedButton(onClick = { target = n; prefs.edit().putInt("target", n).apply() }) { Text("$n") } } }
        OutlinedButton(onClick = { count = 0; prefs.edit().putInt("count", 0).apply() }, modifier = Modifier.padding(top = 8.dp)) { Text("Sıfırla") }
        Text("Zikir sayısı bir ibadet hükmü üretmez; sayaç yalnız kişisel takip aracıdır.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun SourcedListScreen(title: String, list: List<SourcedItem>, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) { item { BackTitle(title, onBack) }; items(list) { d -> Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) { Column(Modifier.padding(12.dp)) { Text(d.title, fontWeight = FontWeight.Bold); Text(d.body); Text("Kaynak: ${d.source}", style = MaterialTheme.typography.bodySmall) } } }; item { Spacer(Modifier.height(32.dp)) } }
}

@Composable
private fun EsmaScreen(onBack: () -> Unit) { LazyColumn(Modifier.fillMaxSize().padding(16.dp)) { item { BackTitle("Esmaül Hüsna", onBack); Text("99 güzel isim — kısa anlamlar öğretici özet olarak verilmiştir.") }; items(WorshipCatalog.esma) { (n,m) -> Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Column(Modifier.padding(10.dp)) { Text(n, fontWeight = FontWeight.Bold); Text(m) } } }; item { Text("Kaynak çerçevesi: Kur’an ve sahih hadislerde geçen esmâ; anlamlar uygulama için kısa özetlenmiştir.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp)) } } }

@Composable
private fun KnowledgeScreen(onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }; var results by remember { mutableStateOf<List<SourcedItem>>(emptyList()) }; var searched by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackTitle("Güvenilir Bilgi", onBack)
        Text("Bu arama yalnız yerel ve kaynaklı maddeleri getirir; kaynak bulamazsa cevap uydurmaz.")
        OutlinedTextField(query, { query = it }, label = { Text("Örn. abdest, sefer, zekât") }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        Button(onClick = { results = KnowledgeAssistant.search(query); searched = true }) { Text("Kaynaklarda ara") }
        if (searched && results.isEmpty()) Text("Bu soru için doğrulanmış yerel kaynak bulamadım. Kişisel fetva gerekiyorsa ehil bir din görevlisine danış.", modifier = Modifier.padding(top = 16.dp))
        LazyColumn { items(results) { r -> Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) { Column(Modifier.padding(12.dp)) { Text(r.title, fontWeight = FontWeight.Bold); Text(r.body); r.madhabNote?.let { Text("Mezhep notu: $it") }; if (r.personalFatwaMayBeNeeded) Text("Bu konu kişisel şartlara göre fetva gerektirebilir.", fontWeight = FontWeight.Bold); Text("Kaynak: ${r.source}", style = MaterialTheme.typography.bodySmall) } } } }
    }
}

@Composable
private fun RamadanScreen(onBack: () -> Unit) {
    val context = LocalContext.current; val prefs = remember { context.getSharedPreferences("ramadan_v8", Context.MODE_PRIVATE) }
    var fasted by rememberSaveable { mutableIntStateOf(prefs.getInt("fasted", 0)) }; var pages by rememberSaveable { mutableIntStateOf(prefs.getInt("pages", 0)) }
    Column(Modifier.fillMaxSize().padding(16.dp)) { BackTitle("Ramazan", onBack); Text("Oruç kaydı: $fasted gün"); Row { Button(onClick = { fasted++; prefs.edit().putInt("fasted", fasted).apply() }) { Text("Bugün tuttum") }; OutlinedButton(onClick = { if (fasted > 0) fasted--; prefs.edit().putInt("fasted", fasted).apply() }, modifier = Modifier.padding(start = 8.dp)) { Text("Düzelt -1") } }; Text("Mukabele: $pages sayfa", modifier = Modifier.padding(top = 12.dp)); Button(onClick = { pages++; prefs.edit().putInt("pages", pages).apply() }) { Text("+1 sayfa") }; Text("İmsak/iftar saatini Bugün ekranındaki namaz motorundan takip et. Fitre/fidye tutarları yıllara ve resmî açıklamalara göre değişebileceğinden uygulama sabit para tutarı uydurmaz.", modifier = Modifier.padding(top = 16.dp)); Text("Kaynak: Kur’an 2:183-185; Diyanet Ramazan rehberleri", style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun HajjScreen(onBack: () -> Unit) {
    val steps = listOf("İhram ve niyet", "Telbiye", "Tavaf", "Sa'y", "Arafat vakfesi", "Müzdelife", "Mina ve cemreler", "Kurban/saç tıraşı ilgili hac türüne göre", "Veda tavafı")
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) { item { BackTitle("Hac ve Umre", onBack); Text("Adımlar genel öğrenme sırasıdır; hac türü ve kişisel durum ayrıntıları resmî rehberle kontrol edilmelidir.") }; items(steps) { s -> var checked by rememberSaveable(s) { mutableStateOf(false) }; Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Checkbox(checked, { checked = it }); Text(s, modifier = Modifier.padding(top = 12.dp)) } }; item { Text("Kaynak: Kur’an 2:196-203; Diyanet Hac ve Umre Rehberi", style = MaterialTheme.typography.bodySmall) } }
}

@Composable
private fun WomenScreen(onBack: () -> Unit) {
    val context = LocalContext.current; val prefs = remember { context.getSharedPreferences("private_worship_v8", Context.MODE_PRIVATE) }
    var paused by rememberSaveable { mutableStateOf(prefs.getBoolean("prayer_pause", false)) }; var qadaFast by rememberSaveable { mutableIntStateOf(prefs.getInt("qada_fast", 0)) }
    Column(Modifier.fillMaxSize().padding(16.dp)) { BackTitle("Kadınlara Özel Kayıt", onBack); Text("Bu bölüm yalnız sen açarsan kullanılır ve veriler cihazda tutulur. Varsayılan yedeğe dahil edilmez."); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Namaz takibini geçici duraklat"); Switch(paused, { paused = it; prefs.edit().putBoolean("prayer_pause", it).apply() }) }; Text("Oruç kaza kaydı: $qadaFast", modifier = Modifier.padding(top = 16.dp)); Row { Button(onClick = { qadaFast++; prefs.edit().putInt("qada_fast", qadaFast).apply() }) { Text("+1") }; OutlinedButton(onClick = { if (qadaFast > 0) qadaFast--; prefs.edit().putInt("qada_fast", qadaFast).apply() }, modifier = Modifier.padding(start = 8.dp)) { Text("-1") } }; Text("Özel durumlarda ibadet hükümleri ayrıntılıdır; sağlık ve fıkhî durum için güvenilir uzmanlara danış.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp)) }
}

@Composable
private fun TravelScreen(onBack: () -> Unit) { Column(Modifier.fillMaxSize().padding(16.dp)) { BackTitle("Seyahat Rehberi", onBack); Text("Şehir değişikliğinde vakitleri güncellemek gerekir. Ancak şehir değişmesi seni otomatik olarak seferî yapmaz.", modifier = Modifier.padding(top = 12.dp)); Text("Kontrol et:", fontWeight = FontWeight.Bold); listOf("Yolculuğun mesafesi", "Yolculuk niyeti", "Gidilen yerde kalış süresi", "Mezhep uygulaman", "Namazı kısaltma/cem ayrıntıları").forEach { Text("• $it") }; Text("Kaynak: Diyanet İlmihal I — Yolculukta Namaz. Kişisel durum için ehil din görevlisine danış.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp)) } }

@Composable
private fun CalendarScreen(onBack: () -> Unit) {
    val cal = remember { IslamicCalendar() }; val fmt = remember { SimpleDateFormat("dd MMMM yyyy", Locale("tr")) }
    val hijri = "${cal.get(IslamicCalendar.DAY_OF_MONTH)} ${hijriMonth(cal.get(IslamicCalendar.MONTH))} ${cal.get(IslamicCalendar.YEAR)}"
    Column(Modifier.fillMaxSize().padding(16.dp)) { BackTitle("İslami Takvim", onBack); Text("Bugün", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp)); Text(hijri, style = MaterialTheme.typography.headlineMedium); Text(fmt.format(java.util.Date())); Text("Hicrî takvim hesapları gözlem/resmî ilanlara göre bir gün farklılık gösterebilir. Ramazan ve bayram başlangıcında yerel resmî duyuruyu esas al.", modifier = Modifier.padding(top = 16.dp)) }
}
private fun hijriMonth(i: Int) = listOf("Muharrem","Safer","Rebiülevvel","Rebiülahir","Cemaziyelevvel","Cemaziyelahir","Recep","Şaban","Ramazan","Şevval","Zilkade","Zilhicce").getOrElse(i){""}

@Composable
private fun ZakatScreen(onBack: () -> Unit) {
    var assets by rememberSaveable { mutableStateOf("") }; var debts by rememberSaveable { mutableStateOf("") }; var nisab by rememberSaveable { mutableStateOf("") }; var result by remember { mutableStateOf<Double?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) { BackTitle("Zekât Hesap Yardımcısı", onBack); Text("Bu araç fetva değil, yaklaşık matematik yardımcısıdır. Altın/gümüş/ticaret malları ve borçların ayrıntılı hükümlerini ayrıca kontrol et."); OutlinedTextField(assets,{assets=it},label={Text("Zekâta tabi varlık toplamı")},modifier=Modifier.fillMaxWidth().padding(top=8.dp)); OutlinedTextField(debts,{debts=it},label={Text("Düşülebilecek borçlar")},modifier=Modifier.fillMaxWidth()); OutlinedTextField(nisab,{nisab=it},label={Text("Güncel nisap parasal karşılığı")},modifier=Modifier.fillMaxWidth()); Button(onClick={result=ZakatCalculator.calculate(assets.toDoubleOrNull()?:0.0,debts.toDoubleOrNull()?:0.0,nisab.toDoubleOrNull()?:0.0)},modifier=Modifier.padding(top=8.dp)){Text("Hesapla")}; result?.let { Text(if(it>0) "Yaklaşık zekât: %.2f".format(it) else "Girilen değerlere göre yaklaşık zekât çıkmıyor; fıkhî şartları ayrıca kontrol et.", style=MaterialTheme.typography.titleMedium, modifier=Modifier.padding(top=12.dp)) }; Text("Kaynak çerçevesi: Diyanet İlmihal I — Zekât", style=MaterialTheme.typography.bodySmall, modifier=Modifier.padding(top=12.dp)) }
}
