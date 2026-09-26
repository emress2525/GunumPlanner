package com.vocalisolator.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vocalisolator.app.media.AndroidMediaInspector
import com.vocalisolator.app.media.AndroidPcmNormalizer
import com.vocalisolator.app.media.AndroidVideoMuxer
import com.vocalisolator.app.output.AndroidOutputExporter
import com.vocalisolator.app.separation.AndroidModelProvisioner
import com.vocalisolator.app.separation.DefaultSeparationOrchestrator
import com.vocalisolator.app.separation.DemucsNativeBridge
import com.vocalisolator.app.ui.VocalIsolatorApp
import com.vocalisolator.app.ui.VocalIsolatorViewModel
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inspector = AndroidMediaInspector(this)
        val normalizer = AndroidPcmNormalizer(this)
        val modelProvisioner = AndroidModelProvisioner(this)
        val orchestrator = DefaultSeparationOrchestrator(
            cacheDir = File(cacheDir, "vocal-isolator"),
            normalizer = normalizer,
            modelProvisioner = modelProvisioner,
            engineFactory = { modelFile -> DemucsNativeBridge { Result.success(modelFile) } },
        )
        val videoMuxer = AndroidVideoMuxer(this)
        val exporter = AndroidOutputExporter(this)
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                VocalIsolatorViewModel(
                    inspector = inspector,
                    orchestrator = orchestrator,
                    videoMuxer = videoMuxer,
                    outputExporter = exporter,
                    videoOutputDir = File(cacheDir, "vocal-isolator-video"),
                ) as T
        }
        val viewModel = ViewModelProvider(this, factory)[VocalIsolatorViewModel::class.java]
        setContent { VocalIsolatorApp(viewModel) }
    }
}
