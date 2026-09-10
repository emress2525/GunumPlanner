from pathlib import Path

# Inject premium-v3 extras into the already-tested premium-v2 UI without replacing its planner code.
p = Path('app/src/main/java/com/emre/gunumplanner/PremiumV2Dialogs.kt')
s = p.read_text(encoding='utf-8')

if 'import android.content.Intent' not in s:
    s = s.replace('package com.emre.gunumplanner\n\n', 'package com.emre.gunumplanner\n\nimport android.content.Intent\n')
if 'import androidx.compose.ui.platform.LocalContext' not in s:
    s = s.replace('import androidx.compose.ui.Modifier\n', 'import androidx.compose.ui.Modifier\nimport androidx.compose.ui.platform.LocalContext\n')

# Direct "photo/location task" entry in the add sheet.
add_head = '''fun V2AddSheet(
    close: () -> Unit,
    quick: () -> Unit,
    task: () -> Unit,
    note: () -> Unit,
    voiceTask: () -> Unit,
    voiceNote: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = close) {'''
add_repl = '''fun V2AddSheet(
    close: () -> Unit,
    quick: () -> Unit,
    task: () -> Unit,
    note: () -> Unit,
    voiceTask: () -> Unit,
    voiceNote: () -> Unit
) {
    val addContext = LocalContext.current
    ModalBottomSheet(onDismissRequest = close) {'''
if add_head in s and 'val addContext = LocalContext.current' not in s:
    s = s.replace(add_head, add_repl, 1)

add_line = '            V2AddOption(Icons.Rounded.Today, "Ayrıntılı görev", "Tarih, saat, süre, tekrar ve öncelik", task)\n'
rich_line = add_line + '''            V2AddOption(Icons.Rounded.AddAPhoto, "Fotoğraflı / konumlu görev", "Kamera, galeri, PDF, dosya ve konum hatırlatması") {
                addContext.startActivity(Intent(addContext, RichTaskActivity::class.java))
                close()
            }
'''
if add_line in s and 'Fotoğraflı / konumlu görev' not in s:
    s = s.replace(add_line, rich_line, 1)

# Existing task/note details: one tap opens all attachments and location options.
needle = '''    delete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = close) {'''
replacement = '''    delete: () -> Unit
) {
    val extrasContext = LocalContext.current
    ModalBottomSheet(onDismissRequest = close) {'''
if needle in s and 'val extrasContext = LocalContext.current' not in s:
    s = s.replace(needle, replacement, 1)

needle2 = '''            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = edit, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Rounded.Edit, null); Spacer(Modifier.width(8.dp)); Text("Düzenle")
            }'''
replacement2 = '''            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    extrasContext.startActivity(
                        Intent(extrasContext, TaskExtrasActivity::class.java)
                            .putExtra("item_id", item.id)
                    )
                    close()
                },
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.AttachFile, null); Spacer(Modifier.width(8.dp)); Text("Fotoğraf • Dosya • Konum")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = edit, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Rounded.Edit, null); Spacer(Modifier.width(8.dp)); Text("Düzenle")
            }'''
if needle2 in s and 'Fotoğraf • Dosya • Konum' not in s:
    s = s.replace(needle2, replacement2, 1)

p.write_text(s, encoding='utf-8')

# Main activity: refresh SQLite-backed lists when returning from camera/files/settings/rich-task screens,
# and keep geofences in sync with complete/reopen actions.
p2 = Path('app/src/main/java/com/emre/gunumplanner/PremiumV2Activity.kt')
a = p2.read_text(encoding='utf-8')
if 'private var resumeCount = 0' not in a:
    a = a.replace('    lateinit var db: Db\n', '    lateinit var db: Db\n    private var resumeCount = 0\n')
if 'override fun onResume()' not in a:
    marker = '''    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Db(this)
        askNotifications()
        setContent { V2Theme { V2PlannerApp(this, db) } }
    }
'''
    repl = marker + '''\n    override fun onResume() {
        super.onResume()
        if (resumeCount++ > 0) recreate()
    }
'''
    if marker in a:
        a = a.replace(marker, repl, 1)

old_reopen = '''            db.reopenItem(item.id)
            scheduleIfFuture(db.getItem(item.id))'''
new_reopen = '''            db.reopenItem(item.id)
            ExtrasRepository.getLocation(db, item.id)?.takeIf { it.enabled }?.let { LocationReminderManager.schedule(activity, it) }
            scheduleIfFuture(db.getItem(item.id))'''
if old_reopen in a and 'ExtrasRepository.getLocation(db, item.id)?.takeIf' not in a:
    a = a.replace(old_reopen, new_reopen, 1)

old_complete = '''            ReminderScheduler.cancel(activity, item.id)
            val nextId = db.completeItem(item.id)'''
new_complete = '''            ReminderScheduler.cancel(activity, item.id)
            LocationReminderManager.cancel(activity, item.id)
            val nextId = db.completeItem(item.id)'''
if old_complete in a and 'LocationReminderManager.cancel(activity, item.id)' not in a:
    a = a.replace(old_complete, new_complete, 1)

p2.write_text(a, encoding='utf-8')
