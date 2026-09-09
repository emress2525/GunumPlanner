package com.emre.gunumplanner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object V2DateText {
    private val tr = Locale("tr", "TR")
    fun short(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("d MMMM", tr))
    fun full(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("d MMMM EEEE", tr))
    fun month(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("MMMM yyyy", tr)).replaceFirstChar { it.uppercaseChar() }
    fun dayName(d: LocalDate): String = d.dayOfWeek.getDisplayName(TextStyle.SHORT, tr).uppercase(tr)
}

@Composable
fun V2TopTitle(title: String, screen: V2Screen, date: LocalDate) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (screen == V2Screen.PLANNER || screen == V2Screen.HISTORY) {
            Text(
                V2DateText.full(date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable fun V2BackButton(onBack: () -> Unit) { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Geri") } }
@Composable fun V2CalendarButton(onClick: () -> Unit) { IconButton(onClick = onClick) { Icon(Icons.Rounded.CalendarMonth, "Tarih seç") } }
@Composable fun V2TodayButton(onClick: () -> Unit) { TextButton(onClick = onClick) { Text("Bugün") } }

@Composable
fun V2PlannerSurface(
    selectedDate: LocalDate,
    mode: V2CalendarMode,
    stats: Db.DayStats,
    dayItems: List<Db.Item>,
    calendarEvents: List<DeviceCalendarEvent>,
    calendarConnected: Boolean,
    db: Db,
    revision: Int,
    onMode: (V2CalendarMode) -> Unit,
    onDate: (LocalDate) -> Unit,
    onOpen: (Db.Item) -> Unit,
    onComplete: (Db.Item) -> Unit,
    onPostpone: (Db.Item) -> Unit,
    onMoveMinutes: (Db.Item, Int) -> Unit,
    onFocus: (Db.Item) -> Unit,
    onConnectCalendar: () -> Unit,
    onVoice: () -> Unit,
    onSmartPlan: () -> Unit
) {
    when (mode) {
        V2CalendarMode.DAY -> V2DayPlanner(
            selectedDate, stats, dayItems, calendarEvents, calendarConnected,
            onMode, onDate, onOpen, onComplete, onPostpone, onMoveMinutes,
            onFocus, onConnectCalendar, onVoice, onSmartPlan
        )
        V2CalendarMode.WEEK -> V2WeekPlanner(selectedDate, db, revision, onMode, onDate)
        V2CalendarMode.MONTH -> V2MonthPlanner(selectedDate, db, revision, onMode, onDate)
    }
}

@Composable
private fun V2ModeSelector(mode: V2CalendarMode, onMode: (V2CalendarMode) -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(4.dp)) {
            listOf(
                V2CalendarMode.DAY to "Gün",
                V2CalendarMode.WEEK to "Hafta",
                V2CalendarMode.MONTH to "Ay"
            ).forEach { (value, label) ->
                val active = mode == value
                Surface(
                    onClick = { onMode(value) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                    tonalElevation = if (active) 1.dp else 0.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        label,
                        Modifier.padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun V2DayPlanner(
    date: LocalDate,
    stats: Db.DayStats,
    data: List<Db.Item>,
    events: List<DeviceCalendarEvent>,
    calendarConnected: Boolean,
    onMode: (V2CalendarMode) -> Unit,
    onDate: (LocalDate) -> Unit,
    onOpen: (Db.Item) -> Unit,
    onComplete: (Db.Item) -> Unit,
    onPostpone: (Db.Item) -> Unit,
    onMove: (Db.Item, Int) -> Unit,
    onFocus: (Db.Item) -> Unit,
    onConnectCalendar: () -> Unit,
    onVoice: () -> Unit,
    onSmartPlan: () -> Unit
) {
    val tasks = data.filter { it.type == Db.TYPE_TASK }
    val notes = data.filter { it.type == Db.TYPE_NOTE }
    val openTasks = tasks.filter { it.status == Db.STATUS_OPEN }
    val scheduled = tasks.filter { it.dueAt > 0 }.sortedBy { it.dueAt }
    val unscheduled = tasks.filter { it.dueAt <= 0 }
    val next = openTasks.filter { it.dueAt > 0 && it.dueAt >= System.currentTimeMillis() }.minByOrNull { it.dueAt }
        ?: openTasks.filter { it.dueAt > 0 }.minByOrNull { it.dueAt }
        ?: openTasks.firstOrNull()
    val totalMinutes = tasks.filter { it.status == Db.STATUS_OPEN }.sumOf { if (it.durationMinutes > 0) it.durationMinutes else 30 }
    val conflictIds = remember(scheduled) { V2ConflictIds(scheduled) }
    val total = stats.open + stats.done
    val completion = if (total == 0) 0f else stats.done.toFloat() / total.toFloat()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { V2DateStrip(date, onDate) }
        item { V2ModeSelector(V2CalendarMode.DAY, onMode) }
        item { V2DayHero(stats, completion, totalMinutes, conflictIds.size) }
        if (next != null) item { V2NowCard(next, { onFocus(next) }, { onOpen(next) }) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    AssistChip(
                        onClick = onVoice,
                        label = { Text("Sesle ekle") },
                        leadingIcon = { Icon(Icons.Rounded.KeyboardVoice, null, Modifier.size(18.dp)) }
                    )
                }
                item {
                    AssistChip(
                        onClick = onSmartPlan,
                        label = { Text("Akıllı plan") },
                        leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(18.dp)) }
                    )
                }
                item {
                    AssistChip(
                        onClick = onConnectCalendar,
                        label = { Text(if (calendarConnected) "Takvim bağlı" else "Takvimi bağla") },
                        leadingIcon = { Icon(if (calendarConnected) Icons.Rounded.EventAvailable else Icons.Rounded.Event, null, Modifier.size(18.dp)) }
                    )
                }
            }
        }

        if (!calendarConnected) {
            item { V2CalendarConnectCard(onConnectCalendar) }
        } else if (events.isNotEmpty()) {
            item { V2Section("Takvim", "${events.size} etkinlik") }
            items(events, key = { "cal-${it.id}-${it.start}" }) { V2CalendarEventCard(it) }
        }

        item { V2Section("Zaman çizelgesi", if (scheduled.isEmpty()) "Saatli görev yok" else "${scheduled.size} görev") }
        if (scheduled.isEmpty()) {
            item { V2EmptyCard("Bugün henüz saatli görev yok", "Bir göreve saat ver veya Akıllı Plan ile boşluklara yerleştir.", Icons.Rounded.Schedule) }
        } else {
            items(scheduled, key = { "task-${it.id}" }) { item ->
                V2SwipeTaskRow(
                    item = item,
                    conflict = conflictIds.contains(item.id),
                    onOpen = onOpen,
                    onComplete = onComplete,
                    onPostpone = onPostpone,
                    onMoveMinutes = onMove
                )
            }
        }

        if (unscheduled.isNotEmpty()) {
            item { V2Section("Saati belli olmayanlar", "${unscheduled.size} görev") }
            items(unscheduled, key = { "free-${it.id}" }) { item ->
                V2SwipeTaskRow(
                    item = item,
                    conflict = false,
                    onOpen = onOpen,
                    onComplete = onComplete,
                    onPostpone = onPostpone,
                    onMoveMinutes = onMove
                )
            }
        }

        if (notes.isNotEmpty()) {
            item { V2Section("Notlar", "${notes.size} kayıt") }
            items(notes, key = { "note-${it.id}" }) { V2NoteCard(it, onOpen) }
        }
    }
}

@Composable
private fun V2DateStrip(selected: LocalDate, onDate: (LocalDate) -> Unit) {
    val start = selected.minusDays(3)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items((0..6).map { start.plusDays(it.toLong()) }) { day ->
            val active = day == selected
            val today = day == LocalDate.now()
            Surface(
                onClick = { onDate(day) },
                modifier = Modifier.width(58.dp),
                shape = RoundedCornerShape(18.dp),
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                tonalElevation = if (active) 0.dp else 1.dp
            ) {
                Column(Modifier.padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(V2DateText.dayName(day), style = MaterialTheme.typography.labelSmall)
                    Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .size(4.dp)
                            .background(
                                if (today) if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary else Color.Transparent,
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun V2DayHero(stats: Db.DayStats, progress: Float, totalMinutes: Int, conflicts: Int) {
    val animatedProgress by animateFloatAsState(progress, label = "day_progress")
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val loadText = when {
        totalMinutes > 540 -> "Kapasite yüksek"
        totalMinutes > 420 -> "Dolu bir gün"
        totalMinutes == 0 -> "Alan açık"
        else -> "Dengeli görünüyor"
    }
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Günün planı", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(loadText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("%${(animatedProgress * 100).roundToInt()}", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V2Pill("${stats.open} açık", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                V2Pill("${stats.done} tamam", Color(0xFFE3F6EC), Color(0xFF176B45))
                V2Pill(if (hours > 0) "${hours}s ${minutes}dk" else "${minutes}dk", Color(0xFFEAF2FF), Color(0xFF27527F))
            }
            AnimatedVisibility(conflicts > 0 || totalMinutes > 540) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WarningAmber, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            buildString {
                                if (conflicts > 0) append("$conflicts çakışan görev var")
                                if (conflicts > 0 && totalMinutes > 540) append(" • ")
                                if (totalMinutes > 540) append("Gün 9 saatin üstünde")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun V2Pill(text: String, bg: Color, fg: Color) {
    Surface(shape = CircleShape, color = bg, contentColor = fg) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun V2NowCard(item: Db.Item, onFocus: () -> Unit, onOpen: () -> Unit) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(Color(0xFF1E2451), Color(0xFF525AE8), Color(0xFF7D68E9))),
                    RoundedCornerShape(30.dp)
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color.White.copy(alpha = .14f), shape = CircleShape) {
                        Text("ŞİMDİ", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.weight(1f))
                    if (item.dueAt > 0) Text(v2Time(item.dueAt), color = Color.White.copy(alpha = .82f), fontWeight = FontWeight.SemiBold)
                }
                Text(item.title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.durationMinutes > 0) Text("${item.durationMinutes} dk", color = Color.White.copy(alpha = .78f))
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onOpen, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text("Detay") }
                    Button(onClick = onFocus, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF343C9B))) {
                        Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Odaklan")
                    }
                }
            }
        }
    }
}

