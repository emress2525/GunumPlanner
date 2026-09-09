package com.emre.gunumplanner

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V2AddSheet(
    close: () -> Unit,
    quick: () -> Unit,
    task: () -> Unit,
    note: () -> Unit,
    voiceTask: () -> Unit,
    voiceNote: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = close) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Hızlı ekle", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Aklındakini en az dokunuşla planına aktar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            V2AddOption(Icons.Rounded.AutoAwesome, "Akıllı görev", "“Yarın 14:30 Ahmet'i ara 30 dk”", quick)
            V2AddOption(Icons.Rounded.Today, "Ayrıntılı görev", "Tarih, saat, süre, tekrar ve öncelik", task)
            V2AddOption(Icons.Rounded.Notes, "Not", "Fikir, bilgi veya kayıt", note)
            V2AddOption(Icons.Rounded.KeyboardVoice, "Sesle görev", "Konuşarak görevi oluştur", voiceTask)
            V2AddOption(Icons.Rounded.Mic, "Sesli not", "Konuş ve konu hafızasına kaydet", voiceNote)
        }
    }
}

@Composable
private fun V2AddOption(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, null, Modifier.padding(10.dp).size(22.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun V2QuickDialog(base: LocalDate, close: () -> Unit, save: (NaturalLanguageParser.ParsedTask) -> Unit) {
    var text by remember { mutableStateOf("") }
    val parsed = remember(text, base) { if (text.isBlank()) null else NaturalLanguageParser.parse(text, base) }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Akıllı görev") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Doğal dille yaz") },
                    placeholder = { Text("Yarın 14:30 Ahmet'i ara 30 dk") },
                    minLines = 3,
                    shape = RoundedCornerShape(18.dp)
                )
                if (parsed != null) {
                    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(parsed.title, fontWeight = FontWeight.SemiBold)
                            Text(parsed.dayKey + if (parsed.dueAt > 0) " • ${v2Time(parsed.dueAt)}" else "")
                            if (parsed.durationMinutes > 0) Text("${parsed.durationMinutes} dk", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { parsed?.let(save) }, enabled = parsed != null) { Text("Ekle") } },
        dismissButton = { TextButton(onClick = close) { Text("Vazgeç") } }
    )
}

