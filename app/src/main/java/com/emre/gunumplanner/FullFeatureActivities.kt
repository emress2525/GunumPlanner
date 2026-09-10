@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.emre.gunumplanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class FullSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Db(this)
        FullRepository.ensureSchema(db)
        setContent { V2Theme { SettingsScreen(this, db) } }
    }
}

@Composable
private fun SettingsScreen(activity: Activity, db: Db) {
    val context = LocalContext.current
    var theme by remember { mutableStateOf(FullRepository.Prefs.theme(context)) }
    var duration by remember { mutableIntStateOf(FullRepository.Prefs.defaultDuration(context)) }
    var monday by remember { mutableStateOf(FullRepository.Prefs.weekStartsMonday(context)) }
    var approval by remember { mutableStateOf(FullRepository.Prefs.autoPlanApproval(context)) }
    val trashCount = remember { FullRepository.trash(db).size }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar ve araçlar", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton({ activity.finish() }) { Icon(Icons.Rounded.ArrowBack, "Geri") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FullSectionCard("Görünüm", Icons.Rounded.Palette) {
                Text("Tema", fontWeight = FontWeight.SemiBold)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("system" to "Sistem", "light" to "Açık", "dark" to "Koyu").forEachIndexed { index, pair ->
                        SegmentedButton(
                            selected = theme == pair.first,
                            onClick = {
                                theme = pair.first
                                FullRepository.Prefs.setTheme(context, pair.first)
                                activity.recreate()
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, 3)
                        ) { Text(pair.second) }
                    }
                }
            }

            FullSectionCard("Planlama", Icons.Rounded.Tune) {
                Text("Varsayılan görev süresi", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 45, 60, 90, 120).forEach { minutes ->
                        FilterChip(
                            selected = duration == minutes,
                            onClick = { duration = minutes; FullRepository.Prefs.setDefaultDuration(context, minutes) },
                            label = { Text("$minutes dk") }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hafta pazartesi başlasın", fontWeight = FontWeight.SemiBold)
                        Text("Haftalık görünüm için", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(monday, { monday = it; FullRepository.Prefs.setWeekStartsMonday(context, it) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Akıllı plan önce onay istesin", fontWeight = FontWeight.SemiBold)
                        Text("Görevleri senden habersiz oynatmaz", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(approval, { approval = it; FullRepository.Prefs.setAutoPlanApproval(context, it) })
                }
            }

            FullSectionCard("Veri ve güvenlik", Icons.Rounded.Backup) {
                FullAction("Yedekle / geri yükle", "Görev, not, fotoğraf, dosya ve konumlar", Icons.Rounded.Backup) {
                    context.startActivity(Intent(context, BackupActivity::class.java))
                }
                FullAction("Çöp kutusu", if (trashCount == 0) "Silinen kayıt yok" else "$trashCount silinen kayıt", Icons.Rounded.DeleteOutline) {
                    context.startActivity(Intent(context, TrashActivity::class.java))
                }
            }

            FullSectionCard("Güçlü araçlar", Icons.Rounded.AutoAwesome) {
                FullAction("Gelişmiş arama", "Tür, durum ve öncelik filtreleri", Icons.Rounded.ManageSearch) {
                    context.startActivity(Intent(context, AdvancedSearchActivity::class.java))
                }
                FullAction("Kayıtlı konumlar", "Ev, iş, fabrika, market…", Icons.Rounded.Place) {
                    context.startActivity(Intent(context, SavedPlacesActivity::class.java))
                }
                FullAction("Tanıtımı yeniden göster", "İlk kullanım ekranlarını aç", Icons.Rounded.Slideshow) {
                    context.startActivity(Intent(context, FullOnboardingActivity::class.java).putExtra("force", true))
                }
            }

            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Ana ekran widget'ı", fontWeight = FontWeight.Bold)
                    Text(
                        "Ana ekranda boş alana basılı tut → Widget'lar → Günüm. Sıradaki görevi, açık görev sayısını ve Tamamla düğmesini gösterir.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun FullSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun FullAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, click: () -> Unit) {
    Surface(onClick = click, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null)
        }
    }
}

class ChecklistActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemId = intent.getLongExtra("item_id", 0L)
        val db = Db(this)
        FullRepository.ensureSchema(db)
        if (itemId <= 0 || db.getItem(itemId) == null) { finish(); return }
        setContent { V2Theme { ChecklistScreen(this, db, itemId) } }
    }
}

@Composable
private fun ChecklistScreen(activity: Activity, db: Db, itemId: Long) {
    val item = db.getItem(itemId) ?: return
    var revision by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf("") }
    val data = remember(revision) { FullRepository.subtasks(db, itemId) }
    val done = data.count { it.done }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("Checklist", fontWeight = FontWeight.Bold); Text(item.title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                navigationIcon = { IconButton({ activity.finish() }) { Icon(Icons.Rounded.ArrowBack, "Geri") } }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("$done / ${data.size} tamamlandı", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { if (data.isEmpty()) 0f else done.toFloat() / data.size }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(text, { text = it }, Modifier.weight(1f), label = { Text("Alt görev") }, singleLine = true)
                    FilledIconButton(onClick = {
                        if (text.isNotBlank()) { FullRepository.addSubtask(db, itemId, text); text = ""; revision++ }
                    }) { Icon(Icons.Rounded.Add, "Ekle") }
                }
            }
            if (data.isEmpty()) item { Text("Henüz alt görev yok. Büyük işi küçük adımlara böl.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp)) }
            items(data, key = { it.id }) { subtask ->
                Card(shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(subtask.done, { FullRepository.toggleSubtask(db, subtask.id, it); revision++ })
                        Text(subtask.title, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        IconButton({ FullRepository.deleteSubtask(db, subtask.id); revision++ }) { Icon(Icons.Rounded.DeleteOutline, "Sil", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

class TrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Db(this)
        FullRepository.ensureSchema(db)
        setContent { V2Theme { TrashScreen(this, db) } }
    }
}

@Composable
private fun TrashScreen(activity: Activity, db: Db) {
    var revision by remember { mutableIntStateOf(0) }
    val data = remember(revision) { FullRepository.trash(db) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Çöp kutusu", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton({ activity.finish() }) { Icon(Icons.Rounded.ArrowBack, "Geri") } }) }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (data.isEmpty()) item { Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Text("Çöp kutusu boş. Yanlışlıkla sildiğin görev ve notları buradan geri alabilirsin.", Modifier.padding(18.dp)) } }
            items(data, key = { it.trashId }) { trash ->
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(trash.title, fontWeight = FontWeight.Bold)
                        Text(trash.dayKey, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button({ FullRepository.restoreTrash(db, trash); GunumWidgetProvider.refreshAll(activity); revision++ }) {
                                Icon(Icons.Rounded.Restore, null); Spacer(Modifier.width(6.dp)); Text("Geri al")
                            }
                            TextButton(onClick = { FullRepository.deleteTrashForever(db, trash); revision++ }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Kalıcı sil") }
                        }
                    }
                }
            }
        }
    }
}

class AdvancedSearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Db(this)
        FullRepository.ensureSchema(db)
        setContent { V2Theme { AdvancedSearchScreen(this, db) } }
    }
}

@Composable
private fun AdvancedSearchScreen(activity: Activity, db: Db) {
    var query by remember { mutableStateOf("") }
    var type by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var priority by remember { mutableStateOf<Int?>(null) }
    val rows = remember(query, type, status, priority) { FullRepository.allItemsFiltered(db, query, type, status, priority, null, null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Gelişmiş arama", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton({ activity.finish() }) { Icon(Icons.Rounded.ArrowBack, "Geri") } }) }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Rounded.Search, null) }, label = { Text("Görev veya not ara") }, singleLine = true) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Filtreler", fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(type == null, { type = null }, { Text("Tümü") })
                        FilterChip(type == Db.TYPE_TASK, { type = Db.TYPE_TASK }, { Text("Görev") })
                        FilterChip(type == Db.TYPE_NOTE, { type = Db.TYPE_NOTE }, { Text("Not") })
                        FilterChip(status == Db.STATUS_OPEN, { status = if (status == Db.STATUS_OPEN) null else Db.STATUS_OPEN }, { Text("Açık") })
                        FilterChip(status == Db.STATUS_DONE, { status = if (status == Db.STATUS_DONE) null else Db.STATUS_DONE }, { Text("Tamam") })
                        FilterChip(priority == 0, { priority = if (priority == 0) null else 0 }, { Text("Acil") })
                        FilterChip(priority == 1, { priority = if (priority == 1) null else 1 }, { Text("Önemli") })
                    }
                }
            }
            item { Text("${rows.size} sonuç", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
            items(rows, key = { it.id }) { item ->
                Surface(
                    onClick = { activity.startActivity(Intent(activity, TaskExtrasActivity::class.java).putExtra("item_id", item.id)) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (item.type == Db.TYPE_NOTE) Icons.Rounded.Notes else if (item.status == Db.STATUS_DONE) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.SemiBold)
                            Text(item.dayKey + if (item.body.isBlank()) "" else " • ${item.body.take(70)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

class SavedPlacesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Db(this)
        FullRepository.ensureSchema(db)
        setContent { V2Theme { SavedPlacesScreen(this, db) } }
    }
}

private data class PendingPlace(val lat: Double, val lng: Double, val address: String)

@Composable
private fun SavedPlacesScreen(activity: Activity, db: Db) {
    var revision by remember { mutableIntStateOf(0) }
    var pending by remember { mutableStateOf<PendingPlace?>(null) }
    var name by remember { mutableStateOf("") }
    var radius by remember { mutableFloatStateOf(200f) }
    val places = remember(revision) { FullRepository.savedPlaces(db) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val lat = data?.getDoubleExtra("lat", Double.NaN) ?: Double.NaN
            val lng = data?.getDoubleExtra("lng", Double.NaN) ?: Double.NaN
            if (!lat.isNaN() && !lng.isNaN()) {
                pending = PendingPlace(lat, lng, data?.getStringExtra("address").orEmpty())
                name = data?.getStringExtra("label").orEmpty()
                radius = data?.getFloatExtra("radius", 200f) ?: 200f
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kayıtlı konumlar", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton({ activity.finish() }) { Icon(Icons.Rounded.ArrowBack, "Geri") } },
                actions = { IconButton({ picker.launch(Intent(activity, MapPickerActivity::class.java)) }) { Icon(Icons.Rounded.AddLocationAlt, "Konum ekle") } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            pending?.let { location ->
                item {
                    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Konumu kaydet", fontWeight = FontWeight.Bold)
                            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Adı") }, placeholder = { Text("Ev, İş, Fabrika") })
                            Text(location.address, style = MaterialTheme.typography.bodySmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(100f, 200f, 500f, 1000f).forEach { meters ->
                                    FilterChip(radius == meters, { radius = meters }, { Text(if (meters >= 1000) "1 km" else "${meters.toInt()} m") })
                                }
                            }
                            Button(
                                onClick = {
                                    FullRepository.savePlace(db, name, location.address, location.lat, location.lng, radius)
                                    pending = null; name = ""; revision++
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = name.isNotBlank()
                            ) { Icon(Icons.Rounded.Save, null); Spacer(Modifier.width(6.dp)); Text("Kaydet") }
                        }
                    }
                }
            }
            if (places.isEmpty()) item { Text("Fabrika, ev, ofis veya sık kullandığın yerleri bir kez kaydet; yeni görevlerde tek dokunuşla ata.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(places, key = { it.id }) { place ->
                Card(shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Place, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(place.name, fontWeight = FontWeight.Bold)
                            Text(place.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(if (place.radius >= 1000) "1 km" else "${place.radius.toInt()} m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton({ FullRepository.deletePlace(db, place.id); revision++ }) { Icon(Icons.Rounded.DeleteOutline, "Sil", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

class BackupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { V2Theme { BackupScreen(this) } }
    }
}

@Composable
private fun BackupScreen(activity: BackupActivity) {
    var status by remember { mutableStateOf("") }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val ok = GunumBackup.restore(activity, uri)
            status = if (ok) "Yedek geri yüklendi. Uygulama yeniden açılıyor…" else "Yedek geri yüklenemedi"
            if (ok) {
                activity.window.decorView.postDelayed({
                    activity.startActivity(Intent(activity, PremiumV2Activity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                    activity.finish()
                }, 700)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Yedekleme", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton({ activity.finish() }) { Icon(Icons.Rounded.ArrowBack, "Geri") } }) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Verilerin sende kalsın", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Görevler, notlar, konular, fotoğraflar, PDF'ler, konumlar, checklist ve ayarlar tek ZIP içinde yedeklenir.")
                }
            }
            Button(onClick = {
                val file = GunumBackup.create(activity)
                if (file != null) {
                    val uri = FileProvider.getUriForFile(activity, activity.packageName + ".files", file)
                    val send = Intent(Intent.ACTION_SEND).setType("application/zip").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    activity.startActivity(Intent.createChooser(send, "Günüm yedeğini kaydet"))
                    status = "Yedek oluşturuldu"
                } else status = "Yedek oluşturulamadı"
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Upload, null); Spacer(Modifier.width(8.dp)); Text("Yedek oluştur / paylaş")
            }
            OutlinedButton(onClick = { importer.launch(arrayOf("application/zip", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(8.dp)); Text("Yedekten geri yükle")
            }
            if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

object GunumBackup {
    fun create(context: Context): File? = runCatching {
        val db = Db(context)
        FullRepository.ensureSchema(db)
        db.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
        db.close()
        val out = File(context.cacheDir, "Gunum_Yedek_${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(out)).use { zip ->
            val database = context.getDatabasePath("gunum.db")
            if (database.exists()) addFile(zip, database, "database/gunum.db")
            val attachmentDir = File(context.filesDir, "attachments")
            if (attachmentDir.exists()) attachmentDir.walkTopDown().filter { it.isFile }.forEach { addFile(zip, it, "attachments/${it.name}") }
            val prefs = File(context.applicationInfo.dataDir, "shared_prefs/gunum_full_prefs.xml")
            if (prefs.exists()) addFile(zip, prefs, "prefs/gunum_full_prefs.xml")
        }
        out
    }.getOrNull()

    private fun addFile(zip: ZipOutputStream, file: File, name: String) {
        zip.putNextEntry(ZipEntry(name))
        FileInputStream(file).use { it.copyTo(zip) }
        zip.closeEntry()
    }

    fun restore(context: Context, uri: Uri): Boolean = runCatching {
        val temp = File(context.cacheDir, "restore_${System.currentTimeMillis()}").apply { mkdirs() }
        var hasDb = false
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.replace('\\', '/')
                    if (!name.contains("..") && !entry.isDirectory) {
                        val target = when {
                            name == "database/gunum.db" -> { hasDb = true; File(temp, "gunum.db") }
                            name.startsWith("attachments/") -> File(temp, "attachments/${File(name).name}")
                            name == "prefs/gunum_full_prefs.xml" -> File(temp, "prefs.xml")
                            else -> null
                        }
                        target?.let { file ->
                            file.parentFile?.mkdirs()
                            FileOutputStream(file).use { zip.copyTo(it) }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: return@runCatching false
        if (!hasDb) return@runCatching false

        val live = context.getDatabasePath("gunum.db")
        live.parentFile?.mkdirs()
        File(temp, "gunum.db").copyTo(live, overwrite = true)
        File(live.absolutePath + "-wal").delete()
        File(live.absolutePath + "-shm").delete()

        val srcAttachments = File(temp, "attachments")
        val dstAttachments = File(context.filesDir, "attachments").apply { mkdirs() }
        if (srcAttachments.exists()) srcAttachments.listFiles()?.forEach { it.copyTo(File(dstAttachments, it.name), overwrite = true) }

        val pref = File(temp, "prefs.xml")
        if (pref.exists()) {
            val dst = File(context.applicationInfo.dataDir, "shared_prefs/gunum_full_prefs.xml")
            dst.parentFile?.mkdirs()
            pref.copyTo(dst, overwrite = true)
        }
        temp.deleteRecursively()
        true
    }.getOrDefault(false)
}

class FullOnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { V2Theme { OnboardingScreen(this) } }
    }
}

@Composable
private fun OnboardingScreen(activity: Activity) {
    var page by remember { mutableIntStateOf(0) }
    val pages = listOf(
        Triple(Icons.Rounded.AutoAwesome, "Günün tek yerde", "Görevlerini, notlarını, takvimini ve konum hatırlatmalarını aynı akışta yönet."),
        Triple(Icons.Rounded.Schedule, "Zamanını gerçekten gör", "Gün–hafta–ay görünümü, sürükle-bırak planlama, çakışma ve kapasite uyarıları."),
        Triple(Icons.Rounded.PhotoLibrary, "Her şeyi göreve bağla", "Fotoğraf, PDF, dosya, checklist ve konum ekle. Eve, işe veya fabrikaya gelince hatırlat."),
        Triple(Icons.Rounded.Psychology, "Daha az düşün, daha çok yap", "Akıllı plan, sesle ekleme, odak modu ve konu hafızası günlük yükünü azaltır.")
    )
    val current = pages[page]

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(28.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(current.first, null, Modifier.padding(28.dp).size(64.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(28.dp))
            Text(current.second, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(current.third, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(36.dp))
            LinearProgressIndicator(progress = { (page + 1) / pages.size.toFloat() }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (page < pages.lastIndex) page++
                    else { FullRepository.Prefs.setOnboardingSeen(activity, true); activity.finish() }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (page < pages.lastIndex) "Devam" else "Günümü aç") }
            if (page < pages.lastIndex) {
                TextButton(onClick = { FullRepository.Prefs.setOnboardingSeen(activity, true); activity.finish() }) { Text("Geç") }
            }
        }
    }
}
