package com.vocalisolator.app

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.vocalisolator.app.media.AndroidMediaInspector
import com.vocalisolator.app.media.AndroidPcmNormalizer
import com.vocalisolator.app.media.AndroidVideoMuxer
import com.vocalisolator.app.model.AppError
import com.vocalisolator.app.model.MediaKind
import com.vocalisolator.app.model.ProcessingProgress
import com.vocalisolator.app.model.ProcessingStage
import com.vocalisolator.app.model.SelectedMedia
import com.vocalisolator.app.model.SeparationResult
import com.vocalisolator.app.output.OutputNaming
import com.vocalisolator.app.separation.AndroidModelProvisioner
import com.vocalisolator.app.separation.DefaultSeparationOrchestrator
import com.vocalisolator.app.separation.DemucsNativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private sealed interface ScreenState {
    data object Empty : ScreenState
    data class Ready(val media: SelectedMedia) : ScreenState
    data class Working(val media: SelectedMedia, val progress: ProcessingProgress) : ScreenState
    data class Results(
        val media: SelectedMedia,
        val separation: SeparationResult,
        val vocalVideo: File? = null,
        val videoBusy: Boolean = false,
        val videoProgress: Float = 0f,
    ) : ScreenState
    data class Error(val media: SelectedMedia?, val message: String) : ScreenState
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VocalIsolatorScreen(this)
                }
            }
        }
    }
}

