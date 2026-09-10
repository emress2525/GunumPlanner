package com.emre.gunumplanner

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.io.File
import java.util.Locale

class TaskExtrasActivity : ComponentActivity() {
    lateinit var db: Db
    var itemId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Db(this)
        ExtrasRepository.ensureSchema(db)
        itemId = intent.getLongExtra("item_id", 0L)
        if (itemId <= 0 || db.getItem(itemId) == null) {
            finish()
            return
        }
        setContent { V2Theme { ExtrasScreen(this, db, itemId) } }
    }

    fun getCurrentLocation(callback: (android.location.Location?) -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback(null)
            return
        }
        val token = CancellationTokenSource()
        LocationServices.getFusedLocationProviderClient(this)
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
            .addOnSuccessListener { callback(it) }
            .addOnFailureListener { callback(null) }
    }

    @Suppress("DEPRECATION")
    fun geocodeAddress(text: String, callback: (Pair<Double, Double>?) -> Unit) {
        Thread {
            val result = runCatching {
                if (!Geocoder.isPresent()) return@runCatching null
                val list = Geocoder(this, Locale("tr", "TR")).getFromLocationName(text, 1)
                val first = list?.firstOrNull() ?: return@runCatching null
                first.latitude to first.longitude
            }.getOrNull()
            runOnUiThread { callback(result) }
        }.start()
    }

    @Suppress("DEPRECATION")
    fun reverseGeocode(lat: Double, lng: Double, callback: (String) -> Unit) {
        Thread {
            val result = runCatching {
                val list = Geocoder(this, Locale("tr", "TR")).getFromLocation(lat, lng, 1)
                list?.firstOrNull()?.getAddressLine(0).orEmpty()
            }.getOrDefault("")
            runOnUiThread { callback(result) }
        }.start()
    }

    fun openAppLocationSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtrasScreen(activity: TaskExtrasActivity, db: Db, itemId: Long) {
    val item = db.getItem(itemId) ?: return
    var revision by remember { mutableIntStateOf(0) }
    var statusText by remember { mutableStateOf("") }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var foregroundGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    var backgroundGranted by remember { mutableStateOf(LocationReminderManager.hasBackgroundPermission(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                foregroundGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                backgroundGranted = LocationReminderManager.hasBackgroundPermission(context)
                if (foregroundGranted && backgroundGranted) {
                    ExtrasRepository.getLocation(db, itemId)?.takeIf { it.enabled }?.let { LocationReminderManager.schedule(context, it) }
                }
                revision++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        foregroundGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!foregroundGranted) statusText = "Konum izni verilmedi"
        revision++
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = cameraFile
        if (ok && file != null && file.exists() && file.length() > 0) {
            ExtrasRepository.addAttachment(db, itemId, ExtrasRepository.cameraPending(file))
            statusText = "Fotoğraf göreve eklendi"
            revision++
        } else {
            file?.delete()
        }
        cameraFile = null
    }

    val imagesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        var added = 0
        uris.forEach { uri ->
            ExtrasRepository.copyUriToPrivateStorage(context, uri, "PHOTO")?.let {
                ExtrasRepository.addAttachment(db, itemId, it)
                added++
            }
        }
        statusText = if (added > 0) "$added fotoğraf eklendi" else "Fotoğraf eklenemedi"
        revision++
    }

    val filesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        var added = 0
        uris.forEach { uri ->
            val mime = context.contentResolver.getType(uri).orEmpty()
            val kind = if (mime.startsWith("image/")) "PHOTO" else if (mime == "application/pdf") "PDF" else "FILE"
            ExtrasRepository.copyUriToPrivateStorage(context, uri, kind)?.let {
                ExtrasRepository.addAttachment(db, itemId, it)
                added++
            }
        }
        statusText = if (added > 0) "$added dosya eklendi" else "Dosya eklenemedi"
        revision++
    }

    val attachments = remember(revision) { ExtrasRepository.getAttachments(db, itemId) }
    val storedLocation = remember(revision) { ExtrasRepository.getLocation(db, itemId) }

    var label by remember(storedLocation?.updatedAt) { mutableStateOf(storedLocation?.label.orEmpty()) }
    var address by remember(storedLocation?.updatedAt) { mutableStateOf(storedLocation?.address.orEmpty()) }
    var lat by remember(storedLocation?.updatedAt) { mutableStateOf(storedLocation?.lat) }
    var lng by remember(storedLocation?.updatedAt) { mutableStateOf(storedLocation?.lng) }
    var radius by remember(storedLocation?.updatedAt) { mutableFloatStateOf(storedLocation?.radius ?: 200f) }
    var transition by remember(storedLocation?.updatedAt) { mutableStateOf(storedLocation?.transition ?: "ENTER") }
    var enabled by remember(storedLocation?.updatedAt) { mutableStateOf(storedLocation?.enabled ?: true) }
    var locating by remember { mutableStateOf(false) }

    fun useCurrentLocation() {
        if (!foregroundGranted) {
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            statusText = "Konum iznini verip tekrar 'Mevcut konum' düğmesine bas"
            return
        }
        locating = true
        activity.getCurrentLocation { loc ->
            locating = false
            if (loc == null) {
                statusText = "Konum alınamadı. GPS'i açıp tekrar dene."
            } else {
                lat = loc.latitude
                lng = loc.longitude
                if (label.isBlank()) label = "Mevcut konum"
                activity.reverseGeocode(loc.latitude, loc.longitude) { resolved ->
                    if (resolved.isNotBlank()) address = resolved
                }
                statusText = "Konum alındı"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Ekler ve konum", fontWeight = FontWeight.Bold)
                        Text(item.title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = { IconButton(onClick = { activity.finish() }) { Icon(Icons.Rounded.ArrowBack, "Geri") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Görevi zenginleştir", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Fotoğraf, PDF, dosya ve konum hatırlatması aynı görevde tutulur.", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .78f))
                }
            }

            Text("Fotoğraf ve dosyalar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                        val file = ExtrasRepository.cameraFile(context)
                        cameraFile = file
                        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
                        cameraLauncher.launch(uri)
                    },
                    modifier = Modifier.weight(1f)
                ) { Icon(Icons.Rounded.PhotoCamera, null); Spacer(Modifier.width(6.dp)); Text("Kamera") }
                FilledTonalButton(onClick = { imagesLauncher.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.PhotoLibrary, null); Spacer(Modifier.width(6.dp)); Text("Galeri")
                }
            }
            OutlinedButton(onClick = { filesLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.AttachFile, null); Spacer(Modifier.width(8.dp)); Text("PDF / dosya ekle")
            }

            if (attachments.isEmpty()) {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)) {
                    Text("Henüz ek yok. Bir görevde istediğin kadar fotoğraf ve dosya tutabilirsin.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                attachments.forEach { att -> AttachmentRow(att, onOpen = { openAttachment(context, att) }, onDelete = {
                    ExtrasRepository.deleteAttachment(db, att)
                    statusText = "Ek silindi"
                    revision++
                }) }
            }

            HorizontalDivider()
            Text("Konum hatırlatması", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Bir yere geldiğinde veya oradan çıktığında görev sana bildirim olarak gelsin.", color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(label, { label = it }, Modifier.fillMaxWidth(), label = { Text("Konum adı") }, placeholder = { Text("Örn. Fabrika, Ev, Market") }, singleLine = true)
            OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(), label = { Text("Adres") }, placeholder = { Text("Adres yaz veya mevcut konumu kullan") }, minLines = 2)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { useCurrentLocation() }, modifier = Modifier.weight(1f), enabled = !locating) {
                    Icon(Icons.Rounded.MyLocation, null); Spacer(Modifier.width(6.dp)); Text(if (locating) "Bulunuyor…" else "Mevcut konum")
                }
                OutlinedButton(
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
                ) { Icon(Icons.Rounded.Search, null); Spacer(Modifier.width(6.dp)); Text("Adresi bul") }
            }

            if (lat != null && lng != null) {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Place, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Konum hazır", fontWeight = FontWeight.SemiBold)
                            Text(String.format(Locale.US, "%.5f, %.5f", lat, lng), style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { openMap(context, lat!!, lng!!, label.ifBlank { address }) }) { Icon(Icons.Rounded.Map, "Haritada aç") }
                    }
                }
            }

            Text("Hatırlatma yarıçapı", fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(100f, 200f, 500f, 1000f)) { value ->
                    FilterChip(selected = radius == value, onClick = { radius = value }, label = { Text(if (value >= 1000) "1 km" else "${value.toInt()} m") })
                }
            }

            Text("Ne zaman?", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = transition == "ENTER", onClick = { transition = "ENTER" }, label = { Text("Gelince") }, leadingIcon = { Icon(Icons.Rounded.Login, null, Modifier.size(18.dp)) })
                FilterChip(selected = transition == "EXIT", onClick = { transition = "EXIT" }, label = { Text("Çıkınca") }, leadingIcon = { Icon(Icons.Rounded.Logout, null, Modifier.size(18.dp)) })
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Konum hatırlatması", fontWeight = FontWeight.SemiBold)
                    Text(if (enabled) "Açık" else "Kapalı", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }

            if (enabled && !backgroundGranted && Build.VERSION.SDK_INT >= 29) {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Arka planda konum izni gerekiyor", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Uygulama kapalıyken de konuma geldiğini anlayabilmesi için Günüm > İzinler > Konum bölümünde “Her zaman izin ver” seçeneğini aç.", color = MaterialTheme.colorScheme.onErrorContainer)
                        Button(onClick = { activity.openAppLocationSettings() }) { Icon(Icons.Rounded.Settings, null); Spacer(Modifier.width(6.dp)); Text("İzin ayarlarını aç") }
                    }
                }
            }

            Button(
                onClick = {
                    val la = lat
                    val lo = lng
                    if (la == null || lo == null) { statusText = "Önce bir konum seç"; return@Button }
                    val rule = ExtrasRepository.LocationRule(
                        itemId = itemId,
                        label = label.trim(),
                        address = address.trim(),
                        lat = la,
                        lng = lo,
                        radius = radius,
                        transition = transition,
                        enabled = enabled,
                        createdAt = storedLocation?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    ExtrasRepository.saveLocation(db, rule)
                    LocationReminderManager.cancel(context, itemId)
                    val scheduled = if (enabled) LocationReminderManager.schedule(context, rule) else true
                    statusText = when {
                        !enabled -> "Konum hatırlatması kapatıldı"
                        scheduled -> "Konum hatırlatması kaydedildi"
                        else -> "Konum kaydedildi; arka plan konum iznini açınca otomatik etkinleşecek"
                    }
                    revision++
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) { Icon(Icons.Rounded.Save, null); Spacer(Modifier.width(8.dp)); Text("Konumu kaydet") }

            if (storedLocation != null) {
                TextButton(
                    onClick = {
                        LocationReminderManager.cancel(context, itemId)
                        ExtrasRepository.deleteLocation(db, itemId)
                        label = ""; address = ""; lat = null; lng = null; radius = 200f; transition = "ENTER"; enabled = true
                        statusText = "Konum hatırlatması kaldırıldı"
                        revision++
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Icon(Icons.Rounded.DeleteLocation, null); Spacer(Modifier.width(6.dp)); Text("Konumu kaldır") }
            }

            if (statusText.isNotBlank()) {
                Snackbar { Text(statusText) }
            }
        }
    }
}

