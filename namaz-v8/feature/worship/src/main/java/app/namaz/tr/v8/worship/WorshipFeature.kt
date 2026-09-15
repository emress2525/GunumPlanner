package app.namaz.tr.v8.worship

import android.content.Context
import android.icu.util.IslamicCalendar
import android.os.Build
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
import java.time.LocalDate
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

data class ClaimCheckResult(
    val matched: List<SourcedItem>,
    val explanation: String,
)

object WorshipCatalog {
    val divineName = "Allah" to "Bütün kemal sıfatlarını kendinde toplayan özel isim"

    val duas = listOf(
        SourcedItem("rabbena", "Dünya ve ahiret iyiliği", "Rabbenâ âtinâ fid-dünyâ haseneten ve fil-âhireti haseneten ve kınâ azâben-nâr. Rabbimiz! Bize dünyada ve ahirette iyilik ver, bizi ateş azabından koru.", "Kur’an 2:201", listOf("dua", "iyilik", "namaz sonrası")),
        SourcedItem("ilim", "İlim duası", "Rabbi zidnî ilmâ. Rabbim! İlmimi artır.", "Kur’an 20:114", listOf("ilim", "öğrenme")),
        SourcedItem("sabir", "Sabır ve sebat", "Rabbenâ efriğ aleynâ sabran ve sebbit akdâmenâ... Rabbimiz! Üzerimize sabır yağdır, ayaklarımızı sabit kıl.", "Kur’an 2:250", listOf("sabır", "sıkıntı")),
        SourcedItem("af", "Bağışlanma", "Rabbenâ lâ tuâhiznâ in nesînâ ev ahta'nâ...", "Kur’an 2:286", listOf("bağışlanma", "akşam")),
        SourcedItem("aile", "Aile huzuru", "Rabbenâ heb lenâ min ezvâcinâ ve zürriyyâtinâ kurrete a'yun...", "Kur’an 25:74", listOf("aile", "çocuk")),
        SourcedItem("yol", "Yolculuk duası", "Sübhânellezî sehhara lenâ hâzâ ve mâ kunnâ lehû mukrinîn...", "Kur’an 43:13-14; Müslim, Hac", listOf("yolculuk", "seyahat")),
        SourcedItem("sikinti", "Sıkıntıda teslimiyet", "Hasbunallâhu ve ni'mel vekîl. Allah bize yeter, O ne güzel vekildir.", "Kur’an 3:173", listOf("sıkıntı", "korku")),
        SourcedItem("annebaba", "Anne-baba için", "Rabbirhamhumâ kemâ rabbeyânî sağîrâ. Rabbim! Küçüklüğümde beni yetiştirdikleri gibi onlara merhamet et.", "Kur’an 17:24", listOf("aile", "anne baba")),
        SourcedItem("sukur", "Şükür duası", "Rabbim, bana ve anne-babama verdiğin nimete şükretmeyi ve razı olacağın işler yapmayı nasip et.", "Kur’an 27:19 — anlam özeti", listOf("şükür", "sabah")),
        SourcedItem("uyku", "Uyku öncesi", "Allahım, senin adınla ölür ve dirilirim anlamındaki kısa uyku duası sünnette aktarılmıştır.", "Buhârî, Daavât — anlam özeti", listOf("uyku", "gece")),
    )

