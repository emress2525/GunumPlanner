package app.namaz.tr.v8

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.namaz.tr.v8.ui.NamazApp
import app.namaz.tr.v8.ui.theme.NamazTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NamazTheme {
                NamazApp()
            }
        }
    }
}