@Composable
fun V2TaskDialog(
    item: Db.Item?,
    base: LocalDate,
    activity: PremiumV2Activity,
    close: () -> Unit,
    save: (String, String, LocalDate, LocalTime?, Int, String, Int) -> Unit
) {
    var title by remember(item?.id) { mutableStateOf(item?.title.orEmpty()) }
    var body by remember(item?.id) { mutableStateOf(item?.body.orEmpty()) }
    var day by remember(item?.id) {
        mutableStateOf(item?.dayKey?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: base)
    }
    var time by remember(item?.id) {
        mutableStateOf(item?.dueAt?.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime() })
    }
    var duration by remember(item?.id) { mutableIntStateOf(item?.durationMinutes?.takeIf { it > 0 } ?: 30) }
    var recurrence by remember(item?.id) { mutableStateOf(item?.recurrence.orEmpty()) }
    var priority by remember(item?.id) { mutableIntStateOf(item?.priority ?: 2) }

    AlertDialog(
        onDismissRequest = close,
        title = { Text(if (item == null) "Yeni görev" else "Görevi düzenle") },
        text = {
            LazyColumn(Modifier.heightIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Görev") }, shape = RoundedCornerShape(18.dp), singleLine = true)
                }
                item {
                    OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth(), label = { Text("Açıklama") }, minLines = 2, maxLines = 4, shape = RoundedCornerShape(18.dp))
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ activity.pickDate(day) { day = it } }, Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                            Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(day.format(DateTimeFormatter.ofPattern("d MMM", Locale("tr", "TR"))))
                        }
                        OutlinedButton({ activity.pickTime(time ?: LocalTime.of(9, 0)) { time = it } }, Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                            Icon(Icons.Rounded.Schedule, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Saat")
                        }
                    }
                }
                if (time != null) {
                    item { TextButton(onClick = { time = null }) { Icon(Icons.Rounded.Close, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Saati kaldır") } }
                }
                item {
                    Text("Tahmini süre", fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(15, 30, 45, 60, 90, 120)) { value ->
                            FilterChip(selected = duration == value, onClick = { duration = value }, label = { Text(if (value < 60) "$value dk" else if (value == 60) "1 saat" else "${value / 60}s ${value % 60}dk") })
                        }
                    }
                }
                item {
                    Text("Öncelik", fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(0 to "Acil", 1 to "Önemli", 2 to "Normal")) { (value, label) ->
                            FilterChip(selected = priority == value, onClick = { priority = value }, label = { Text(label) })
                        }
                    }
                }
                item {
                    Text("Tekrar", fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(
                            listOf(
                                "" to "Yok",
                                NaturalLanguageParser.DAILY to "Her gün",
                                NaturalLanguageParser.WEEKDAYS to "Hafta içi",
                                NaturalLanguageParser.WEEKLY to "Her hafta",
                                NaturalLanguageParser.MONTHLY to "Her ay"
                            )
                        ) { (value, label) ->
                            FilterChip(selected = recurrence == value, onClick = { recurrence = value }, label = { Text(label) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { save(title.trim(), body.trim(), day, time, duration, recurrence, priority) }, enabled = title.isNotBlank()) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = close) { Text("Vazgeç") } }
    )
}

@Composable
fun V2NoteDialog(item: Db.Item?, close: () -> Unit, save: (String, String) -> Unit) {
    var title by remember(item?.id) { mutableStateOf(item?.title.orEmpty()) }
    var body by remember(item?.id) { mutableStateOf(item?.body.orEmpty()) }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(if (item == null) "Yeni not" else "Notu düzenle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Başlık") }, shape = RoundedCornerShape(18.dp), singleLine = true)
                OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth(), label = { Text("Not") }, minLines = 7, maxLines = 12, shape = RoundedCornerShape(18.dp))
            }
        },
        confirmButton = { Button(onClick = { save(title.trim(), body.trim()) }, enabled = title.isNotBlank() || body.isNotBlank()) { Text("Kaydet") } },
        dismissButton = { TextButton(onClick = close) { Text("Vazgeç") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V2ItemSheet(
    item: Db.Item,
    close: () -> Unit,
    complete: () -> Unit,
    focus: () -> Unit,
    tomorrow: () -> Unit,
    addToCalendar: () -> Unit,
    edit: () -> Unit,
    delete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = close) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 30.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(item.dayKey + if (item.dueAt > 0) " • ${v2Time(item.dueAt)}" else "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (item.type == Db.TYPE_TASK) {
                    Surface(shape = CircleShape, color = v2Priority(item.priority).copy(alpha = .12f)) {
                        Box(Modifier.padding(10.dp).size(10.dp).background(v2Priority(item.priority), CircleShape))
                    }
                }
            }
            if (item.body.isNotBlank()) { Spacer(Modifier.height(16.dp)); Text(item.body) }
            if (item.type == Db.TYPE_TASK) {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.durationMinutes > 0) V2MetaChip(Icons.Rounded.Timer, "${item.durationMinutes} dk")
                    if (item.recurrence.isNotBlank()) V2MetaChip(Icons.Rounded.Repeat, NaturalLanguageParser.recurrenceLabel(item.recurrence))
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = focus, Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Odak modunu aç")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = complete, Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Icon(if (item.status == Db.STATUS_DONE) Icons.Rounded.RestartAlt else Icons.Rounded.Check, null)
                    Spacer(Modifier.width(8.dp)); Text(if (item.status == Db.STATUS_DONE) "Tekrar aç" else "Tamamla")
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = tomorrow, Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Rounded.Update, null, Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("Yarına") }
                    OutlinedButton(onClick = addToCalendar, enabled = item.dueAt > 0, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Rounded.Event, null, Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("Takvime") }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = edit, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Rounded.Edit, null); Spacer(Modifier.width(8.dp)); Text("Düzenle")
            }
            TextButton(onClick = delete, Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Rounded.Delete, null); Spacer(Modifier.width(8.dp)); Text("Sil")
            }
        }
    }
}

@Composable
private fun V2MetaChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(5.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun V2FocusDialog(item: Db.Item, close: () -> Unit, complete: () -> Unit, postpone: () -> Unit) {
    var running by remember { mutableStateOf(false) }
    var seconds by remember { mutableLongStateOf(0L) }
    val estimateSeconds = ((if (item.durationMinutes > 0) item.durationMinutes else 30) * 60L).coerceAtLeast(1L)
    val rawProgress = (seconds.toFloat() / estimateSeconds.toFloat()).coerceIn(0f, 1f)
    val progress by animateFloatAsState(rawProgress, label = "focus_progress")

    LaunchedEffect(running) {
        while (running) {
            delay(1000)
            seconds++
        }
    }

    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .8f), MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background)
                        )
                    )
            ) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = close) { Icon(Icons.Rounded.Close, "Kapat") }
                        Spacer(Modifier.weight(1f))
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = .8f)) {
                            Text("ODAK", Modifier.padding(horizontal = 13.dp, vertical = 7.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.weight(.5f))
                    Text(item.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (item.body.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(item.body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(36.dp))
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(210.dp), strokeWidth = 10.dp, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedContent(targetState = seconds, label = "timer") { value ->
                                Text(V2FormatElapsed(value), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                            }
                            Text("hedef ${item.durationMinutes.takeIf { it > 0 } ?: 30} dk", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(34.dp))
                    FilledTonalButton(onClick = { running = !running }, modifier = Modifier.height(58.dp).fillMaxWidth(.78f), shape = RoundedCornerShape(20.dp)) {
                        Icon(if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (running) "Duraklat" else if (seconds == 0L) "Başlat" else "Devam et", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = { running = false; seconds = 0L }) { Icon(Icons.Rounded.RestartAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("Sıfırla") }
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = postpone, Modifier.weight(1f), shape = RoundedCornerShape(18.dp)) { Text("Yarına ertele") }
                        Button(onClick = complete, Modifier.weight(1f), shape = RoundedCornerShape(18.dp)) { Icon(Icons.Rounded.Check, null); Spacer(Modifier.width(5.dp)); Text("Tamamla") }
                    }
                }
            }
        }
    }
}

private fun V2FormatElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
