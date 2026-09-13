package app.namaz.tr.v8

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import app.namaz.tr.v8.alarm.PrayerAlarmCoordinator
import app.namaz.tr.v8.ui.NamazApp
import app.namaz.tr.v8.ui.theme.NamazTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as NamazApplication).graph
        lifecycleScope.launch {
            PrayerAlarmCoordinator(this@MainActivity, graph).rescheduleUpcoming()
        }
        setContent {
            NamazTheme { NamazApp(graph) }
        }
    }
}
