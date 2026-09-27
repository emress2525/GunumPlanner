package com.vocalisolator.app

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.vocalisolator.app.cloud.CloudQuality
import com.vocalisolator.app.cloud.CloudResult
import com.vocalisolator.app.cloud.StemSplitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private sealed interface ScreenState {
    data object Empty : ScreenState
    data class Ready(val uri: Uri, val name: String) : ScreenState
    data class Working(val uri: Uri, val name: String, val progress: Float, val message: String) : ScreenState
    data class Results(val name: String, val result: CloudResult) : ScreenState
    data class Error(val previous: Ready?, val message: String) : ScreenState
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CloudVocalIsolator(this)
                }
            }
        }
    }
}

@Composable
private fun CloudVocalIsolator(activity: MainActivity) {
    val scope = rememberCoroutineScope()
    val browser = LocalUriHandler.current
    val prefs = remember { activity.getSharedPreferences("vocal_cloud", Context.MODE_PRIVATE) }

    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "").orEmpty()) }
    var apiDraft by remember { mutableStateOf(apiKey) }
    var quality by remember { mutableStateOf(CloudQuality.FAST) }
    var state by remember { mutableStateOf<ScreenState>(ScreenState.Empty) }
    var work by remember { mutableStateOf<Job?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var pendingSave by remember { mutableStateOf<File?>(null) }

    fun fileName(uri: Uri): String {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "media"
        activity.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) name = c.getString(i) ?: name
            }
        }
        return name
    }

    fun play(file: File) {
        runCatching {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { mp ->
                    mp.release()
                    if (player === mp) player = null
                }
                prepare()
                start()
            }
        }
    }

    fun share(file: File) {
        val uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            file,
        )
        activity.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "audio/mpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Paylaş",
            )
        )
    }

    fun saveTo(uri: Uri, file: File) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    activity.contentResolver.openOutputStream(uri, "w")?.use { out ->
                        file.inputStream().buffered().use { input ->
                            input.copyTo(out, 1024 * 1024)
                        }
                    } ?: error("Dosya yazılamadı")
                }
            }.onFailure {
                state = ScreenState.Error(null, "Dosya kaydedilemedi: ${it.message ?: "bilinmeyen hata"}")
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/mpeg")
    ) { uri ->
        val file = pendingSave
        pendingSave = null
        if (uri != null && file != null) saveTo(uri, file)
    }

    fun start(ready: ScreenState.Ready) {
        if (apiKey.isBlank()) {
            state = ScreenState.Error(ready, "Önce sunucu API anahtarını kaydet.")
            return
        }
        work?.cancel()
        work = scope.launch {
            state = ScreenState.Working(ready.uri, ready.name, 0f, "Başlatılıyor")
            runCatching {
                withContext(Dispatchers.IO) {
                    StemSplitClient(activity, apiKey).separate(ready.uri, quality) { progress, message ->
                        activity.runOnUiThread {
                            state = ScreenState.Working(
                                ready.uri,
                                ready.name,
                                progress,
                                message,
                            )
                        }
                    }
                }
            }.onSuccess { result ->
                state = ScreenState.Results(ready.name, result)
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) {
                    state = ready
                } else {
                    state = ScreenState.Error(
                        ready,
                        error.message ?: "Sunucu işlemi başarısız",
                    )
                }
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            work?.cancel()
            state = ScreenState.Ready(uri, fileName(uri))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            work?.cancel()
            player?.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Vocal Isolator Cloud",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Moises mantığı: telefon ağır AI hesabı yapmaz; dosya sunucuda ayrılır.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Sunucu bağlantısı", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = apiDraft,
                    onValueChange = { apiDraft = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("StemSplit API anahtarı") },
                    visualTransformation = PasswordVisualTransformation(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        apiKey = apiDraft.trim()
                        prefs.edit().putString("api_key", apiKey).apply()
                    }) {
                        Text(if (apiKey.isBlank()) "Anahtarı Kaydet" else "Güncelle")
                    }
                    OutlinedButton(onClick = {
                        browser.openUri("https://stemsplit.io/app/settings/api")
                    }) {
                        Text("Ücretsiz Anahtar Al")
                    }
                }
                if (apiKey.isNotBlank()) {
                    Text("✓ Sunucu anahtarı kayıtlı", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text("Kalite / hız")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = quality == CloudQuality.FAST,
                onClick = { quality = CloudQuality.FAST },
                label = { Text("Hızlı") },
            )
            FilterChip(
                selected = quality == CloudQuality.BALANCED,
                onClick = { quality = CloudQuality.BALANCED },
                label = { Text("Dengeli") },
            )
            FilterChip(
                selected = quality == CloudQuality.BEST,
                onClick = { quality = CloudQuality.BEST },
                label = { Text("En İyi") },
            )
        }

        Button(
            onClick = { picker.launch(arrayOf("audio/*", "video/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Video / Müzik Seç")
        }

        when (val current = state) {
            ScreenState.Empty -> {
                Text("MP3, WAV, M4A ve medya dosyalarını seçebilirsin.")
            }

            is ScreenState.Ready -> {
                MediaCard(current.name)
                Button(
                    onClick = { start(current) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Vokali Ayır")
                }
            }

            is ScreenState.Working -> {
                MediaCard(current.name)
                Text(current.message)
                LinearProgressIndicator(
                    progress = { current.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("%${(current.progress * 100).toInt()}")
                OutlinedButton(
                    onClick = {
                        work?.cancel()
                        state = ScreenState.Ready(current.uri, current.name)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("İptal")
                }
            }

            is ScreenState.Results -> {
                MediaCard(current.name)
                StemCard(
                    title = "🎤 Sadece Vokal",
                    file = current.result.vocals,
                    onPlay = { play(it) },
                    onSave = {
                        pendingSave = it
                        saveLauncher.launch("${current.name.substringBeforeLast('.')}_vocals.mp3")
                    },
                    onShare = { share(it) },
                )
                StemCard(
                    title = "🎵 Enstrümantal",
                    file = current.result.instrumental,
                    onPlay = { play(it) },
                    onSave = {
                        pendingSave = it
                        saveLauncher.launch("${current.name.substringBeforeLast('.')}_instrumental.mp3")
                    },
                    onShare = { share(it) },
                )
                OutlinedButton(
                    onClick = { state = ScreenState.Empty },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Yeni Dosya")
                }
            }

            is ScreenState.Error -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Hata", fontWeight = FontWeight.Bold)
                        Text(current.message)
                        Button(onClick = {
                            state = current.previous ?: ScreenState.Empty
                        }) {
                            Text("Tamam")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Bu sürümde ayırma işlemi sunucuda yapılır. Seçtiğin dosya ayırma için StemSplit hizmetine yüklenir.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun MediaCard(name: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(name, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StemCard(
    title: String,
    file: File,
    onPlay: (File) -> Unit,
    onSave: (File) -> Unit,
    onShare: (File) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Text("${file.length() / 1024 / 1024} MB • MP3")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onPlay(file) }) { Text("Dinle") }
                OutlinedButton(onClick = { onSave(file) }) { Text("Kaydet") }
                OutlinedButton(onClick = { onShare(file) }) { Text("Paylaş") }
            }
        }
    }
}
