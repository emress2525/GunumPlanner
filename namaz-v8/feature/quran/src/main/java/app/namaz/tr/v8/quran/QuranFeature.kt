package app.namaz.tr.v8.quran

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import kotlin.math.ceil

data class QuranVerse(
    val id: Int,
    val number: Int,
    val key: String,
    val arabic: String,
    val turkish: String,
    val audioGhamadi: String?,
    val audioMaher: String?,
)

data class QuranSurah(val number: Int, val name: String, val verses: List<QuranVerse>)
data class QuranSearchHit(val surah: Int, val surahName: String, val verse: QuranVerse)

object SurahNames {
    val all = listOf(
        "Fâtiha","Bakara","Âl-i İmrân","Nisâ","Mâide","En'âm","A'râf","Enfâl","Tevbe","Yûnus",
        "Hûd","Yûsuf","Ra'd","İbrâhîm","Hicr","Nahl","İsrâ","Kehf","Meryem","Tâhâ","Enbiyâ","Hac",
        "Mü'minûn","Nûr","Furkân","Şuarâ","Neml","Kasas","Ankebût","Rûm","Lokmân","Secde","Ahzâb",
        "Sebe'","Fâtır","Yâsîn","Sâffât","Sâd","Zümer","Mü'min","Fussilet","Şûrâ","Zuhruf","Duhân",
        "Câsiye","Ahkâf","Muhammed","Fetih","Hucurât","Kâf","Zâriyât","Tûr","Necm","Kamer","Rahmân",
        "Vâkıa","Hadîd","Mücâdele","Haşr","Mümtehine","Saf","Cuma","Münâfikûn","Tegâbün","Talâk","Tahrîm",
        "Mülk","Kalem","Hâkka","Meâric","Nûh","Cin","Müzzemmil","Müddessir","Kıyâmet","İnsan","Mürselât",
        "Nebe'","Nâziât","Abese","Tekvîr","İnfitâr","Mutaffifîn","İnşikâk","Bürûc","Târık","A'lâ","Gâşiye",
        "Fecr","Beled","Şems","Leyl","Duhâ","İnşirâh","Tîn","Alak","Kadir","Beyyine","Zilzâl","Âdiyât",
        "Kâria","Tekâsür","Asr","Hümeze","Fîl","Kureyş","Mâûn","Kevser","Kâfirûn","Nasr","Tebbet","İhlâs","Felak","Nâs"
    )
    fun name(number: Int): String = all.getOrElse(number - 1) { "Sure $number" }
}

class AssetQuranRepository(private val context: Context) {
    private val cache = LinkedHashMap<Int, QuranSurah>()

    suspend fun surah(number: Int): QuranSurah = withContext(Dispatchers.IO) {
        cache[number] ?: run {
            val arabic = context.assets.open("quran/$number.json").bufferedReader().use { it.readText() }
            val meal = context.assets.open("meal/$number.json").bufferedReader().use { it.readText() }
            QuranLicensedAssetParser.parseSurah(arabic, meal, number)
        }.also { cache[number] = it }
    }

    suspend fun tafsir(number: Int): String = withContext(Dispatchers.IO) {
        runCatching { context.assets.open("tafseer/elmalili/$number.md").bufferedReader().use { it.readText() } }
            .getOrElse { "Bu sure için çevrimdışı tefsir metni açılamadı." }
    }

    suspend fun search(query: String, limit: Int = 80): List<QuranSearchHit> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.length < 2) return@withContext emptyList()
        val out = mutableListOf<QuranSearchHit>()
        for (s in 1..114) {
            val surah = surah(s)
            if (SurahNames.name(s).lowercase().contains(q)) {
                surah.verses.firstOrNull()?.let { out += QuranSearchHit(s, SurahNames.name(s), it) }
            }
            for (v in surah.verses) {
                if (v.turkish.lowercase().contains(q) || v.arabic.contains(query.trim())) {
                    out += QuranSearchHit(s, SurahNames.name(s), v)
                    if (out.size >= limit) return@withContext out
                }
            }
        }
        out
    }
}

class QuranProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("quran_progress_v8", Context.MODE_PRIVATE)

    fun addHistory(key: String) {
        val current = prefs.getString("history", "").orEmpty().split('|').filter { it.isNotBlank() }.toMutableList()
        current.remove(key)
        current.add(0, key)
        prefs.edit().putString("history", current.take(10).joinToString("|")).apply()
    }
    fun history(): List<String> = prefs.getString("history", "").orEmpty().split('|').filter { it.isNotBlank() }

    fun toggleBookmark(key: String): Boolean = toggleSet("bookmarks", key)
    fun isBookmarked(key: String): Boolean = key in prefs.getStringSet("bookmarks", emptySet()).orEmpty()
    fun toggleMemorization(key: String): Boolean = toggleSet("memorization", key)
    fun isMemorizing(key: String): Boolean = key in prefs.getStringSet("memorization", emptySet()).orEmpty()

    fun setNote(key: String, note: String) {
        val clean = note.trim()
        if (clean.isBlank()) prefs.edit().remove("note_$key").apply() else prefs.edit().putString("note_$key", clean).apply()
    }
    fun note(key: String): String = prefs.getString("note_$key", "").orEmpty()

    fun setKhatmDays(days: Int) = prefs.edit().putInt("khatm_days", days.coerceIn(1, 365)).apply()
    fun khatmDays(): Int = prefs.getInt("khatm_days", 30).coerceIn(1, 365)
    fun markRead(globalId: Int) {
        val set = prefs.getStringSet("read", emptySet()).orEmpty().toMutableSet()
        set += globalId.toString()
        prefs.edit().putStringSet("read", set).apply()
    }
    fun readCount(): Int = prefs.getStringSet("read", emptySet()).orEmpty().size

    private fun toggleSet(name: String, key: String): Boolean {
        val set = prefs.getStringSet(name, emptySet()).orEmpty().toMutableSet()
        val added = if (key in set) { set.remove(key); false } else { set.add(key); true }
        prefs.edit().putStringSet(name, set).apply()
        return added
    }
}

object KhatmPlanner {
    fun dailyTarget(totalVerses: Int = 6236, completed: Int, daysRemaining: Int): Int {
        if (completed >= totalVerses) return 0
        return ceil((totalVerses - completed).toDouble() / daysRemaining.coerceAtLeast(1)).toInt()
    }
}

class QuranAudioController(private val context: Context) {
    private var player: MediaPlayer? = null
    private val audioDir = File(context.filesDir, "quran_audio").apply { mkdirs() }

    fun localFile(key: String): File = File(audioDir, key.replace(':', '_') + ".mp3")
    fun isDownloaded(key: String): Boolean = localFile(key).length() > 1024

    suspend fun download(url: String, key: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val target = localFile(key)
            val partial = File(target.absolutePath + ".part")
            URL(url).openStream().use { input -> partial.outputStream().use { output -> input.copyTo(output) } }
            require(partial.length() > 1024) { "Ses dosyası boş veya eksik" }
            if (target.exists()) target.delete()
            require(partial.renameTo(target)) { "Ses dosyası kaydedilemedi" }
            target
        }
    }

    fun play(url: String, key: String, speed: Float = 1f, repeatCount: Int = 1) {
        stop()
        var remaining = repeatCount.coerceIn(1, 10)
        val source = localFile(key).takeIf { it.exists() && it.length() > 1024 }?.absolutePath ?: url
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build(),
            )
            setDataSource(source)
            setOnPreparedListener {
                runCatching { it.playbackParams = it.playbackParams.setSpeed(speed.coerceIn(0.75f, 1.5f)) }
                it.start()
            }
            setOnCompletionListener {
                remaining--
                if (remaining > 0) {
                    it.seekTo(0)
                    it.start()
                } else {
                    stop()
                }
            }
            prepareAsync()
        }
    }

    fun stop() {
        player?.runCatching { stop() }
        player?.release()
        player = null
    }
    fun release() = stop()
}

@Composable
fun QuranScreen() {
    val context = LocalContext.current
    val repository = remember(context) { AssetQuranRepository(context.applicationContext) }
    val progress = remember(context) { QuranProgressStore(context.applicationContext) }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    if (selected > 0) {
        QuranReaderScreen(selected, repository, progress) { selected = 0 }
    } else {
        QuranLibraryScreen(repository, progress) { selected = it }
    }
}