@Composable
private fun AttachmentRow(att: ExtrasRepository.Attachment, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (att.mime.startsWith("image/")) {
                val bitmap = remember(att.path) { runCatching { BitmapFactory.decodeFile(att.path) }.getOrNull() }
                if (bitmap != null) {
                    Image(bitmap.asImageBitmap(), null, Modifier.size(64.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                } else {
                    AttachmentIcon(Icons.Rounded.BrokenImage)
                }
            } else if (att.mime == "application/pdf") AttachmentIcon(Icons.Rounded.PictureAsPdf)
            else AttachmentIcon(Icons.Rounded.InsertDriveFile)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(att.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (att.kind == "PHOTO") "Fotoğraf" else if (att.kind == "PDF") "PDF" else "Dosya", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, "Sil", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun AttachmentIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Icon(icon, null, Modifier.padding(15.dp).size(34.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

private fun openAttachment(context: android.content.Context, att: ExtrasRepository.Attachment) {
    val file = File(att.path)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, att.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try { context.startActivity(intent) } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(att.mime).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Dosyayı aç"))
    }
}

private fun openMap(context: android.content.Context, lat: Double, lng: Double, label: String) {
    val query = Uri.encode(label.ifBlank { "$lat,$lng" })
    val geo = Uri.parse("geo:$lat,$lng?q=$lat,$lng($query)")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, geo)) }
}