@Composable
private fun VocalIsolatorScreen(activity: MainActivity) {
    val scope = rememberCoroutineScope()
    val inspector = remember { AndroidMediaInspector(activity) }
    val normalizer = remember { AndroidPcmNormalizer(activity) }
    val modelProvisioner = remember { AndroidModelProvisioner(activity) }
    val orchestrator = remember {
        DefaultSeparationOrchestrator(
            cacheDir = File(activity.cacheDir, "vocal-isolator"),
            normalizer = normalizer,
            modelProvisioner = modelProvisioner,
            engineFactory = { model -> DemucsNativeBridge { Result.success(model) } },
        )
    }
    val videoMuxer = remember { AndroidVideoMuxer(activity) }

    var state by remember { mutableStateOf<ScreenState>(ScreenState.Empty) }
    var workJob by remember { mutableStateOf<Job?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var pendingSave by remember { mutableStateOf<File?>(null) }

    fun errorText(error: Throwable): String = when (error) {
        is AppError.UnsupportedMedia -> "Desteklenmeyen dosya biçimi"
        is AppError.NoAudioTrack -> "Bu dosyada ses parçası yok"
        is AppError.CannotReadMedia -> "Dosya okunamadı"
        is AppError.DecoderFailure -> "Ses çözülemedi"
        is AppError.ModelLoadFailure -> "AI vokal modeli yüklenemedi"
        is AppError.InsufficientMemory -> "Bu işlem için yeterli bellek yok"
        is AppError.InsufficientStorage -> "Geçici işlem için yeterli depolama yok"
        is AppError.VideoMuxFailure -> "Sadece vokalli video oluşturulamadı"
        is AppError.Cancelled -> "İşlem iptal edildi"
        else -> "Vokal ayırma işlemi başarısız"
    }

    fun copyTo(uri: Uri, source: File) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    activity.contentResolver.openOutputStream(uri, "w")!!.use { out ->
                        source.inputStream().buffered().use { input -> input.copyTo(out, 1024 * 1024) }
                    }
                }
            }.onFailure {
                val media = when (val s = state) {
                    is ScreenState.Ready -> s.media
                    is ScreenState.Results -> s.media
                    is ScreenState.Working -> s.media
                    is ScreenState.Error -> s.media
                    ScreenState.Empty -> null
                }
                state = ScreenState.Error(media, "Dosya kaydedilemedi")
            }
        }
    }

    val saveAudio = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { uri ->
        val file = pendingSave
        pendingSave = null
        if (uri != null && file != null) copyTo(uri, file)
    }
    val saveVideo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { uri ->
        val file = pendingSave
        pendingSave = null
        if (uri != null && file != null) copyTo(uri, file)
    }

    fun share(file: File, mime: String) {
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(intent, "Paylaş"))
    }

    fun play(file: File) {
        runCatching {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { it.release(); if (player === it) player = null }
                prepare()
                start()
            }
        }
    }

    fun startSeparation(media: SelectedMedia) {
        workJob?.cancel()
        workJob = scope.launch {
            state = ScreenState.Working(media, ProcessingProgress(ProcessingStage.PREPARING, 0f, "Hazırlanıyor"))
            orchestrator.process(media) { progress ->
                activity.runOnUiThread { state = ScreenState.Working(media, progress) }
            }.onSuccess { result ->
                state = ScreenState.Results(media, result)
            }.onFailure { error ->
                state = if (error is AppError.Cancelled) ScreenState.Ready(media) else ScreenState.Error(media, errorText(error))
            }
        }
    }

    fun createVocalVideo(results: ScreenState.Results) {
        if (results.media.kind != MediaKind.VIDEO || results.videoBusy) return
        val dir = File(activity.cacheDir, "vocal-isolator-video").apply { mkdirs() }
        val output = File(dir, OutputNaming.vocalVideo(results.media.displayName))
        workJob?.cancel()
        workJob = scope.launch {
            state = results.copy(videoBusy = true, videoProgress = 0f)
            videoMuxer.createVocalOnlyVideo(results.media.uri, results.separation.vocalsWav, output) { p ->
                activity.runOnUiThread {
                    val current = state as? ScreenState.Results ?: return@runOnUiThread
                    state = current.copy(videoBusy = true, videoProgress = p)
                }
            }.onSuccess { file ->
                state = results.copy(vocalVideo = file, videoBusy = false, videoProgress = 1f)
            }.onFailure { error ->
                state = if (error is AppError.Cancelled) results else ScreenState.Error(results.media, errorText(error))
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            workJob?.cancel()
            scope.launch {
                inspector.inspect(uri).onSuccess {
                    orchestrator.cleanupCompletedSession()
                    state = ScreenState.Ready(it)
                }.onFailure { state = ScreenState.Error(null, errorText(it)) }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            workJob?.cancel()
            orchestrator.cancel()
            videoMuxer.cancel()
            player?.release()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Vocal Isolator", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Video veya müzikten şarkıcı sesini cihazında ayır.", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = { picker.launch(arrayOf("audio/*", "video/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text("Video / Müzik Seç")
        }

        when (val current = state) {
            ScreenState.Empty -> {
                Text("MP4, MKV, MOV, MP3, WAV ve M4A desteklenir.")
            }
            is ScreenState.Ready -> {
                MediaCard(current.media)
                Button(onClick = { startSeparation(current.media) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Vokali Ayır")
                }
            }
            is ScreenState.Working -> {
                MediaCard(current.media)
                Text(current.progress.message.ifBlank { "İşleniyor" })
                LinearProgressIndicator(progress = { current.progress.fraction }, modifier = Modifier.fillMaxWidth())
                Text("%${(current.progress.fraction * 100).toInt()}")
                OutlinedButton(
                    onClick = {
                        orchestrator.cancel()
                        videoMuxer.cancel()
                        workJob?.cancel()
                        state = ScreenState.Ready(current.media)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("İptal") }
            }
            is ScreenState.Results -> {
                MediaCard(current.media)
                StemCard(
                    title = "🎤 Sadece Vokal",
                    file = current.separation.vocalsWav,
                    onPlay = { play(it) },
                    onSave = {
                        pendingSave = it
                        saveAudio.launch(OutputNaming.vocals(current.media.displayName))
                    },
                    onShare = { share(it, "audio/wav") },
                )
                StemCard(
                    title = "🎵 Enstrümantal",
                    file = current.separation.instrumentalWav,
                    onPlay = { play(it) },
                    onSave = {
                        pendingSave = it
                        saveAudio.launch(OutputNaming.instrumental(current.media.displayName))
                    },
                    onShare = { share(it, "audio/wav") },
                )
                if (current.media.kind == MediaKind.VIDEO) {
                    if (current.vocalVideo == null) {
                        Button(onClick = { createVocalVideo(current) }, enabled = !current.videoBusy, modifier = Modifier.fillMaxWidth()) {
                            Text(if (current.videoBusy) "Video oluşturuluyor…" else "Videoyu Sadece Vokalle Oluştur")
                        }
                        if (current.videoBusy) {
                            LinearProgressIndicator(progress = { current.videoProgress }, modifier = Modifier.fillMaxWidth())
                        }
                    } else {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🎬 Sadece Vokalli Video", fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        pendingSave = current.vocalVideo
                                        saveVideo.launch(OutputNaming.vocalVideo(current.media.displayName))
                                    }) { Text("Kaydet") }
                                    OutlinedButton(onClick = { share(current.vocalVideo, "video/mp4") }) { Text("Paylaş") }
                                }
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        orchestrator.cleanupCompletedSession()
                        state = ScreenState.Empty
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Yeni Dosya") }
            }
            is ScreenState.Error -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Hata", fontWeight = FontWeight.Bold)
                        Text(current.message)
                        Button(onClick = { state = current.media?.let { ScreenState.Ready(it) } ?: ScreenState.Empty }) { Text("Tamam") }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Tüm ayırma işlemi telefonda yapılır; dosyan bir sunucuya yüklenmez.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MediaCard(media: SelectedMedia) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(media.displayName, fontWeight = FontWeight.Bold)
            val seconds = media.durationMs / 1000
            Text("${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')} • ${if (media.kind == MediaKind.VIDEO) "Video" else "Ses"}")
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
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text("${file.length() / 1024 / 1024} MB")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onPlay(file) }) { Text("Dinle") }
                OutlinedButton(onClick = { onSave(file) }) { Text("Kaydet") }
                OutlinedButton(onClick = { onShare(file) }) { Text("Paylaş") }
            }
        }
    }
}