@Composable
private fun QuranLibraryScreen(repository: AssetQuranRepository, progress: QuranProgressStore, onOpen: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<QuranSearchHit>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var planDays by rememberSaveable { mutableIntStateOf(progress.khatmDays()) }
    val history = progress.history()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Kur’an Pro", style = MaterialTheme.typography.headlineMedium)
        Text("114 sure çevrimdışı • Tanzil Uthmani metni • QuranEnc Türkçe meal • Elmalılı tefsir", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(query, { query = it }, label = { Text("Ayet, meal veya sure ara") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { searching = true; results = repository.search(query); searching = false } }) { Text(if (searching) "Aranıyor…" else "Ara") }
            OutlinedButton(onClick = { query = ""; results = emptyList() }) { Text("Temizle") }
        }

        val read = progress.readCount()
        Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Hatim planı", style = MaterialTheme.typography.titleMedium)
                Text("Okundu: $read / 6236 • $planDays günlük planda bugünkü öneri: ${KhatmPlanner.dailyTarget(completed = read, daysRemaining = planDays)} ayet")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(30, 60, 90).forEach { d ->
                        FilterChip(selected = planDays == d, onClick = { planDays = d; progress.setKhatmDays(d) }, label = { Text("$d gün") })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    OutlinedButton(onClick = { planDays = (planDays - 1).coerceAtLeast(1); progress.setKhatmDays(planDays) }) { Text("− gün") }
                    OutlinedButton(onClick = { planDays = (planDays + 1).coerceAtMost(365); progress.setKhatmDays(planDays) }) { Text("+ gün") }
                }
            }
        }

        if (history.isNotEmpty() && results.isEmpty()) {
            Text("Son okudukların: ${history.take(4).joinToString(" • ")}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp))
        }

        if (results.isNotEmpty()) {
            LazyColumn {
                item { Text("Arama sonuçları", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp)) }
                items(results, key = { it.verse.key }) { hit ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${hit.surah}. ${hit.surahName} • ${hit.verse.key}", style = MaterialTheme.typography.labelLarge)
                            Text(hit.verse.turkish, maxLines = 3)
                            OutlinedButton(onClick = { onOpen(hit.surah) }) { Text("Sureyi aç") }
                        }
                    }
                }
            }
        } else {
            LazyColumn {
                items((1..114).toList()) { n ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$n. ${SurahNames.name(n)}", style = MaterialTheme.typography.titleMedium)
                            OutlinedButton(onClick = { onOpen(n) }) { Text("Oku") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuranReaderScreen(number: Int, repository: AssetQuranRepository, progress: QuranProgressStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audio = remember(context) { QuranAudioController(context.applicationContext) }
    val uiPrefs = remember(context) { context.getSharedPreferences("ui_prefs_v8", Context.MODE_PRIVATE) }
    var arabicScale by rememberSaveable { mutableFloatStateOf(uiPrefs.getFloat("arabic_scale", 1f).coerceIn(0.8f, 1.6f)) }
    var surah by remember { mutableStateOf<QuranSurah?>(null) }
    var tafsir by remember { mutableStateOf<String?>(null) }
    var showTafsir by rememberSaveable { mutableStateOf(false) }
    var showMeal by rememberSaveable { mutableStateOf(true) }
    var memorization by rememberSaveable { mutableStateOf(false) }
    var reciterMaher by rememberSaveable { mutableStateOf(false) }
    var speed by rememberSaveable { mutableFloatStateOf(1f) }
    var repeatCount by rememberSaveable { mutableIntStateOf(1) }

    DisposableEffect(Unit) { onDispose { audio.release() } }
    LaunchedEffect(number) { surah = repository.surah(number); progress.addHistory("$number:1") }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("← Sureler") }
            FilterChip(showMeal, { showMeal = !showMeal }, label = { Text("Meal") })
            FilterChip(memorization, { memorization = !memorization }, label = { Text("Ezber") })
        }
        Text("$number. ${SurahNames.name(number)}", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp))
        Text("Arapça: Tanzil Project (CC BY 3.0) • Meal: Rowad Tercüme Merkezi / QuranEnc.com • Tefsir: Elmalılı", style = MaterialTheme.typography.bodySmall)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
            FilterChip(reciterMaher, { reciterMaher = !reciterMaher }, label = { Text(if (reciterMaher) "Mahir" else "Ghamadi") })
            OutlinedButton(onClick = { showTafsir = !showTafsir; if (showTafsir && tafsir == null) scope.launch { tafsir = repository.tafsir(number) } }) { Text(if (showTafsir) "Tefsiri kapat" else "Tefsir") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { speed = if (speed >= 1.5f) 0.75f else speed + 0.25f }) { Text("Hız ${"%.2f".format(speed)}×") }
            OutlinedButton(onClick = { repeatCount = when (repeatCount) { 1 -> 3; 3 -> 5; else -> 1 } }) { Text("Tekrar ×$repeatCount") }
            OutlinedButton(onClick = {
                arabicScale = if (arabicScale >= 1.6f) 0.8f else (arabicScale + 0.2f).coerceAtMost(1.6f)
                uiPrefs.edit().putFloat("arabic_scale", arabicScale).apply()
            }) { Text("Arapça ${"%.1f".format(arabicScale)}×") }
        }

        if (showTafsir) {
            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Text(tafsir ?: "Tefsir yükleniyor…", modifier = Modifier.padding(12.dp)) }
        }

        val data = surah
        if (data == null) {
            Text("Sure yükleniyor…", modifier = Modifier.padding(24.dp))
        } else {
            LazyColumn {
                items(data.verses, key = { it.key }) { verse ->
                    var revealed by rememberSaveable(verse.key) { mutableStateOf(false) }
                    var noteOpen by rememberSaveable("note_${verse.key}") { mutableStateOf(false) }
                    var note by rememberSaveable("note_text_${verse.key}") { mutableStateOf(progress.note(verse.key)) }
                    var downloaded by remember(verse.key) { mutableStateOf(audio.isDownloaded(verse.key)) }
                    var downloadMessage by remember(verse.key) { mutableStateOf("") }

                    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("${verse.number}", style = MaterialTheme.typography.labelMedium)
                            if (!memorization || revealed) {
                                Text(
                                    verse.arabic,
                                    fontSize = (30 * arabicScale).sp,
                                    lineHeight = (46 * arabicScale).sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                Text("Ayet gizli — önce ezberinden oku, sonra göstererek kontrol et.")
                            }
                            if (memorization) OutlinedButton(onClick = { revealed = !revealed }) { Text(if (revealed) "Gizle" else "Göster") }
                            if (showMeal) Text(verse.turkish, modifier = Modifier.padding(top = 8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                                OutlinedButton(onClick = {
                                    val url = if (reciterMaher) verse.audioMaher else verse.audioGhamadi
                                    if (url != null) audio.play(url, verse.key, speed, repeatCount)
                                }) { Text("▶ Dinle") }
                                OutlinedButton(onClick = {
                                    val url = if (reciterMaher) verse.audioMaher else verse.audioGhamadi
                                    if (url != null) scope.launch {
                                        downloadMessage = "İndiriliyor…"
                                        val result = audio.download(url, verse.key)
                                        downloaded = result.isSuccess
                                        downloadMessage = if (result.isSuccess) "Çevrimdışı hazır" else "İndirme başarısız"
                                    }
                                }, enabled = !downloaded) { Text(if (downloaded) "✓ Offline" else "İndir") }
                            }
                            if (downloadMessage.isNotBlank()) Text(downloadMessage, style = MaterialTheme.typography.bodySmall)

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                                OutlinedButton(onClick = { progress.toggleBookmark(verse.key) }) { Text(if (progress.isBookmarked(verse.key)) "★ Kayıtlı" else "☆ Kaydet") }
                                OutlinedButton(onClick = { progress.markRead(verse.id) }) { Text("Hatime ekle") }
                                OutlinedButton(onClick = { progress.toggleMemorization(verse.key) }) { Text(if (progress.isMemorizing(verse.key)) "✓ Ezberde" else "Ezbere ekle") }
                            }
                            OutlinedButton(onClick = { noteOpen = !noteOpen }, modifier = Modifier.padding(top = 6.dp)) { Text(if (noteOpen) "Notu kapat" else "Not") }
                            if (noteOpen) {
                                OutlinedTextField(note, { note = it }, label = { Text("Kişisel not") }, modifier = Modifier.fillMaxWidth())
                                Button(onClick = { progress.setNote(verse.key, note) }, modifier = Modifier.padding(top = 4.dp)) { Text("Notu kaydet") }
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(24.dp))
                    Text("Ezber yardımcısı, mahreç veya tajvid için kesin doğru/yanlış hükmü vermez. Şüpheli okuma yalnız kontrol edilmesi gereken yer olarak değerlendirilir.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
