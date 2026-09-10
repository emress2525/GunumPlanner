from pathlib import Path

# Main planner integration
p = Path('app/src/main/java/com/emre/gunumplanner/PremiumV2Activity.kt')
s = p.read_text(encoding='utf-8')

if 'import androidx.compose.ui.platform.LocalContext' not in s:
    s = s.replace('import androidx.compose.ui.Modifier\n', 'import androidx.compose.ui.Modifier\nimport androidx.compose.ui.platform.LocalContext\n')
if 'import androidx.compose.material.icons.rounded.Settings' not in s:
    s = s.replace('import androidx.compose.material.icons.rounded.Search\n', 'import androidx.compose.material.icons.rounded.Search\nimport androidx.compose.material.icons.rounded.Settings\n')

if 'FullRepository.ensureSchema(db)' not in s:
    s = s.replace('        db = Db(this)\n', '        db = Db(this)\n        FullRepository.ensureSchema(db)\n', 1)

old_create = '''        setContent { V2Theme { V2PlannerApp(this, db) } }
    }
'''
new_create = '''        setContent { V2Theme { V2PlannerApp(this, db) } }
        if (!FullRepository.Prefs.onboardingSeen(this) && !FullRepository.hasAnyItems(db)) {
            startActivity(Intent(this, FullOnboardingActivity::class.java))
        }
    }
'''
if old_create in s and 'FullOnboardingActivity' not in s:
    s = s.replace(old_create, new_create, 1)

old_theme = '''fun V2Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) V2Dark else V2Light,
'''
new_theme = '''fun V2Theme(content: @Composable () -> Unit) {
    val themeContext = LocalContext.current
    val prefTheme = FullRepository.Prefs.theme(themeContext)
    val darkMode = when (prefTheme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkMode) V2Dark else V2Light,
'''
if old_theme in s:
    s = s.replace(old_theme, new_theme, 1)

s = s.replace('    fun refresh() { revision++ }', '    fun refresh() { revision++; GunumWidgetProvider.refreshAll(activity) }')

old_next = '''            val nextId = db.completeItem(item.id)
            if (nextId > 0) scheduleIfFuture(db.getItem(nextId))'''
new_next = '''            val nextId = db.completeItem(item.id)
            if (nextId > 0) {
                FullRepository.propagateSeries(db, item.id, nextId)
                scheduleIfFuture(db.getItem(nextId))
            }'''
if old_next in s:
    s = s.replace(old_next, new_next, 1)

# Top bar settings shortcut.
old_call = '''                onToday = {
                    dateText = LocalDate.now().toString()
                    screen = V2Screen.PLANNER
                    mode = V2CalendarMode.DAY
                }
'''
new_call = '''                onToday = {
                    dateText = LocalDate.now().toString()
                    screen = V2Screen.PLANNER
                    mode = V2CalendarMode.DAY
                },
                onSettings = { activity.startActivity(Intent(activity, FullSettingsActivity::class.java)) }
'''
if old_call in s and 'onSettings = {' not in s:
    s = s.replace(old_call, new_call, 1)

old_sig = '''    onCalendar: () -> Unit,
    onToday: () -> Unit
) {'''
new_sig = '''    onCalendar: () -> Unit,
    onToday: () -> Unit,
    onSettings: () -> Unit
) {'''
if old_sig in s:
    s = s.replace(old_sig, new_sig, 1)

old_actions = '''            if (screen == V2Screen.PLANNER || screen == V2Screen.HISTORY) {
                V2TodayButton(onToday)
                V2CalendarButton(onCalendar)
            }
        },'''
new_actions = '''            if (screen == V2Screen.PLANNER || screen == V2Screen.HISTORY) {
                V2TodayButton(onToday)
                V2CalendarButton(onCalendar)
            }
            IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "Ayarlar") }
        },'''
if old_actions in s:
    s = s.replace(old_actions, new_actions, 1)

# Smart plan: approval screen by default; users may opt out in Settings.
old_smart = '''                    onSmartPlan = {
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
'''
new_smart = '''                    onSmartPlan = {
                        if (FullRepository.Prefs.autoPlanApproval(activity)) {
                            activity.startActivity(Intent(activity, SmartPlanReviewActivity::class.java))
                        } else {
                            val plan = SmartPlanner.propose(db.getOpenTasksUpTo(LocalDate.now().toString()), LocalDate.now())
                            plan.forEach {
                                ReminderScheduler.cancel(activity, it.itemId)
                                db.rescheduleItem(it.itemId, it.newDayKey, it.newDueAt)
                                ReminderScheduler.schedule(activity, it.itemId, it.newDueAt)
                            }
                            dateText = LocalDate.now().toString()
                            refresh()
                            scope.launch { snackbar.showSnackbar(if (plan.isEmpty()) "Planlanacak görev yok" else "${plan.size} görev yeniden planlandı") }
                        }
                    }
'''
if old_smart in s:
    s = s.replace(old_smart, new_smart, 1)

