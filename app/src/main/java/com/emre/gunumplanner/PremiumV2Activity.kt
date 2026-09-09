package com.emre.gunumplanner

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.*

class PremiumV2Activity : ComponentActivity() {
    lateinit var db: Db
    private var voiceResult: ((String) -> Unit)? = null
    private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            val text = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (text.isNotBlank()) voiceResult?.invoke(text.trim())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Db(this)
        askNotifications()
        setContent { V2Theme { V2PlannerApp(this, db) } }
    }

    fun voice(cb: (String) -> Unit) {
        voiceResult = cb
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Günüm seni dinliyor")
        }
        try { voiceLauncher.launch(intent) } catch (_: ActivityNotFoundException) {}
    }

    fun pickDate(date: LocalDate, cb: (LocalDate) -> Unit) =
        DatePickerDialog(this, { _, y, m, d -> cb(LocalDate.of(y, m + 1, d)) }, date.year, date.monthValue - 1, date.dayOfMonth).show()

    fun pickTime(time: LocalTime, cb: (LocalTime) -> Unit) =
        TimePickerDialog(this, { _, h, m -> cb(LocalTime.of(h, m)) }, time.hour, time.minute, true).show()

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 901)
        }
    }
}

private val V2Light = lightColorScheme(
    primary = Color(0xFF5B5FEF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E9FF),
    onPrimaryContainer = Color(0xFF20215E),
    secondary = Color(0xFF7B5CE1),
    secondaryContainer = Color(0xFFF0EAFF),
    tertiary = Color(0xFF00897B),
    background = Color(0xFFF7F8FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F2F7),
    onSurface = Color(0xFF181A21),
    onSurfaceVariant = Color(0xFF656B78),
    outline = Color(0xFFD8DCE7),
    error = Color(0xFFCF3D4F)
)

private val V2Dark = darkColorScheme(
    primary = Color(0xFFC2C4FF),
    onPrimary = Color(0xFF25296D),
    primaryContainer = Color(0xFF383D8C),
    onPrimaryContainer = Color(0xFFE3E4FF),
    secondary = Color(0xFFD3C3FF),
    secondaryContainer = Color(0xFF4D3E83),
    tertiary = Color(0xFF6DD6C8),
    background = Color(0xFF0E1015),
    surface = Color(0xFF17191F),
    surfaceVariant = Color(0xFF22252D),
    onSurface = Color(0xFFF3F4F7),
    onSurfaceVariant = Color(0xFFB7BBC5),
    outline = Color(0xFF3D414B),
    error = Color(0xFFFFB3BC)
)

@Composable
fun V2Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) V2Dark else V2Light,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(34.dp)
        ),
        content = content
    )
}

enum class V2Screen { PLANNER, HISTORY, TOPICS, SEARCH, TOPIC }
enum class V2CalendarMode { DAY, WEEK, MONTH }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V2PlannerApp(activity: PremiumV2Activity, db: Db) {
    var screen by rememberSaveable { mutableStateOf(V2Screen.PLANNER) }
    var mode by rememberSaveable { mutableStateOf(V2CalendarMode.DAY) }
    var dateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var topicId by rememberSaveable { mutableStateOf(0L) }
    var query by rememberSaveable { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }

    var addSheet by remember { mutableStateOf(false) }
    var quickDialog by remember { mutableStateOf(false) }
    var taskDialog by remember { mutableStateOf(false) }
    var noteDialog by remember { mutableStateOf(false) }
    var taskEdit by remember { mutableStateOf<Db.Item?>(null) }
    var noteEdit by remember { mutableStateOf<Db.Item?>(null) }
    var chosen by remember { mutableStateOf<Db.Item?>(null) }
    var focusItem by remember { mutableStateOf<Db.Item?>(null) }

