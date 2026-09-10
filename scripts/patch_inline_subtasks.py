from pathlib import Path

p = Path('app/src/main/java/com/emre/gunumplanner/PremiumV2Planner.kt')
s = p.read_text(encoding='utf-8')

marker = '@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nprivate fun V2SwipeTaskRow('

if 'private fun V2TaskWithSubtasks(' not in s:
    if marker not in s:
        raise SystemExit('V2SwipeTaskRow marker not found')

    head, tail = s.split(marker, 1)
    old_call = '                V2SwipeTaskRow(\n                    item = item,'
    new_call = '                V2TaskWithSubtasks(\n                    item = item,'
    count = head.count(old_call)
    if count < 2:
        raise SystemExit(f'Expected at least 2 task row calls, found {count}')
    head = head.replace(old_call, new_call, 2)

    wrapper = r'''
@Composable
private fun V2TaskWithSubtasks(
    item: Db.Item,
    conflict: Boolean,
    onOpen: (Db.Item) -> Unit,
    onComplete: (Db.Item) -> Unit,
    onPostpone: (Db.Item) -> Unit,
    onMoveMinutes: (Db.Item, Int) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val db = remember(context) { Db(context) }
    var localRevision by remember(item.id) { mutableIntStateOf(0) }
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, item.id) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) localRevision++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val subtasks = remember(item.id, localRevision) {
        FullRepository.subtasks(db, item.id)
    }
    val doneCount = subtasks.count { it.done }
    val allDone = subtasks.isNotEmpty() && doneCount == subtasks.size

    Column(Modifier.fillMaxWidth().animateContentSize()) {
        V2SwipeTaskRow(
            item = item,
            conflict = conflict,
            onOpen = onOpen,
            onComplete = onComplete,
            onPostpone = onPostpone,
            onMoveMinutes = onMoveMinutes
        )

        if (subtasks.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Surface(
                onClick = { expanded = !expanded },
                shape = RoundedCornerShape(16.dp),
                color = if (allDone)
                    Color(0xFF173D2E).copy(alpha = if (MaterialTheme.colorScheme.background.luminance() < .5f) .72f else .12f)
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = .62f),
                modifier = Modifier.fillMaxWidth().padding(start = 44.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (allDone) Icons.Rounded.TaskAlt else Icons.Rounded.Checklist,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (allDone) Color(0xFF43A875) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$doneCount/${subtasks.size} alt görev",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (allDone) {
                        Text(
                            "Tamamlandı",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF43A875),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Icon(
                        if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "Alt görevleri kapat" else "Alt görevleri aç",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    Modifier.fillMaxWidth().padding(start = 44.dp, top = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subtasks.take(6).forEach { sub ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = sub.done,
                                    onCheckedChange = { checked ->
                                        FullRepository.toggleSubtask(db, sub.id, checked)
                                        localRevision++
                                    }
                                )
                                Text(
                                    sub.title,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (sub.done)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (subtasks.size > 6) {
                        Text(
                            "+${subtasks.size - 6} alt görev daha",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    TextButton(
                        onClick = {
                            context.startActivity(
                                android.content.Intent(context, ChecklistActivity::class.java)
                                    .putExtra("item_id", item.id)
                            )
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Rounded.Edit, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Alt görevleri yönet")
                    }
                }
            }
        }
    }
}

'''
    s = head + wrapper + marker + tail

# Bump visible app version for this build.
gradle = Path('app/build.gradle')
g = gradle.read_text(encoding='utf-8')
g = g.replace("versionCode 21", "versionCode 22")
g = g.replace("versionName '1.0.1-full-mapfix'", "versionName '1.0.2-inline-subtasks'")
gradle.write_text(g, encoding='utf-8')

p.write_text(s, encoding='utf-8')
print('Inline subtasks patch applied')