    val hadiths = listOf(
        SourcedItem("niyet", "Ameller niyetlere göredir", "Hadisin kısa anlamı: amellerin değeri niyetlerle ilişkilidir; kişi niyet ettiği şeye göre karşılık görür.", "Buhârî, Bed’ü'l-vahy 1; Müslim, İmâre 155", listOf("niyet", "amel", "hadis")),
        SourcedItem("merhamet", "Merhamet", "Kısa anlam özeti: merhamet etmeyene merhamet olunmayacağı uyarısı yapılır.", "Buhârî, Edeb 18; Müslim, Fezâil 66", listOf("merhamet", "ahlak", "hadis")),
        SourcedItem("komsu", "Komşuluk", "Kısa anlam özeti: Cebrâil'in komşu hakkını sürekli hatırlatması, komşuluk hakkının önemini gösterir.", "Buhârî, Edeb 28; Müslim, Birr 140", listOf("komşu", "hak", "hadis")),
        SourcedItem("kolaylik", "Kolaylaştırmak", "Kısa anlam özeti: insanlara zorluk çıkarmamak, kolaylaştırmak ve müjdeleyici olmak öğütlenir.", "Buhârî, İlim 11; Müslim, Cihâd 6", listOf("kolaylık", "davet", "hadis")),
        SourcedItem("temizlik", "Temizlik", "Kısa anlam özeti: temizlik ve arınmanın imandaki yüksek değeri vurgulanır.", "Müslim, Tahâret 1", listOf("temizlik", "abdest", "hadis")),
        SourcedItem("guven", "Elinden ve dilinden emin olunan kişi", "Kısa anlam özeti: Müslümanın başkalarına diliyle ve eliyle zarar vermemesi öne çıkarılır.", "Buhârî, Îmân 4; Müslim, Îmân 64", listOf("ahlak", "zarar", "hadis")),
        SourcedItem("kardeslik", "Kardeşi için istemek", "Kısa anlam özeti: kişinin kendisi için sevdiği hayrı kardeşi için de sevmesi imanın olgunluğuyla ilişkilendirilir.", "Buhârî, Îmân 7; Müslim, Îmân 71", listOf("kardeşlik", "iyilik", "hadis")),
        SourcedItem("sadaka", "Güzel söz", "Kısa anlam özeti: güzel sözün de sadaka niteliğinde olduğu bildirilir.", "Buhârî, Edeb 34; Müslim, Zekât 56", listOf("sadaka", "söz", "hadis")),
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
        SourcedItem("kanabdest", "Kan ve abdest", "Abdesti bozan durumların ayrıntıları mezhepler arasında farklı değerlendirilir; kanama konusunda Hanefî ve Şafiî uygulaması aynı değildir.", "Diyanet İlmihal I — Abdest", listOf("kan", "abdest"), "Hanefî ve Şafiî görüşleri farklıdır.", personalFatwaMayBeNeeded = true),
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

    fun validate(): List<String> = (duas + hadiths + knowledge)
        .filter { it.id.isBlank() || it.title.isBlank() || it.source.isBlank() }
        .map { it.id }
}

object KnowledgeAssistant {
    fun search(query: String): List<SourcedItem> {
        val q = query.trim().lowercase(Locale("tr"))
        if (q.length < 2) return emptyList()
        return (WorshipCatalog.knowledge + WorshipCatalog.duas + WorshipCatalog.hadiths).filter { item ->
            item.title.lowercase(Locale("tr")).contains(q) ||
                item.body.lowercase(Locale("tr")).contains(q) ||
                item.tags.any { it.lowercase(Locale("tr")).contains(q) }
        }
    }
}

object ClaimVerifier {
    fun check(text: String): ClaimCheckResult {
        val query = text.trim().lowercase(Locale("tr"))
        if (query.length < 4) return ClaimCheckResult(emptyList(), "Kontrol için daha uzun bir metin yaz.")
        val words = query.split(Regex("[^a-zçğıöşüâîû0-9]+"))
            .filter { it.length >= 4 }
            .toSet()
        val candidates = (WorshipCatalog.hadiths + WorshipCatalog.duas + WorshipCatalog.knowledge)
            .map { item ->
                val haystack = (item.title + " " + item.body + " " + item.tags.joinToString(" ")).lowercase(Locale("tr"))
                item to words.count { haystack.contains(it) }
            }
            .filter { it.second >= 2 }
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
        return if (candidates.isEmpty()) {
            ClaimCheckResult(emptyList(), "Doğrulanmış yerel kaynakta yeterli eşleşme bulamadım. Bu, sözün kesin yanlış olduğu anlamına gelmez; yalnızca uygulamanın onu doğrulayamadığı anlamına gelir.")
        } else {
            ClaimCheckResult(candidates, "Benzer kaynaklı kayıtlar bulundu. Bu eşleşme tek başına bir sözün hadis isnadını kesin doğrulamaz; kaynak kartlarını karşılaştır.")
        }
    }
}

object ZakatCalculator {
    fun calculate(assets: Double, debts: Double, nisab: Double): Double {
        val net = (assets - debts).coerceAtLeast(0.0)
        return if (nisab > 0 && net >= nisab) net * 0.025 else 0.0
    }