# Recurring task series metadata on create/edit.
needle_create_task = '''                val id = db.insertItem(Db.TYPE_TASK, title, body, day.toString(), due, topic, recurrence, duration, priority)
                if (due > 0) ReminderScheduler.schedule(activity, id, due)'''
repl_create_task = '''                val id = db.insertItem(Db.TYPE_TASK, title, body, day.toString(), due, topic, recurrence, duration, priority)
                if (recurrence.isNotBlank()) FullRepository.ensureSeries(db, id)
                if (due > 0) ReminderScheduler.schedule(activity, id, due)'''
if needle_create_task in s:
    s = s.replace(needle_create_task, repl_create_task, 1)

needle_edit_task = '''                db.updateItem(old.id, title, body, day.toString(), due, old.topicId, recurrence, duration, priority)
                scheduleIfFuture(db.getItem(old.id))'''
repl_edit_task = '''                db.updateItem(old.id, title, body, day.toString(), due, old.topicId, recurrence, duration, priority)
                if (recurrence.isNotBlank()) FullRepository.ensureSeries(db, old.id)
                scheduleIfFuture(db.getItem(old.id))'''
if needle_edit_task in s:
    s = s.replace(needle_edit_task, repl_edit_task, 1)

# Soft delete: keep data and linked attachments for restore.
old_delete = '''                ReminderScheduler.cancel(activity, item.id)
                db.deleteItem(item.id)
                chosen = null
                refresh()'''
new_delete = '''                ReminderScheduler.cancel(activity, item.id)
                LocationReminderManager.cancel(activity, item.id)
                FullRepository.moveToTrash(db, item)
                chosen = null
                refresh()'''
if old_delete in s:
    s = s.replace(old_delete, new_delete, 1)

p.write_text(s, encoding='utf-8')

# Dialogs: checklist, recurring series, actual focus time and defaults.
p = Path('app/src/main/java/com/emre/gunumplanner/PremiumV2Dialogs.kt')
s = p.read_text(encoding='utf-8')

s = s.replace('item?.durationMinutes?.takeIf { it > 0 } ?: 30', 'item?.durationMinutes?.takeIf { it > 0 } ?: FullRepository.Prefs.defaultDuration(activity)')

marker = '''                Icon(Icons.Rounded.AttachFile, null); Spacer(Modifier.width(8.dp)); Text("Fotoğraf • Dosya • Konum")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = edit, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {'''
expanded = '''                Icon(Icons.Rounded.AttachFile, null); Spacer(Modifier.width(8.dp)); Text("Fotoğraf • Dosya • Konum")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    extrasContext.startActivity(Intent(extrasContext, ChecklistActivity::class.java).putExtra("item_id", item.id))
                    close()
                },
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Rounded.Checklist, null); Spacer(Modifier.width(8.dp)); Text("Alt görevler / checklist") }
            if (item.type == Db.TYPE_TASK && item.recurrence.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        extrasContext.startActivity(Intent(extrasContext, SeriesEditActivity::class.java).putExtra("item_id", item.id))
                        close()
                    },
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) { Icon(Icons.Rounded.Repeat, null); Spacer(Modifier.width(8.dp)); Text("Tekrar serisini yönet") }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = edit, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {'''
if marker in s and 'Alt görevler / checklist' not in s:
    s = s.replace(marker, expanded, 1)

s = s.replace('Icon(Icons.Rounded.Delete, null); Spacer(Modifier.width(8.dp)); Text("Sil")', 'Icon(Icons.Rounded.Delete, null); Spacer(Modifier.width(8.dp)); Text("Çöp kutusuna taşı")', 1)

focus_head = '''fun V2FocusDialog(item: Db.Item, close: () -> Unit, complete: () -> Unit, postpone: () -> Unit) {
    var running by remember { mutableStateOf(false) }
    var seconds by remember { mutableLongStateOf(0L) }
'''
focus_repl = '''fun V2FocusDialog(item: Db.Item, close: () -> Unit, complete: () -> Unit, postpone: () -> Unit) {
    val focusContext = LocalContext.current
    var running by remember { mutableStateOf(false) }
    var seconds by remember { mutableLongStateOf(0L) }
    val historicalAverage = remember(item.id) { FullRepository.averageFocusMinutes(Db(focusContext), item.id) }
    fun saveFocus() {
        if (seconds > 0L) {
            val end = System.currentTimeMillis()
            FullRepository.recordFocus(Db(focusContext), item.id, end - seconds * 1000L, end)
        }
    }
'''
if focus_head in s:
    s = s.replace(focus_head, focus_repl, 1)

