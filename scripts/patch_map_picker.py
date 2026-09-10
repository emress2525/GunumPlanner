from pathlib import Path

p = Path('app/src/main/java/com/emre/gunumplanner/TaskExtrasActivity.kt')
s = p.read_text(encoding='utf-8')

if 'import android.app.Activity' not in s:
    s = s.replace('package com.emre.gunumplanner\n\n', 'package com.emre.gunumplanner\n\nimport android.app.Activity\n')

# The launcher must be declared after the location state variables so its result callback can update them.
anchor = '''    var locating by remember { mutableStateOf(false) }\n'''
launcher = '''    var locating by remember { mutableStateOf(false) }

    val mapPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val newLat = data?.getDoubleExtra("lat", Double.NaN) ?: Double.NaN
            val newLng = data?.getDoubleExtra("lng", Double.NaN) ?: Double.NaN
            if (!newLat.isNaN() && !newLng.isNaN()) {
                lat = newLat
                lng = newLng
                data?.getStringExtra("address")?.takeIf { it.isNotBlank() }?.let { address = it }
                data?.getStringExtra("label")?.takeIf { it.isNotBlank() }?.let { label = it }
                statusText = "Haritadan konum seçildi"
                revision++
            }
        }
    }
'''
if anchor in s and 'val mapPickerLauncher =' not in s:
    s = s.replace(anchor, launcher, 1)

old = '''                OutlinedButton(
                    onClick = {
                        if (address.isBlank()) { statusText = "Önce adres yaz"; return@OutlinedButton }
                        locating = true
                        activity.geocodeAddress(address) { pair ->
                            locating = false
                            if (pair == null) statusText = "Adres bulunamadı"
                            else { lat = pair.first; lng = pair.second; if (label.isBlank()) label = address.substringBefore(','); statusText = "Adres bulundu" }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !locating
                ) { Icon(Icons.Rounded.Search, null); Spacer(Modifier.width(6.dp)); Text("Adresi bul") }'''
new = '''                OutlinedButton(
                    onClick = {
                        val i = Intent(context, MapPickerActivity::class.java)
                            .putExtra("query", address)
                            .putExtra("has_coord", lat != null && lng != null)
                        lat?.let { i.putExtra("lat", it) }
                        lng?.let { i.putExtra("lng", it) }
                        mapPickerLauncher.launch(i)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !locating
                ) { Icon(Icons.Rounded.Map, null); Spacer(Modifier.width(6.dp)); Text("Haritada seç") }'''
if old in s:
    s = s.replace(old, new, 1)
elif 'Text("Haritada seç")' not in s:
    raise SystemExit('Map picker button target not found')

p.write_text(s, encoding='utf-8')