@Composable
private fun V2CalendarConnectCard(onConnect: () -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .72f)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surface) {
                Icon(Icons.Rounded.CalendarMonth, null, Modifier.padding(10.dp).size(23.dp), tint = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Takvimini aynı akışta gör", fontWeight = FontWeight.Bold)
                Text("Google Calendar telefonda senkronizeyse toplantıların burada görünür.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onConnect) { Text("Bağla") }
        }
    }
}

@Composable
private fun V2CalendarEventCard(event: DeviceCalendarEvent) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(if (event.allDay) "Tüm gün" else v2Time(event.start), Modifier.width(58.dp).padding(top = 14.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .72f), modifier = Modifier.weight(1f)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(event.title.ifBlank { "Takvim etkinliği" }, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (event.allDay) event.calendarName else "${v2Time(event.start)}–${v2Time(event.end)} • ${event.calendarName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Rounded.Event, null, tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V2SwipeTaskRow(
    item: Db.Item,
    conflict: Boolean,
    onOpen: (Db.Item) -> Unit,
    onComplete: (Db.Item) -> Unit,
    onPostpone: (Db.Item) -> Unit,
    onMoveMinutes: (Db.Item, Int) -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onComplete(item)
                SwipeToDismissBoxValue.EndToStart -> onPostpone(item)
                else -> Unit
            }
            false
        }
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val target = state.targetValue
            val complete = target == SwipeToDismissBoxValue.StartToEnd
            val active = target != SwipeToDismissBoxValue.Settled
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (!active) Color.Transparent else if (complete) Color(0xFF1F9D61) else Color(0xFFF59E0B))
                    .padding(horizontal = 22.dp),
                contentAlignment = if (complete) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (active) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (complete) Icons.Rounded.Check else Icons.Rounded.Update, null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text(if (complete) "Tamamla" else "Yarına", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) {
        V2DraggableTaskRow(item, conflict, onOpen, onComplete, onMoveMinutes)
    }
}