s = s.replace('Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false))', 'Dialog(onDismissRequest = { saveFocus(); close() }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false))', 1)
s = s.replace('IconButton(onClick = close) { Icon(Icons.Rounded.Close, "Kapat") }', 'IconButton(onClick = { saveFocus(); close() }) { Icon(Icons.Rounded.Close, "Kapat") }', 1)
s = s.replace('Text("hedef ${item.durationMinutes.takeIf { it > 0 } ?: 30} dk", color = MaterialTheme.colorScheme.onSurfaceVariant)', 'Text("hedef ${item.durationMinutes.takeIf { it > 0 } ?: 30} dk", color = MaterialTheme.colorScheme.onSurfaceVariant)\n                            if (historicalAverage > 0) Text("geçmiş ortalama $historicalAverage dk", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)')
s = s.replace('OutlinedButton(onClick = postpone, Modifier.weight(1f), shape = RoundedCornerShape(18.dp))', 'OutlinedButton(onClick = { saveFocus(); postpone() }, Modifier.weight(1f), shape = RoundedCornerShape(18.dp))', 1)
s = s.replace('Button(onClick = complete, Modifier.weight(1f), shape = RoundedCornerShape(18.dp))', 'Button(onClick = { saveFocus(); complete() }, Modifier.weight(1f), shape = RoundedCornerShape(18.dp))', 1)

p.write_text(s, encoding='utf-8')

# Task extras: saved locations can be assigned in one tap, and current location can be saved for future tasks.
p = Path('app/src/main/java/com/emre/gunumplanner/TaskExtrasActivity.kt')
s = p.read_text(encoding='utf-8')

if 'val savedPlaces = remember(revision)' not in s:
    s = s.replace('    val storedLocation = remember(revision) { ExtrasRepository.getLocation(db, itemId) }\n', '    val storedLocation = remember(revision) { ExtrasRepository.getLocation(db, itemId) }\n    val savedPlaces = remember(revision) { FullRepository.savedPlaces(db) }\n', 1)

ui_marker = '''            OutlinedTextField(label, { label = it }, Modifier.fillMaxWidth(), label = { Text("Konum adı") }, placeholder = { Text("Örn. Fabrika, Ev, Market") }, singleLine = true)
'''
ui_repl = '''            if (savedPlaces.isNotEmpty()) {
                Text("Kayıtlı yerler", fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(savedPlaces, key = { it.id }) { p ->
                        AssistChip(
                            onClick = {
                                label = p.name; address = p.address; lat = p.lat; lng = p.lng; radius = p.radius
                                statusText = "${p.name} göreve atandı"
                            },
                            label = { Text(p.name) },
                            leadingIcon = { Icon(Icons.Rounded.Place, null, Modifier.size(17.dp)) }
                        )
                    }
                }
            }
            OutlinedTextField(label, { label = it }, Modifier.fillMaxWidth(), label = { Text("Konum adı") }, placeholder = { Text("Örn. Fabrika, Ev, Market") }, singleLine = true)
'''
if ui_marker in s and 'Text("Kayıtlı yerler"' not in s:
    s = s.replace(ui_marker, ui_repl, 1)

save_place_marker = '''            Text("Hatırlatma yarıçapı", fontWeight = FontWeight.SemiBold)
'''
save_place_repl = '''            if (lat != null && lng != null) {
                OutlinedButton(
                    onClick = {
                        FullRepository.savePlace(db, label.ifBlank { "Konum" }, address, lat!!, lng!!, radius)
                        statusText = "${label.ifBlank { "Konum" }} kayıtlı yerlere eklendi"
                        revision++
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Icon(Icons.Rounded.BookmarkAdd, null); Spacer(Modifier.width(6.dp)); Text("Bu konumu sonraki görevler için kaydet") }
            }

            Text("Hatırlatma yarıçapı", fontWeight = FontWeight.SemiBold)
'''
if save_place_marker in s and 'sonraki görevler için kaydet' not in s:
    s = s.replace(save_place_marker, save_place_repl, 1)

p.write_text(s, encoding='utf-8')

# Rich task location manager refreshes after returning without destroying unsaved form state.
p = Path('app/src/main/java/com/emre/gunumplanner/RichTaskActivity.kt')
s = p.read_text(encoding='utf-8')
if 'rememberLauncherForActivityResult' not in s:
    s = s.replace('import androidx.activity.compose.setContent\n', 'import androidx.activity.compose.setContent\nimport androidx.activity.compose.rememberLauncherForActivityResult\nimport androidx.activity.result.contract.ActivityResultContracts\n')
if 'val placesLauncher = rememberLauncherForActivityResult' not in s:
    anchor = '    val selectedPlace = savedPlaces.firstOrNull { it.id == selectedPlaceId }\n'
    launcher = anchor + '    val placesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { placeRevision++ }\n'
    s = s.replace(anchor, launcher, 1)
s = s.replace('activity.startActivity(Intent(activity, SavedPlacesActivity::class.java)); placeRevision++', 'placesLauncher.launch(Intent(activity, SavedPlacesActivity::class.java))')
p.write_text(s, encoding='utf-8')

# Backup screen: keep restore flow simple and compilable.
p = Path('app/src/main/java/com/emre/gunumplanner/FullFeatureActivities.kt')
s = p.read_text(encoding='utf-8')
start = '    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())'
if start in s:
    a = s.index(start)
    b = s.index('    Scaffold(topBar=', a)
    replacement = '''    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){ uri ->
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
'''
    s = s[:a] + replacement + s[b:]
p.write_text(s, encoding='utf-8')
