package app.namaz.tr.v8.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.namaz.tr.v8.onboarding.OnboardingRepository
import app.namaz.tr.v8.onboarding.OnboardingScreen
import app.namaz.tr.v8.onboarding.OnboardingState
import kotlinx.coroutines.launch

private val Emerald = Color(0xFF0F5C4B)
private val Cream = Color(0xFFF6F1E6)
private val DeepEmerald = Color(0xFF083D33)

private val NamazLightColors = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    secondary = DeepEmerald,
    background = Cream,
    surface = Cream,
    onBackground = Color(0xFF1C2521),
    onSurface = Color(0xFF1C2521)
)

@Composable
fun NamazV8App() {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { OnboardingRepository(context) }
    val state by repository.state.collectAsStateWithLifecycle(
        initialValue = OnboardingState(profile = null, mode = AppMode.SIMPLE)
    )
    val scope = rememberCoroutineScope()

    MaterialTheme(colorScheme = NamazLightColors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (state.profile == null) {
                OnboardingScreen { profile, mode ->
                    scope.launch { repository.complete(profile, mode) }
                }
            } else {
                MainScaffold(
                    mode = state.mode,
                    onModeChange = { mode -> scope.launch { repository.setMode(mode) } }
                )
            }
        }
    }
}
