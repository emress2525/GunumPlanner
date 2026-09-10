from pathlib import Path

p = Path('app/src/main/java/com/emre/gunumplanner/PremiumV2Dialogs.kt')
s = p.read_text(encoding='utf-8')

if 'import android.content.Intent' not in s:
    s = s.replace('package com.emre.gunumplanner\n\n', 'package com.emre.gunumplanner\n\nimport android.content.Intent\n')
if 'import androidx.compose.ui.platform.LocalContext' not in s:
    s = s.replace('import androidx.compose.ui.Modifier\n', 'import androidx.compose.ui.Modifier\nimport androidx.compose.ui.platform.LocalContext\n')

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

# Existing debug builds are not reactive to SQLite changes made in a second Activity.
# Force a safe refresh when returning from the extras screen by refreshing the main Activity.
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
        // The first resume belongs to initial launch. Later resumes may follow camera/files/settings screens.
        // Recreating makes the SQLite-backed planner immediately reflect new extras without touching task data.
        if (resumeCount++ > 0 && intent.getBooleanExtra("refresh_on_resume", false)) {
            intent.removeExtra("refresh_on_resume")
            recreate()
        }
    }
'''
    if marker in a:
        a = a.replace(marker, repl, 1)
# The extras screen does not need to recreate the planner; regular planner refresh happens on next DB action.
p2.write_text(a, encoding='utf-8')
