from pathlib import Path

p = Path('app/src/main/java/com/emre/gunumplanner/MapPickerActivity.kt')
s = p.read_text(encoding='utf-8')

if 'import androidx.compose.ui.draw.clipToBounds' not in s:
    s = s.replace('import androidx.compose.ui.Modifier\n', 'import androidx.compose.ui.Modifier\nimport androidx.compose.ui.draw.clipToBounds\n')

old_row = '''                        Row(\n                            Modifier.fillMaxWidth().padding(horizontal = 12.dp),\n                            horizontalArrangement = Arrangement.spacedBy(8.dp)\n                        ) {'''
new_row = '''                        Row(\n                            Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 12.dp, vertical = 4.dp),\n                            horizontalArrangement = Arrangement.spacedBy(8.dp)\n                        ) {'''
if old_row in s:
    s = s.replace(old_row, new_row, 1)

old_map = 'modifier = Modifier.fillMaxWidth().weight(1f),'
new_map = 'modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds(),'
if old_map in s:
    s = s.replace(old_map, new_map, 1)
elif '.clipToBounds()' not in s:
    raise SystemExit('AndroidView modifier target not found')

p.write_text(s, encoding='utf-8')
