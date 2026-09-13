package app.namaz.tr.v8.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF176B55),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F2E8),
    background = Color(0xFFFFFBF5),
    surface = Color(0xFFFFFBF5),
    onSurface = Color(0xFF1B1C1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CD9C5),
    onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF00513F),
    background = Color(0xFF111513),
    surface = Color(0xFF111513),
    onSurface = Color(0xFFE1E3DF),
)

@Composable
fun NamazTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