    val selectedDate = runCatching { LocalDate.parse(dateText) }.getOrDefault(LocalDate.now())
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var calendarGranted by remember {
        mutableStateOf(
            activity.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingCalendarAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        calendarGranted = result[Manifest.permission.READ_CALENDAR] == true ||
            activity.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val action = pendingCalendarAction
        pendingCalendarAction = null
        if (calendarGranted) action?.invoke()
        else scope.launch { snackbar.showSnackbar("Takvim izni verilmedi") }
    }

    fun refresh() { revision++ }

    fun withCalendarPermission(action: () -> Unit) {
        val read = activity.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val write = activity.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        if (read && write) {
            calendarGranted = true
            action()
        } else {
            pendingCalendarAction = action
            calendarPermissionLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
        }
    }

    fun scheduleIfFuture(item: Db.Item?) {
        if (item != null && item.dueAt > System.currentTimeMillis() && item.status == Db.STATUS_OPEN) {
            ReminderScheduler.schedule(activity, item.id, item.dueAt)
        }
    }

    fun complete(item: Db.Item) {
        if (item.status == Db.STATUS_DONE) {
            db.reopenItem(item.id)
            scheduleIfFuture(db.getItem(item.id))
        } else {
            ReminderScheduler.cancel(activity, item.id)
            val nextId = db.completeItem(item.id)
            if (nextId > 0) scheduleIfFuture(db.getItem(nextId))
        }
        refresh()
    }

    fun postpone(item: Db.Item) {
        val oldDate = runCatching { LocalDate.parse(item.dayKey) }.getOrDefault(selectedDate)
        val newDate = oldDate.plusDays(1)
        val newDue = if (item.dueAt > 0) {
            val t = Instant.ofEpochMilli(item.dueAt).atZone(ZoneId.systemDefault()).toLocalTime()
            LocalDateTime.of(newDate, t).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else 0L
        ReminderScheduler.cancel(activity, item.id)
        db.postponeItem(item.id, newDate.toString(), newDue)
        scheduleIfFuture(db.getItem(item.id))
        refresh()
    }

    fun moveByMinutes(item: Db.Item, deltaMinutes: Int) {
        if (deltaMinutes == 0) return
        val baseDate = runCatching { LocalDate.parse(item.dayKey) }.getOrDefault(selectedDate)
        val baseDateTime = if (item.dueAt > 0) {
            Instant.ofEpochMilli(item.dueAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
        } else {
            baseDate.atTime(9, 0)
        }
        val moved = baseDateTime.plusMinutes(deltaMinutes.toLong())
        val due = moved.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        ReminderScheduler.cancel(activity, item.id)
        db.rescheduleItem(item.id, moved.toLocalDate().toString(), due)
        scheduleIfFuture(db.getItem(item.id))
        dateText = moved.toLocalDate().toString()
        refresh()
    }

    val dayItems = remember(dateText, revision) { db.getItemsForDay(dateText) }
    val stats = remember(dateText, revision) { db.getDayStats(dateText) }
    val historyEvents = remember(screen, dateText, revision) {
        if (screen == V2Screen.HISTORY) db.getEventsForDay(dateText) else emptyList()
    }
    val topics = remember(screen, revision) {
        if (screen == V2Screen.TOPICS || screen == V2Screen.TOPIC) db.getTopics() else emptyList()
    }
    val topicItems = remember(screen, topicId, revision) {
        if (screen == V2Screen.TOPIC) db.getItemsForTopic(topicId) else emptyList()
    }
    val results = remember(screen, query, revision) {
        if (screen == V2Screen.SEARCH && query.isNotBlank()) db.searchItems(query) else emptyList()
    }
    val deviceEvents = remember(dateText, calendarGranted, revision) {
        if (calendarGranted) CalendarBridge.eventsForDay(activity, selectedDate) else emptyList()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            V2TopBar(
                screen = screen,
                selectedDate = selectedDate,
                topicTitle = if (screen == V2Screen.TOPIC) db.getTopic(topicId)?.title else null,
                onBack = { screen = V2Screen.TOPICS },
                onCalendar = {
                    activity.pickDate(selectedDate) {
                        dateText = it.toString()
                        screen = V2Screen.PLANNER
                        mode = V2CalendarMode.DAY
                    }
                },
                onToday = {
                    dateText = LocalDate.now().toString()
                    screen = V2Screen.PLANNER
                    mode = V2CalendarMode.DAY
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                V2NavItem(Icons.Rounded.Home, "Bugün", screen == V2Screen.PLANNER) {
                    screen = V2Screen.PLANNER
                    dateText = LocalDate.now().toString()
                }
                V2NavItem(Icons.Rounded.CalendarMonth, "Geçmiş", screen == V2Screen.HISTORY) {
                    screen = V2Screen.HISTORY
                }
                V2NavItem(Icons.Rounded.Folder, "Konular", screen == V2Screen.TOPICS || screen == V2Screen.TOPIC) {
                    screen = V2Screen.TOPICS
                }
                V2NavItem(Icons.Rounded.Search, "Ara", screen == V2Screen.SEARCH) {
                    screen = V2Screen.SEARCH
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { addSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp)
            ) { Icon(Icons.Rounded.Add, "Ekle") }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "main_screen",
            modifier = Modifier.padding(innerPadding)
        ) { target ->
            when (target) {
                V2Screen.PLANNER -> V2PlannerSurface(
                    selectedDate = selectedDate,
                    mode = mode,
                    stats = stats,
                    dayItems = dayItems,
                    calendarEvents = deviceEvents,
                    calendarConnected = calendarGranted,
                    db = db,
                    revision = revision,
                    onMode = { mode = it },
                    onDate = { dateText = it.toString() },
                    onOpen = { chosen = it },
                    onComplete = { complete(it) },
                    onPostpone = { postpone(it) },
                    onMoveMinutes = { item, mins -> moveByMinutes(item, mins) },
                    onFocus = { focusItem = it },
                    onConnectCalendar = { withCalendarPermission { refresh() } },
                    onVoice = {
                        activity.voice { text ->
                            val p = NaturalLanguageParser.parse(text, selectedDate)
                            val topic = TopicEngine.findMatchingTopic(db, p.title, p.original)
                            val id = db.insertItem(Db.TYPE_TASK, p.title, "", p.dayKey, p.dueAt, topic, p.recurrence, p.durationMinutes, p.priority)
                            if (p.dueAt > 0) ReminderScheduler.schedule(activity, id, p.dueAt)
                            dateText = p.dayKey
                            refresh()
                        }
                    },
                    onSmartPlan = {
                        val plan = SmartPlanner.propose(db.getOpenTasksUpTo(LocalDate.now().toString()), LocalDate.now())
                        plan.forEach {
                            ReminderScheduler.cancel(activity, it.itemId)
                            db.rescheduleItem(it.itemId, it.newDayKey, it.newDueAt)
                            ReminderScheduler.schedule(activity, it.itemId, it.newDueAt)
                        }
                        dateText = LocalDate.now().toString()
                        refresh()
                        scope.launch {
                            snackbar.showSnackbar(if (plan.isEmpty()) "Planlanacak görev yok" else "${plan.size} görev yeniden planlandı")
                        }
                    }
                )

                V2Screen.HISTORY -> V2HistoryScreen(
                    selectedDate = selectedDate,
                    stats = stats,
                    events = historyEvents,
                    onDate = { dateText = it.toString() },
                    onPickDate = { activity.pickDate(selectedDate) { dateText = it.toString() } }
                )

                V2Screen.TOPICS -> V2TopicsScreen(
                    topics = topics,
                    db = db,
                    onOpen = { topicId = it; screen = V2Screen.TOPIC }
                )

                V2Screen.TOPIC -> V2TopicScreen(
                    topic = db.getTopic(topicId),
                    items = topicItems,
                    onOpen = { chosen = it }
                )

                V2Screen.SEARCH -> V2SearchScreen(
                    query = query,
                    onQuery = { query = it },
                    results = results,
                    onOpen = { chosen = it }
                )
            }
        }
    }

    if (addSheet) {
        V2AddSheet(
            close = { addSheet = false },
            quick = { addSheet = false; quickDialog = true },
            task = { addSheet = false; taskEdit = null; taskDialog = true },
            note = { addSheet = false; noteEdit = null; noteDialog = true },
            voiceTask = {
                addSheet = false
                activity.voice { text ->
                    val p = NaturalLanguageParser.parse(text, selectedDate)
                    val topic = TopicEngine.findMatchingTopic(db, p.title, p.original)
                    val id = db.insertItem(Db.TYPE_TASK, p.title, "", p.dayKey, p.dueAt, topic, p.recurrence, p.durationMinutes, p.priority)
                    if (p.dueAt > 0) ReminderScheduler.schedule(activity, id, p.dueAt)
                    dateText = p.dayKey
                    refresh()
                }
            },
            voiceNote = {
                addSheet = false
                activity.voice { text ->
                    val title = TopicEngine.suggestTitle(text)
                    val topic = if (screen == V2Screen.TOPIC) topicId else TopicEngine.findOrCreateTopic(db, title, text)
                    db.insertItem(Db.TYPE_NOTE, title, text, LocalDate.now().toString(), 0L, topic)
                    TopicEngine.refreshAutoTitle(db, topic)
                    refresh()
                }
            }
        )
    }

    if (quickDialog) {
        V2QuickDialog(selectedDate, { quickDialog = false }) { p ->
            val topic = TopicEngine.findMatchingTopic(db, p.title, p.original)
            val id = db.insertItem(Db.TYPE_TASK, p.title, "", p.dayKey, p.dueAt, topic, p.recurrence, p.durationMinutes, p.priority)
            if (p.dueAt > 0) ReminderScheduler.schedule(activity, id, p.dueAt)
            dateText = p.dayKey
            quickDialog = false
            refresh()
        }
    }

    if (taskDialog) {
        V2TaskDialog(taskEdit, selectedDate, activity, { taskDialog = false }) { title, body, day, time, duration, recurrence, priority ->
            val due = if (time == null) 0L else LocalDateTime.of(day, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val old = taskEdit
            if (old == null) {
                val topic = TopicEngine.findMatchingTopic(db, title, body)
                val id = db.insertItem(Db.TYPE_TASK, title, body, day.toString(), due, topic, recurrence, duration, priority)
                if (due > 0) ReminderScheduler.schedule(activity, id, due)
            } else {
                ReminderScheduler.cancel(activity, old.id)
                db.updateItem(old.id, title, body, day.toString(), due, old.topicId, recurrence, duration, priority)
                scheduleIfFuture(db.getItem(old.id))
            }
            dateText = day.toString()
            taskDialog = false
            refresh()
        }
    }

    if (noteDialog) {
        V2NoteDialog(noteEdit, { noteDialog = false }) { title0, body ->
            val title = title0.ifBlank { TopicEngine.suggestTitle(body) }
            val old = noteEdit
            if (old == null) {
                val topic = if (screen == V2Screen.TOPIC) topicId else TopicEngine.findOrCreateTopic(db, title, body)
                db.insertItem(Db.TYPE_NOTE, title, body, LocalDate.now().toString(), 0L, topic)
                TopicEngine.refreshAutoTitle(db, topic)
            } else {
                db.updateItem(old.id, title, body, old.dayKey, 0L, old.topicId, "", 0, 2)
                if (old.topicId > 0) TopicEngine.refreshAutoTitle(db, old.topicId)
            }
            noteDialog = false
            refresh()
        }
    }

    chosen?.let { item ->
        V2ItemSheet(
            item = item,
            close = { chosen = null },
            complete = { complete(item); chosen = null },
            focus = { chosen = null; focusItem = item },
            tomorrow = { postpone(item); chosen = null },
            addToCalendar = {
                withCalendarPermission {
                    val ok = CalendarBridge.addTask(activity, item)
                    scope.launch { snackbar.showSnackbar(if (ok) "Takvime eklendi" else "Takvime eklenemedi") }
                }
            },
            edit = {
                chosen = null
                if (item.type == Db.TYPE_NOTE) { noteEdit = item; noteDialog = true }
                else { taskEdit = item; taskDialog = true }
            },
            delete = {
                ReminderScheduler.cancel(activity, item.id)
                db.deleteItem(item.id)
                chosen = null
                refresh()
            }
        )
    }

    focusItem?.let { item ->
        V2FocusDialog(
            item = item,
            close = { focusItem = null },
            complete = { complete(item); focusItem = null },
            postpone = { postpone(item); focusItem = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V2TopBar(
    screen: V2Screen,
    selectedDate: LocalDate,
    topicTitle: String?,
    onBack: () -> Unit,
    onCalendar: () -> Unit,
    onToday: () -> Unit
) {
    val title = when (screen) {
        V2Screen.PLANNER -> if (selectedDate == LocalDate.now()) "Bugün" else V2DateText.short(selectedDate)
        V2Screen.HISTORY -> "Geçmiş"
        V2Screen.TOPICS -> "Konular"
        V2Screen.SEARCH -> "Arama"
        V2Screen.TOPIC -> topicTitle ?: "Konu"
    }
    TopAppBar(
        title = { V2TopTitle(title, screen, selectedDate) },
        navigationIcon = {
            if (screen == V2Screen.TOPIC) V2BackButton(onBack)
        },
        actions = {
            if (screen == V2Screen.PLANNER || screen == V2Screen.HISTORY) {
                V2TodayButton(onToday)
                V2CalendarButton(onCalendar)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
private fun RowScope.V2NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, label) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary
        )
    )
}
