package com.emre.gunumplanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.util.Locale

class MapPickerActivity : ComponentActivity() {
    data class SearchHit(val label: String, val address: String, val lat: Double, val lng: Double)

    private var nativeMap: MapView? = null

    override fun onResume() {
        super.onResume()
        nativeMap?.onResume()
    }

    override fun onPause() {
        nativeMap?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        nativeMap?.onDetach()
        nativeMap = null
        super.onDestroy()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )

        val db = Db(this)
        FullRepository.ensureSchema(db)
        val startQuery = intent.getStringExtra("query").orEmpty()
        val hasCoord = intent.getBooleanExtra("has_coord", false)
        val startLat = if (hasCoord) intent.getDoubleExtra("lat", 39.0) else 39.0
        val startLng = if (hasCoord) intent.getDoubleExtra("lng", 35.0) else 35.0
        val startRadius = intent.getFloatExtra("radius", 200f)

        setContent {
            V2Theme {
                var query by remember { mutableStateOf(startQuery) }
                var selectedLat by remember { mutableStateOf<Double?>(if (hasCoord) startLat else null) }
                var selectedLng by remember { mutableStateOf<Double?>(if (hasCoord) startLng else null) }
                var selectedAddress by remember { mutableStateOf(startQuery) }
                var selectedLabel by remember { mutableStateOf(startQuery.substringBefore(',').trim()) }
                var radius by remember { mutableFloatStateOf(startRadius) }
                var status by remember {
                    mutableStateOf(
                        if (hasCoord) "Konum hazır • pini sürükleyebilir veya haritada başka yere dokunabilirsin"
                        else "Adres ara veya haritada bir noktaya dokun"
                    )
                }
                var searching by remember { mutableStateOf(false) }
                var searchHits by remember { mutableStateOf<List<SearchHit>>(emptyList()) }
                var mapView by remember { mutableStateOf<MapView?>(null) }
                var marker by remember { mutableStateOf<Marker?>(null) }
                var radiusCircle by remember { mutableStateOf<Polygon?>(null) }
                val savedPlaces = remember { FullRepository.savedPlaces(db) }

                fun drawSelection(centerCamera: Boolean = false) {
                    val map = mapView ?: return
                    val la = selectedLat ?: return
                    val lo = selectedLng ?: return
                    val center = GeoPoint(la, lo)

                    val activeMarker = marker ?: Marker(map).also { m ->
                        m.isDraggable = true
                        m.title = "Seçilen konum"
                        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        m.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                            override fun onMarkerDragStart(marker: Marker) = Unit

                            override fun onMarkerDrag(marker: Marker) {
                                val circle = radiusCircle
                                if (circle != null) {
                                    circle.setPoints(Polygon.pointsAsCircle(marker.position, radius.toDouble()))
                                    map.invalidate()
                                }
                            }

                            override fun onMarkerDragEnd(marker: Marker) {
                                val p = marker.position
                                selectedLat = p.latitude
                                selectedLng = p.longitude
                                val circle = radiusCircle
                                if (circle != null) {
                                    circle.setPoints(Polygon.pointsAsCircle(p, radius.toDouble()))
                                }
                                map.invalidate()
                                reverseGeocode(p.latitude, p.longitude)
                            }
                        })
                        map.overlays.add(m)
                        marker = m
                    }
                    activeMarker.position = center

                    val activeCircle = radiusCircle ?: Polygon(map).also { p ->
                        p.fillPaint.color = Color.argb(35, 91, 95, 239)
                        p.outlinePaint.color = Color.rgb(91, 95, 239)
                        p.outlinePaint.strokeWidth = 4f
                        map.overlays.add(0, p)
                        radiusCircle = p
                    }
                    activeCircle.setPoints(Polygon.pointsAsCircle(center, radius.toDouble()))

                    if (centerCamera) {
                        map.controller.setZoom(16.5)
                        map.controller.animateTo(center)
                    }
                    map.invalidate()
                }