@Composable
private fun V2DraggableTaskRow(
    item: Db.Item,
    conflict: Boolean,
    onOpen: (Db.Item) -> Unit,
    onComplete: (Db.Item) -> Unit,
    onMoveMinutes: (Db.Item, Int) -> Unit
) {
    var dragY by remember(item.id, item.dueAt) { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val elevation by androidx.compose.animation.core.animateDpAsState(if (dragging) 10.dp else 1.dp, label = "drag_elevation")
    val done = item.status == Db.STATUS_DONE

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            if (item.dueAt > 0) v2Time(item.dueAt) else "—",
            Modifier.width(50.dp).padding(top = 18.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.padding(top = 21.dp).size(10.dp).background(if (done) Color(0xFF1F9D61) else v2Priority(item.priority), CircleShape))
            Box(Modifier.width(2.dp).height(74.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = .55f)))
        }
        Spacer(Modifier.width(9.dp))
        Card(
            onClick = { onOpen(item) },
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { translationY = dragY }
                .pointerInput(item.id, item.dueAt) {
                    val pxPerThirtyMinutes = 54.dp.toPx()
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            dragging = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragY += amount.y
                        },
                        onDragCancel = {
                            dragging = false
                            dragY = 0f
                        },
                        onDragEnd = {
                            val rawMinutes = (dragY / pxPerThirtyMinutes) * 30f
                            val snapped = (rawMinutes / 15f).roundToInt() * 15
                            dragging = false
                            dragY = 0f
                            if (abs(snapped) >= 15) onMoveMinutes(item, snapped)
                        }
                    )
                },
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    conflict -> MaterialTheme.colorScheme.errorContainer.copy(alpha = .58f)
                    done -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f)
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation)
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onComplete(item) }, Modifier.size(38.dp)) {
                    Icon(
                        if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        null,
                        tint = if (done) Color(0xFF1F9D61) else v2Priority(item.priority)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f).animateContentSize()) {
                    Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (item.body.isNotBlank()) Text(item.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (item.durationMinutes > 0) Text("${item.durationMinutes} dk", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.priority < 2) Text(if (item.priority == 0) "Acil" else "Önemli", style = MaterialTheme.typography.labelSmall, color = v2Priority(item.priority), fontWeight = FontWeight.Bold)
                        if (conflict) Text("Çakışıyor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
                Icon(Icons.Rounded.DragHandle, "Basılı tutup taşı", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f))
            }
        }
    }
}

