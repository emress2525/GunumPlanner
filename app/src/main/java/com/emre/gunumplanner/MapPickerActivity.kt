package com.emre.gunumplanner

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale

class MapPickerActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startQuery = intent.getStringExtra("query").orEmpty()
        val hasCoord = intent.getBooleanExtra("has_coord", false)
        val startLat = if (hasCoord) intent.getDoubleExtra("lat", 39.0) else 39.0
        val startLng = if (hasCoord) intent.getDoubleExtra("lng", 35.0) else 35.0

        setContent {
            V2Theme {
                var query by remember { mutableStateOf(startQuery) }
                var selectedLat by remember { mutableStateOf<Double?>(if (hasCoord) startLat else null) }
                var selectedLng by remember { mutableStateOf<Double?>(if (hasCoord) startLng else null) }
                var selectedAddress by remember { mutableStateOf(startQuery) }
                var selectedLabel by remember { mutableStateOf(startQuery.substringBefore(',').trim()) }
                var status by remember { mutableStateOf(if (hasCoord) "Konum hazır • pini sürükleyebilir veya haritada başka yere dokunabilirsin" else "Adres ara veya haritada bir noktaya dokun") }
                var searching by remember { mutableStateOf(false) }
                var webView by remember { mutableStateOf<WebView?>(null) }
                var mapReady by remember { mutableStateOf(false) }

                fun moveMap(lat: Double, lng: Double) {
                    selectedLat = lat
                    selectedLng = lng
                    if (mapReady) {
                        webView?.evaluateJavascript("window.setSelection($lat,$lng);", null)
                    }
                }

                fun reverseGeocode(lat: Double, lng: Double) {
                    Thread {
                        val resolved = runCatching {
                            @Suppress("DEPRECATION")
                            Geocoder(this@MapPickerActivity, Locale("tr", "TR"))
                                .getFromLocation(lat, lng, 1)
                                ?.firstOrNull()
                                ?.getAddressLine(0)
                                .orEmpty()
                        }.getOrDefault("")
                        runOnUiThread {
                            if (resolved.isNotBlank()) {
                                selectedAddress = resolved
                                selectedLabel = resolved.substringBefore(',').trim()
                                query = resolved
                            }
                            status = "Konum seçildi • istersen pini sürükleyerek ince ayar yap"
                        }
                    }.start()
                }

                fun searchAddress() {
                    val text = query.trim()
                    if (text.isBlank()) {
                        status = "Önce adres veya yer adı yaz"
                        return
                    }
                    searching = true
                    status = "Adres aranıyor…"
                    Thread {
                        val found = runCatching {
                            @Suppress("DEPRECATION")
                            Geocoder(this@MapPickerActivity, Locale("tr", "TR"))
                                .getFromLocationName(text, 1)
                                ?.firstOrNull()
                        }.getOrNull()
                        runOnUiThread {
                            searching = false
                            if (found == null) {
                                status = "Adres bulunamadı. Daha açık bir adres yaz veya haritadan seç."
                            } else {
                                selectedAddress = found.getAddressLine(0).orEmpty().ifBlank { text }
                                selectedLabel = text.substringBefore(',').trim()
                                moveMap(found.latitude, found.longitude)
                                status = "Adres bulundu • konumu doğrulayıp ‘Bu konumu kullan’ de"
                            }
                        }
                    }.start()
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text("Haritada konum seç", fontWeight = FontWeight.Bold)
                                    Text("Adresi ara veya doğrudan haritaya dokun", style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) { Icon(Icons.Rounded.ArrowBack, "Geri") }
                            }
                        )
                    },
                    bottomBar = {
                        Surface(tonalElevation = 4.dp) {
                            Column(
                                Modifier.fillMaxWidth().navigationBarsPadding().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(
                                    onClick = {
                                        val lat = selectedLat ?: return@Button
                                        val lng = selectedLng ?: return@Button
                                        setResult(
                                            Activity.RESULT_OK,
                                            Intent()
                                                .putExtra("lat", lat)
                                                .putExtra("lng", lng)
                                                .putExtra("address", selectedAddress)
                                                .putExtra("label", selectedLabel)
                                        )
                                        finish()
                                    },
                                    enabled = selectedLat != null && selectedLng != null,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(Icons.Rounded.MyLocation, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Bu konumu kullan")
                                }
                            }
                        }
                    }
                ) { padding ->
                    Column(
                        Modifier.fillMaxSize().padding(padding),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Adres / yer adı") },
                                placeholder = { Text("Örn. Anıtkabir, Ankara") },
                                singleLine = true
                            )
                            FilledIconButton(
                                onClick = { searchAddress() },
                                enabled = !searching,
                                modifier = Modifier.size(56.dp)
                            ) { Icon(Icons.Rounded.Search, "Ara") }
                        }

                        AndroidView(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            factory = { context ->
                                WebView(context).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.setSupportZoom(true)
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false
                                    webViewClient = object : WebViewClient() {}
                                    addJavascriptInterface(object {
                                        @JavascriptInterface
                                        fun onMapTapped(lat: Double, lng: Double) {
                                            runOnUiThread {
                                                selectedLat = lat
                                                selectedLng = lng
                                                reverseGeocode(lat, lng)
                                            }
                                        }

                                        @JavascriptInterface
                                        fun onMapReady() {
                                            runOnUiThread {
                                                mapReady = true
                                                if (hasCoord) moveMap(startLat, startLng)
                                            }
                                        }
                                    }, "Android")
                                    webView = this
                                    loadDataWithBaseURL(
                                        "https://www.openstreetmap.org/",
                                        mapHtml(startLat, startLng, hasCoord),
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun mapHtml(lat: Double, lng: Double, hasCoord: Boolean): String {
        val zoom = if (hasCoord) 16 else 6
        return """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>
html, body, #map { width:100%; height:100%; margin:0; padding:0; background:#eef1f7; }
.leaflet-control-attribution { font-size:10px; }
</style>
</head>
<body>
<div id="map"></div>
<script>
const map = L.map('map', { zoomControl:true }).setView([$lat, $lng], $zoom);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
  maxZoom: 19,
  attribution: '&copy; OpenStreetMap contributors'
}).addTo(map);
let marker = null;
function setMarker(lat, lng) {
  if (marker === null) {
    marker = L.marker([lat,lng], {draggable:true}).addTo(map);
    marker.on('dragend', function(e) {
      const p = e.target.getLatLng();
      Android.onMapTapped(p.lat, p.lng);
    });
  } else {
    marker.setLatLng([lat,lng]);
  }
}
window.setSelection = function(lat, lng) {
  setMarker(lat,lng);
  map.setView([lat,lng], Math.max(map.getZoom(),16), {animate:true});
};
map.on('click', function(e) {
  setMarker(e.latlng.lat, e.latlng.lng);
  Android.onMapTapped(e.latlng.lat, e.latlng.lng);
});
${if (hasCoord) "setMarker($lat,$lng);" else ""}
setTimeout(function(){ Android.onMapReady(); }, 250);
</script>
</body>
</html>
""".trimIndent()
    }
}