                fun moveMap(lat: Double, lng: Double, address: String = "", label: String = "") {
                    selectedLat = lat
                    selectedLng = lng
                    if (address.isNotBlank()) {
                        selectedAddress = address
                        query = address
                    }
                    if (label.isNotBlank()) selectedLabel = label
                    drawSelection(centerCamera = true)
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
                            status = "Konum seçildi • pini sürükleyerek ince ayar yapabilirsin"
                        }
                    }.start()
                }

                fun choose(hit: SearchHit) {
                    moveMap(hit.lat, hit.lng, hit.address, hit.label)
                    searchHits = emptyList()
                    status = "Konum seçildi • yarıçapı kontrol et"
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
                        val list = runCatching {
                            @Suppress("DEPRECATION")
                            Geocoder(this@MapPickerActivity, Locale("tr", "TR"))
                                .getFromLocationName(text, 5)
                                .orEmpty()
                        }.getOrDefault(emptyList())
                        val hits = list.map { a: Address ->
                            val ad = a.getAddressLine(0).orEmpty().ifBlank { text }
                            SearchHit(
                                a.featureName?.takeIf { it.isNotBlank() } ?: text.substringBefore(','),
                                ad,
                                a.latitude,
                                a.longitude
                            )
                        }
                        runOnUiThread {
                            searching = false
                            searchHits = hits
                            when {
                                hits.isEmpty() -> status = "Adres bulunamadı. Daha açık yaz veya haritadan seç."
                                hits.size == 1 -> choose(hits.first())
                                else -> status = "${hits.size} sonuç bulundu • doğru olanı seç"
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
                                    Text("Ara, pini taşı, yarıçapı gör", style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            navigationIcon = {
                                IconButton({ finish() }) { Icon(Icons.Rounded.ArrowBack, "Geri") }
                            }
                        )
                    },
                    bottomBar = {
                        Surface(tonalElevation = 4.dp) {
                            Column(
                                Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(listOf(100f, 200f, 500f, 1000f)) { m ->
                                        FilterChip(
                                            selected = radius == m,
                                            onClick = {
                                                radius = m
                                                drawSelection(centerCamera = false)
                                            },
                                            label = { Text(if (m >= 1000) "1 km" else "${m.toInt()} m") }
                                        )
                                    }
                                }
                                Text(
                                    status,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        val la = selectedLat ?: return@Button
                                        val lo = selectedLng ?: return@Button
                                        setResult(
                                            Activity.RESULT_OK,
                                            Intent()
                                                .putExtra("lat", la)
                                                .putExtra("lng", lo)
                                                .putExtra("address", selectedAddress)
                                                .putExtra("label", selectedLabel)
                                                .putExtra("radius", radius)
                                        )
                                        finish()
                                    },
                                    enabled = selectedLat != null && selectedLng != null,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp)
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                query,
                                { query = it },
                                Modifier.weight(1f),
                                label = { Text("Adres / yer adı") },
                                placeholder = { Text("Örn. Anıtkabir, Ankara") },
                                singleLine = true
                            )
                            FilledIconButton(
                                { searchAddress() },
                                enabled = !searching,
                                modifier = Modifier.size(56.dp)
                            ) { Icon(Icons.Rounded.Search, "Ara") }
                        }

                        if (savedPlaces.isNotEmpty()) {
                            LazyRow(
                                Modifier.padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(savedPlaces, key = { it.id }) { p ->
                                    AssistChip(
                                        onClick = {
                                            radius = p.radius
                                            moveMap(p.lat, p.lng, p.address, p.name)
                                            status = "${p.name} seçildi"
                                        },
                                        label = { Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        leadingIcon = { Icon(Icons.Rounded.Place, null, Modifier.size(18.dp)) }
                                    )
                                }
                            }
                        }

                        if (searchHits.isNotEmpty()) {
                            LazyRow(
                                Modifier.padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(searchHits) { h ->
                                    Surface(
                                        onClick = { choose(h) },
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text(h.label, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(h.address, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }

                        AndroidView(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            factory = { context ->
                                MapView(context).apply {
                                    setTileSource(TileSourceFactory.MAPNIK)
                                    setMultiTouchControls(true)
                                    setBuiltInZoomControls(false)
                                    controller.setZoom(if (hasCoord) 16.5 else 6.0)
                                    controller.setCenter(GeoPoint(startLat, startLng))

                                    val events = MapEventsOverlay(object : MapEventsReceiver {
                                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                            selectedLat = p.latitude
                                            selectedLng = p.longitude
                                            drawSelection(centerCamera = false)
                                            reverseGeocode(p.latitude, p.longitude)
                                            return true
                                        }

                                        override fun longPressHelper(p: GeoPoint): Boolean {
                                            selectedLat = p.latitude
                                            selectedLng = p.longitude
                                            drawSelection(centerCamera = false)
                                            reverseGeocode(p.latitude, p.longitude)
                                            return true
                                        }
                                    })
                                    overlays.add(events)
                                    mapView = this
                                    nativeMap = this
                                    if (hasCoord) drawSelection(centerCamera = false)
                                }
                            },
                            update = { map ->
                                mapView = map
                                nativeMap = map
                                if (selectedLat != null && selectedLng != null) {
                                    drawSelection(centerCamera = false)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