@Composable
private fun V2NoteCard(item: Db.Item, onOpen: (Db.Item) -> Unit) {
    Surface(onClick = { onOpen(item) }, shape = RoundedCornerShape(22.dp), color = Color(0xFFFFFBF2)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = RoundedCornerShape(13.dp), color = Color(0xFFFFE9B5)) {
                Icon(Icons.Rounded.Notes, null, Modifier.padding(9.dp).size(20.dp), tint = Color(0xFF8A5700))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.SemiBold, color = Color(0xFF372B13))
                if (item.body.isNotBlank()) Text(item.body, color = Color(0xFF6E6045), style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun V2Section(title: String, trailing: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun V2EmptyCard(title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .68f)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surface) {
                Icon(icon, null, Modifier.padding(10.dp).size(22.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun V2WeekPlanner(
    selected: LocalDate,
    db: Db,
    revision: Int,
    onMode: (V2CalendarMode) -> Unit,
    onDate: (LocalDate) -> Unit
) {
    val monday = selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = remember(monday) { (0..6).map { monday.plusDays(it.toLong()) } }
    val summaries = remember(monday, revision) {
        days.associateWith { day ->
            val items = db.getItemsForDay(day.toString()).filter { it.type == Db.TYPE_TASK }
            V2DaySummary(
                stats = db.getDayStats(day.toString()),
                minutes = items.filter { it.status == Db.STATUS_OPEN }.sumOf { if (it.durationMinutes > 0) it.durationMinutes else 30 },
                next = items.filter { it.status == Db.STATUS_OPEN }.minByOrNull { if (it.dueAt > 0) it.dueAt else Long.MAX_VALUE }
            )
        }
    }
    val totalMinutes = summaries.values.sumOf { it.minutes }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 6.dp, 16.dp, 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { V2ModeSelector(V2CalendarMode.WEEK, onMode) }
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DateRange, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Haftanın yükü", fontWeight = FontWeight.Bold)
                        Text("${V2DateText.short(monday)} – ${V2DateText.short(monday.plusDays(6))}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${totalMinutes / 60}s ${totalMinutes % 60}dk", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        items(days, key = { it.toString() }) { day ->
            val summary = summaries.getValue(day)
            V2WeekDayCard(day, summary, day == selected) {
                onDate(day)
                onMode(V2CalendarMode.DAY)
            }
        }
    }
}

private data class V2DaySummary(val stats: Db.DayStats, val minutes: Int, val next: Db.Item?)

@Composable
private fun V2WeekDayCard(day: LocalDate, s: V2DaySummary, selected: Boolean, onClick: () -> Unit) {
    val total = s.stats.open + s.stats.done
    val p = if (total == 0) 0f else s.stats.done.toFloat() / total
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 1.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(V2DateText.dayName(day), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    s.next?.title ?: if (total == 0) "Boş gün" else "Plan hazır",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { p }, Modifier.fillMaxWidth().height(5.dp).clip(CircleShape))
                Spacer(Modifier.height(6.dp))
                Text("${s.stats.open} açık • ${s.stats.done} tamam • ${s.minutes / 60}s ${s.minutes % 60}dk", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun V2MonthPlanner(
    selected: LocalDate,
    db: Db,
    revision: Int,
    onMode: (V2CalendarMode) -> Unit,
    onDate: (LocalDate) -> Unit
) {
    var shownMonth by remember(selected.year, selected.monthValue) { mutableStateOf(selected.withDayOfMonth(1)) }
    val gridStart = shownMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = remember(gridStart) { (0..41).map { gridStart.plusDays(it.toLong()) } }
    val stats = remember(gridStart, revision) { days.associateWith { db.getDayStats(it.toString()) } }
    val selectedItems = remember(selected, revision) { db.getItemsForDay(selected.toString()).filter { it.type == Db.TYPE_TASK }.take(3) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp, 6.dp, 12.dp, 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { V2ModeSelector(V2CalendarMode.MONTH, onMode) }
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton({ shownMonth = shownMonth.minusMonths(1) }) { Icon(Icons.Rounded.ChevronLeft, "Önceki ay") }
                        Text(V2DateText.month(shownMonth), Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        IconButton({ shownMonth = shownMonth.plusMonths(1) }) { Icon(Icons.Rounded.ChevronRight, "Sonraki ay") }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz").forEach { label ->
                            Text(label, Modifier.weight(1f).padding(vertical = 8.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    days.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth()) {
                            week.forEach { day ->
                                V2MonthCell(
                                    day = day,
                                    inMonth = day.month == shownMonth.month,
                                    selected = day == selected,
                                    today = day == LocalDate.now(),
                                    stats = stats.getValue(day),
                                    onClick = { onDate(day) }
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(V2DateText.full(selected), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { onMode(V2CalendarMode.DAY) }) { Text("Günü aç") }
            }
        }
        if (selectedItems.isEmpty()) item { V2EmptyCard("Bu gün boş", "Takvimden başka bir gün seçebilir veya yeni görev ekleyebilirsin.", Icons.Rounded.EventAvailable) }
        else items(selectedItems, key = { "month-${it.id}" }) { item -> V2CompactTask(item) }
    }
}

@Composable
private fun RowScope.V2MonthCell(day: LocalDate, inMonth: Boolean, selected: Boolean, today: Boolean, stats: Db.DayStats, onClick: () -> Unit) {
    val count = stats.open + stats.done
    Box(
        Modifier
            .weight(1f)
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.dayOfMonth.toString(),
                color = when {
                    selected -> MaterialTheme.colorScheme.onPrimary
                    !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .42f)
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (selected || today) FontWeight.Bold else FontWeight.Normal
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(minOf(count, 3)) {
                    Box(Modifier.size(3.dp).background(if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, CircleShape))
                }
            }
        }
        if (today && !selected) Box(Modifier.align(Alignment.TopCenter).padding(top = 4.dp).size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
    }
}

@Composable
private fun V2CompactTask(item: Db.Item) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(v2Priority(item.priority), CircleShape))
            Spacer(Modifier.width(10.dp))
            Text(item.title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.dueAt > 0) Text(v2Time(item.dueAt), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun V2HistoryScreen(
    selectedDate: LocalDate,
    stats: Db.DayStats,
    events: List<Db.Event>,
    onDate: (LocalDate) -> Unit,
    onPickDate: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 6.dp, 16.dp, 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { V2DateStrip(selectedDate, onDate) }
        item {
            Surface(onClick = onPickDate, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${stats.done} tamam • ${stats.open} açık • ${stats.postponed} erteleme", fontWeight = FontWeight.Bold)
                        Text("Geçmiş günlerde yapılan değişiklikler silinmez.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.CalendarMonth, null)
                }
            }
        }
        item { V2Section("Hareket günlüğü", "${events.size} kayıt") }
        if (events.isEmpty()) item { V2EmptyCard("Bu gün için geçmiş yok", "Henüz oluşturma, düzenleme, tamamlama veya erteleme kaydı bulunmuyor.", Icons.Rounded.HistoryToggleOff) }
        else items(events, key = { it.id }) { e ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(v2Time(e.createdAt), Modifier.width(52.dp).padding(top = 12.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(v2EventName(e.eventType), color = v2EventColor(e.eventType), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text(e.titleSnapshot, fontWeight = FontWeight.SemiBold)
                        if (e.details.isNotBlank()) Text(e.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun V2TopicsScreen(topics: List<Db.Topic>, db: Db, onOpen: (Long) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.primaryContainer)), RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(10.dp))
                    Text("Konu hafızası", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Dağınık notları aynı konu altında topla; geçmişe dönüp tek yerden gör.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { V2Section("Konular", "${topics.size} başlık") }
        if (topics.isEmpty()) item { V2EmptyCard("Henüz konu oluşmadı", "Bir not eklediğinde Günüm benzer içerikleri konu halinde toplamaya başlayacak.", Icons.Rounded.FolderOpen) }
        else items(topics, key = { it.id }) { topic ->
            Card(onClick = { onOpen(topic.id) }, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Icon(Icons.Rounded.Folder, null, Modifier.padding(11.dp).size(23.dp), tint = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(topic.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${db.countItemsInTopic(topic.id)} kayıt • otomatik ${if (topic.autoGroup) "açık" else "kapalı"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (topic.locked) Icon(Icons.Rounded.Lock, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Rounded.ChevronRight, null)
                }
            }
        }
    }
}

@Composable
fun V2TopicScreen(topic: Db.Topic?, items: List<Db.Item>, onOpen: (Db.Item) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Column(Modifier.padding(20.dp)) {
                    Text(topic?.title ?: "Konu", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Bu başlığın kronolojik hafızası", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { V2Section("Kayıtlar", items.size.toString()) }
        if (items.isEmpty()) item { V2EmptyCard("Bu konu henüz boş", "Yeni bir not veya görev ekleyebilirsin.", Icons.Rounded.FolderOpen) }
        else items(items, key = { it.id }) { item ->
            if (item.type == Db.TYPE_NOTE) V2NoteCard(item, onOpen)
            else Surface(onClick = { onOpen(item) }, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (item.status == Db.STATUS_DONE) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = if (item.status == Db.STATUS_DONE) Color(0xFF1F9D61) else v2Priority(item.priority))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.SemiBold)
                        Text(item.dayKey + if (item.dueAt > 0) " • ${v2Time(item.dueAt)}" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun V2SearchScreen(query: String, onQuery: (String) -> Unit, results: List<Db.Item>, onOpen: (Db.Item) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton({ onQuery("") }) { Icon(Icons.Rounded.Close, null) } },
                placeholder = { Text("Görev, not veya kelime ara") },
                shape = RoundedCornerShape(22.dp)
            )
        }
        item { V2Section(if (query.isBlank()) "Hızlı bul" else "Sonuçlar", if (query.isBlank()) "Geçmişin tamamı" else "${results.size} kayıt") }
        if (query.isBlank()) item { V2EmptyCard("Geçmişin aranabilir", "Başlıkta ve not içeriğinde geçen herhangi bir kelimeyi yaz.", Icons.Rounded.ManageSearch) }
        else if (results.isEmpty()) item { V2EmptyCard("Sonuç bulunamadı", "Daha kısa veya daha genel bir kelime deneyebilirsin.", Icons.Rounded.SearchOff) }
        else items(results, key = { it.id }) { item ->
            if (item.type == Db.TYPE_NOTE) V2NoteCard(item, onOpen)
            else Surface(onClick = { onOpen(item) }, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (item.status == Db.STATUS_DONE) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = if (item.status == Db.STATUS_DONE) Color(0xFF1F9D61) else v2Priority(item.priority))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.SemiBold)
                        Text(item.dayKey + if (item.dueAt > 0) " • ${v2Time(item.dueAt)}" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun V2ConflictIds(tasks: List<Db.Item>): Set<Long> {
    val active = tasks.filter { it.status == Db.STATUS_OPEN && it.dueAt > 0 }.sortedBy { it.dueAt }
    val out = mutableSetOf<Long>()
    for (i in active.indices) {
        val a = active[i]
        val aEnd = a.dueAt + (if (a.durationMinutes > 0) a.durationMinutes else 30) * 60_000L
        for (j in i + 1 until active.size) {
            val b = active[j]
            if (b.dueAt >= aEnd) break
            out += a.id
            out += b.id
        }
    }
    return out
}

fun v2Time(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
fun v2Priority(p: Int): Color = when (p) { 0 -> Color(0xFFE34D59); 1 -> Color(0xFFF59E0B); else -> Color(0xFF5B5FEF) }
fun v2EventName(type: String): String = when (type) {
    "CREATED" -> "Oluşturuldu"
    "NOTE_CREATED" -> "Not alındı"
    "COMPLETED" -> "Tamamlandı"
    "POSTPONED" -> "Ertelendi"
    "MOVED_IN" -> "Taşındı"
    "EDITED" -> "Düzenlendi"
    "DELETED" -> "Silindi"
    "REOPENED" -> "Tekrar açıldı"
    "RECUR_CREATED" -> "Tekrar oluşturuldu"
    "SCHEDULED" -> "Planlandı"
    else -> type
}
fun v2EventColor(type: String): Color = when (type) {
    "COMPLETED" -> Color(0xFF1F9D61)
    "POSTPONED", "DELETED" -> Color(0xFFE34D59)
    "NOTE_CREATED" -> Color(0xFFF59E0B)
    else -> Color(0xFF5B5FEF)
}