    fun calculate(cash: Double, goldValue: Double, tradeGoods: Double, receivables: Double, debts: Double, nisab: Double): Double =
        calculate(cash + goldValue + tradeGoods + receivables, debts, nisab)
}

@Composable
fun WorshipScreen() {
    var tool by rememberSaveable { mutableStateOf<String?>(null) }
    when (tool) {
        "dhikr" -> return DhikrScreen { tool = null }
        "duas" -> return SourcedListScreen("Dualar", WorshipCatalog.duas) { tool = null }
        "hadith" -> return SourcedListScreen("Hadis Merkezi", WorshipCatalog.hadiths) { tool = null }
        "verify" -> return ClaimCheckScreen { tool = null }
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
        "dhikr" to "Zikir Sayacı ve Geçmiş",
        "duas" to "Dua Kütüphanesi",
        "hadith" to "Kaynaklı Hadis Merkezi",
        "verify" to "Doğrusunu Kontrol Et",
        "esma" to "Esmaül Hüsna",
        "knowledge" to "Güvenilir Bilgi",
        "ramadan" to "Ramazan",
        "hajj" to "Hac ve Umre",
        "women" to "Kadınlara Özel Kayıt",
        "travel" to "Seyahat Rehberi",
        "calendar" to "İslami Takvim",
        "zakat" to "Zekât Hesap Yardımcısı",
    )
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("İbadet", style = MaterialTheme.typography.headlineMedium)
            Text("Araçlar yerel çalışır; dini metinlerde kaynak görünür.", modifier = Modifier.padding(bottom = 8.dp))
        }
        items(tools) { (id, title) ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
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
    var title by rememberSaveable { mutableStateOf(prefs.getString("title", "Sübhanallah").orEmpty()) }
    var count by rememberSaveable { mutableIntStateOf(prefs.getInt("count", 0)) }
    var target by rememberSaveable { mutableIntStateOf(prefs.getInt("target", 33)) }
    var vibration by rememberSaveable { mutableStateOf(prefs.getBoolean("vibration", true)) }
    val todayKey = "daily_${LocalDate.now()}"
    var dailyTotal by rememberSaveable { mutableIntStateOf(prefs.getInt(todayKey, 0)) }
    val sevenDayTotal = remember(dailyTotal) {
        (0L..6L).sumOf { offset -> prefs.getInt("daily_${LocalDate.now().minusDays(offset)}", 0) }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Header("Zikir Sayacı", onBack)
        OutlinedTextField(title, {
            title = it
            prefs.edit().putString("title", it).apply()
        }, label = { Text("Zikir adı") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        Text("$count / $target", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(vertical = 20.dp))
        Button(onClick = {
            count++
            dailyTotal++
            prefs.edit().putInt("count", count).putInt(todayKey, dailyTotal).apply()
            if (vibration) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= 26) vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                else @Suppress("DEPRECATION") vibrator?.vibrate(20)
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("+1") }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
            listOf(33, 99, 100, 1000).forEach { n ->
                OutlinedButton(onClick = { target = n; prefs.edit().putInt("target", n).apply() }) { Text("$n") }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Titreşim", modifier = Modifier.padding(top = 14.dp))
            Switch(vibration, { vibration = it; prefs.edit().putBoolean("vibration", it).apply() })
        }
        Text("Bugün toplam: $dailyTotal • Son 7 gün: $sevenDayTotal", modifier = Modifier.padding(top = 8.dp))
        OutlinedButton(onClick = { count = 0; prefs.edit().putInt("count", 0).apply() }, modifier = Modifier.padding(top = 8.dp)) { Text("Aktif sayacı sıfırla") }
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
                    item.madhabNote?.let { Text("Mezhep notu: $it", style = MaterialTheme.typography.bodySmall) }
                    Text("Kaynak: ${item.source}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun ClaimCheckScreen(onBack: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<ClaimCheckResult?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Header("Doğrusunu Kontrol Et", onBack)
        Text("İnternette gördüğün dini sözü yapıştır. Uygulama yalnız kendi kaynaklı yerel kataloğunda arar; eşleşme bulamadığında hadis/ayet uydurmaz.", modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(text, { text = it }, label = { Text("Kontrol edilecek söz") }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), minLines = 4)
        Button(onClick = { result = ClaimVerifier.check(text) }) { Text("Kaynaklarda kontrol et") }
        result?.let { checked ->
            Text(checked.explanation, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
            checked.matched.forEach { item ->
                Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(item.title, fontWeight = FontWeight.Bold)
                        Text(item.body)
                        Text("Kaynak: ${item.source}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
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
                Column(Modifier.padding(10.dp)) {
                    Text(name, fontWeight = FontWeight.Bold)
                    Text(meaning)
                }
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
        OutlinedTextField(query, { query = it }, label = { Text("Örn. abdest, sefer, zekât, kan") }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
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
    var qada by rememberSaveable { mutableIntStateOf(prefs.getInt("qada", 0)) }
    var pages by rememberSaveable { mutableIntStateOf(prefs.getInt("pages", 0)) }
    var sahurNote by rememberSaveable { mutableStateOf(prefs.getString("sahur_note", "").orEmpty()) }
    var iftarNote by rememberSaveable { mutableStateOf(prefs.getString("iftar_note", "").orEmpty()) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Header("Ramazan", onBack)
        Text("Oruç kaydı: $fasted gün • Kaza kaydı: $qada", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { fasted++; prefs.edit().putInt("fasted", fasted).apply() }) { Text("Bugün tuttum") }
            OutlinedButton(onClick = { qada++; prefs.edit().putInt("qada", qada).apply() }) { Text("Kaza +1") }
        }
        Text("Mukabele: $pages sayfa", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { pages++; prefs.edit().putInt("pages", pages).apply() }) { Text("+1 sayfa") }
            OutlinedButton(onClick = { pages = (pages - 1).coerceAtLeast(0); prefs.edit().putInt("pages", pages).apply() }) { Text("−1") }
        }
        OutlinedTextField(sahurNote, { sahurNote = it; prefs.edit().putString("sahur_note", it).apply() }, label = { Text("Sahur notu / hedefi") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        OutlinedTextField(iftarNote, { iftarNote = it; prefs.edit().putString("iftar_note", it).apply() }, label = { Text("İftar notu / hedefi") }, modifier = Modifier.fillMaxWidth())
        Text("İmsak/iftar saatini Bugün ekranındaki namaz motorundan takip et. Fitre/fidye parasal tutarı yıla göre değiştiği için uygulama sabit rakam uydurmaz.", modifier = Modifier.padding(top = 14.dp))
        Text("Kaynak: Kur’an 2:183-185; Diyanet Ramazan rehberleri", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HajjScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hajj_v8", Context.MODE_PRIVATE) }
    val steps = listOf("İhram ve niyet", "Telbiye", "Tavaf", "Tavaf namazı", "Sa'y", "Arafat vakfesi", "Müzdelife", "Mina ve cemreler", "Kurban/saç tıraşı ilgili hac türüne göre", "Veda tavafı")
    val duas = listOf(
        "Telbiye" to "Lebbeyk Allâhümme lebbeyk... — hac/umre sırasında telbiye zikri.",
        "Tavaf arasında" to "Kur’an 2:201’deki dünya ve ahiret iyiliği duası okunabilir; tavafın her turu için zorunlu özel dua yoktur.",
        "Arafat" to "Arafat’ta dua, zikir, istiğfar ve salavatla meşgul olunur; kişisel dualar da yapılabilir.",
    )
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { Header("Hac ve Umre", onBack); Text("Genel öğrenme sırasıdır; hac türü ve kişisel durum ayrıntılarını resmî rehberle kontrol et.", modifier = Modifier.padding(vertical = 8.dp)) }
        items(steps) { step ->
            var checked by rememberSaveable(step) { mutableStateOf(prefs.getBoolean("step_$step", false)) }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Checkbox(checked, { checked = it; prefs.edit().putBoolean("step_$step", it).apply() })
                Text(step, modifier = Modifier.padding(top = 12.dp))
            }
        }
        item {
            Text("Offline dua kartları", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
            duas.forEach { (title, body) ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Column(Modifier.padding(12.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(body) } }
            }
            Text("Kaynak: Kur’an 2:196-203; Diyanet Hac ve Umre Rehberi", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
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
        Text("Hayız, nifas, istihaza ve gusül hükümleri kişisel ayrıntı içerebilir; Öğren/İlmihal kaynaklarını kullan ve gerekirse ehil bir din görevlisine danış.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun TravelScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Header("Seyahat Rehberi", onBack)
        Text("Şehir değişikliğinde vakitleri Namaz ayarlarından güncelle. Şehir değişmesi seni otomatik olarak seferî yapmaz.", modifier = Modifier.padding(top = 12.dp))
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
    val gregorian = LocalDate.now().toString()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Header("İslami Takvim", onBack)
        Text("Bugün", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        Text(hijri, style = MaterialTheme.typography.headlineMedium)
        Text("Miladî: $gregorian")
        Text("Önemli dönemler", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
        Text("• Muharrem ve Âşûrâ\n• Üç aylar: Recep, Şaban, Ramazan\n• Ramazan ve Kadir gecesi\n• Ramazan ve Kurban bayramları\n• Zilhicce'nin ilk günleri ve Arefe")
        Text("Hicrî hesap gözlem/resmî ilana göre bir gün farklılık gösterebilir. Kandil, Ramazan ve bayram başlangıcında yerel resmî duyuruyu esas al.", modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun ZakatScreen(onBack: () -> Unit) {
    var cash by rememberSaveable { mutableStateOf("") }
    var gold by rememberSaveable { mutableStateOf("") }
    var trade by rememberSaveable { mutableStateOf("") }
    var receivable by rememberSaveable { mutableStateOf("") }
    var debts by rememberSaveable { mutableStateOf("") }
    var nisab by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<Double?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Header("Zekât Hesap Yardımcısı", onBack)
        Text("Fetva değil, yaklaşık matematik yardımcısıdır. Mal türünün zekâta tabi olup olmadığını ve borç hükümlerini ayrıca kontrol et.")
        OutlinedTextField(cash, { cash = it }, label = { Text("Nakit / banka") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        OutlinedTextField(gold, { gold = it }, label = { Text("Altın/gümüş parasal değeri") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(trade, { trade = it }, label = { Text("Ticari mal değeri") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(receivable, { receivable = it }, label = { Text("Tahsil edilebilir alacak") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(debts, { debts = it }, label = { Text("Düşülebilecek borçlar") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(nisab, { nisab = it }, label = { Text("Güncel nisap parasal karşılığı") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            result = ZakatCalculator.calculate(
                cash.toDoubleOrNull() ?: 0.0,
                gold.toDoubleOrNull() ?: 0.0,
                trade.toDoubleOrNull() ?: 0.0,
                receivable.toDoubleOrNull() ?: 0.0,
                debts.toDoubleOrNull() ?: 0.0,
                nisab.toDoubleOrNull() ?: 0.0,
            )
        }, modifier = Modifier.padding(top = 8.dp)) { Text("Hesapla") }
        result?.let { value ->
            Text(if (value > 0) "Yaklaşık zekât: %.2f".format(value) else "Girilen değerlere göre yaklaşık zekât çıkmıyor; fıkhî şartları ayrıca kontrol et.", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        }
        Text("Kaynak çerçevesi: Diyanet İlmihal I — Zekât", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
    }
}
