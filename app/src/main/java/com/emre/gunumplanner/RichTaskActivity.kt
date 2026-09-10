package com.emre.gunumplanner

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

class RichTaskActivity : ComponentActivity() {
    lateinit var db: Db

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Db(this)
        FullRepository.ensureSchema(db)
        setContent { V2Theme { RichTaskScreen(this, db) } }
    }

    fun pickDate(date: LocalDate, cb: (LocalDate) -> Unit) =
        DatePickerDialog(this, { _, y, m, d -> cb(LocalDate.of(y, m + 1, d)) }, date.year, date.monthValue - 1, date.dayOfMonth).show()

    fun pickTime(time: LocalTime, cb: (LocalTime) -> Unit) =
        TimePickerDialog(this, { _, h, m -> cb(LocalTime.of(h, m)) }, time.hour, time.minute, true).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RichTaskScreen(activity: RichTaskActivity, db: Db) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var time by remember { mutableStateOf<LocalTime?>(null) }
    var duration by remember { mutableIntStateOf(FullRepository.Prefs.defaultDuration(activity)) }
    var priority by remember { mutableIntStateOf(2) }
    var recurrence by remember { mutableStateOf("") }
    var placeRevision by remember { mutableIntStateOf(0) }
    var selectedPlaceId by remember { mutableStateOf<Long?>(null) }
    val savedPlaces = remember(placeRevision) { FullRepository.savedPlaces(db) }
    val selectedPlace = savedPlaces.firstOrNull { it.id == selectedPlaceId }

    fun save(openExtras: Boolean) {
        if (title.isBlank()) return
        val due = time?.let { LocalDateTime.of(date, it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() } ?: 0L
        val topic = TopicEngine.findMatchingTopic(db, title.trim(), body.trim())
        val id = db.insertItem(Db.TYPE_TASK, title.trim(), body.trim(), date.toString(), due, topic, recurrence, duration, priority)
        if (due > 0) ReminderScheduler.schedule(activity, id, due)
        if (recurrence.isNotBlank()) FullRepository.ensureSeries(db, id)

        selectedPlace?.let { p ->
            val rule = ExtrasRepository.LocationRule(
                itemId = id,
                label = p.name,
                address = p.address,
                lat = p.lat,
                lng = p.lng,
                radius = p.radius,
                transition = "ENTER",
                enabled = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            ExtrasRepository.saveLocation(db, rule)
            LocationReminderManager.schedule(activity, rule)
        }
        GunumWidgetProvider.refreshAll(activity)

        if (openExtras) activity.startActivity(Intent(activity, TaskExtrasActivity::class.java).putExtra("item_id", id))
        activity.finish()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("Zengin görev", fontWeight = FontWeight.Bold); Text("Fotoğraf • dosya • konum • checklist", style = MaterialTheme.typography.bodySmall) } },
                navigationIcon = { IconButton(onClick = { activity.finish() }) { Icon(Icons.Rounded.ArrowBack, "Geri") } }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Görev") }, singleLine = true, shape = RoundedCornerShape(18.dp))
            OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth(), label = { Text("Açıklama") }, minLines = 3, shape = RoundedCornerShape(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { activity.pickDate(date) { date = it } }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.CalendarMonth, null); Spacer(Modifier.width(6.dp)); Text(date.format(DateTimeFormatter.ofPattern("d MMM", Locale("tr", "TR"))))
                }
                OutlinedButton(onClick = { activity.pickTime(time ?: LocalTime.of(9, 0)) { time = it } }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Schedule, null); Spacer(Modifier.width(6.dp)); Text(time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Saat")
                }
            }
            if (time != null) TextButton(onClick = { time = null }) { Text("Saati kaldır") }

            Text("Tahmini süre", fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(15, 30, 45, 60, 90, 120)) { value ->
                    FilterChip(duration == value, { duration = value }, { Text(if (value < 60) "$value dk" else if (value == 60) "1 saat" else "${value / 60}s ${value % 60}dk") })
                }
            }

            Text("Öncelik", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(priority == 0, { priority = 0 }, { Text("Acil") })
                FilterChip(priority == 1, { priority = 1 }, { Text("Önemli") })
                FilterChip(priority == 2, { priority = 2 }, { Text("Normal") })
            }

            Text("Tekrar", fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("" to "Yok", NaturalLanguageParser.DAILY to "Her gün", NaturalLanguageParser.WEEKDAYS to "Hafta içi", NaturalLanguageParser.WEEKLY to "Her hafta", NaturalLanguageParser.MONTHLY to "Her ay")) { (value, label) ->
                    FilterChip(recurrence == value, { recurrence = value }, { Text(label) })
                }
            }

            Text("Konum", fontWeight = FontWeight.SemiBold)
            if (savedPlaces.isEmpty()) {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AddLocationAlt, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) { Text("Kayıtlı konum yok", fontWeight = FontWeight.SemiBold); Text("Fabrika, Ev veya Ofis'i bir kez kaydet", style = MaterialTheme.typography.bodySmall) }
                        TextButton(onClick = { activity.startActivity(Intent(activity, SavedPlacesActivity::class.java)); placeRevision++ }) { Text("Ekle") }
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selectedPlaceId == null, { selectedPlaceId = null }, { Text("Konum yok") }) }
                    items(savedPlaces, key = { it.id }) { p ->
                        FilterChip(
                            selected = selectedPlaceId == p.id,
                            onClick = { selectedPlaceId = if (selectedPlaceId == p.id) null else p.id },
                            label = { Text(p.name) },
                            leadingIcon = { Icon(Icons.Rounded.Place, null, Modifier.size(17.dp)) }
                        )
                    }
                }
                TextButton(onClick = { activity.startActivity(Intent(activity, SavedPlacesActivity::class.java)); placeRevision++ }) {
                    Icon(Icons.Rounded.EditLocationAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("Kayıtlı konumları yönet")
                }
            }
            selectedPlace?.let { p ->
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Column(Modifier.padding(14.dp)) { Text("${p.name} atanacak", fontWeight = FontWeight.Bold); Text("${p.address} • ${if (p.radius >= 1000) "1 km" else "${p.radius.toInt()} m"} • gelince hatırlat", style = MaterialTheme.typography.bodySmall) }
                }
            }

            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.padding(16.dp)) {
                    Icon(Icons.Rounded.AttachFile, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("Kaydedince kamera, galeri, PDF/dosya, checklist ve ayrıntılı konum ekranına geçebilirsin.", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Button(onClick = { save(true) }, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) {
                Text("Kaydet ve ayrıntıları ekle", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = { save(false) }, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Text("Görevi kaydet") }
        }
    }
}
