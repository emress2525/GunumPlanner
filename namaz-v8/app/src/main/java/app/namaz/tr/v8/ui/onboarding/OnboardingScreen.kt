package app.namaz.tr.v8.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.namaz.tr.v8.model.UserProfile

private data class ProfileChoice(
    val profile: UserProfile,
    val title: String,
    val detail: String,
)

private val choices = listOf(
    ProfileChoice(UserProfile.RELIGION_FROM_ZERO, "Dini sıfırdan öğrenmek istiyorum", "Temelden başlayan sakin bir öğrenme rotası."),
    ProfileChoice(UserProfile.NEW_TO_PRAYER, "Namaza yeni başladım", "Namaz vakitleri, adımlar ve okunanlarla başlayalım."),
    ProfileChoice(UserProfile.LEARN_QURAN, "Kur’an öğrenmek istiyorum", "Kur’an okuma ve öğrenme araçlarını öne çıkaralım."),
    ProfileChoice(UserProfile.DAILY_WORSHIP, "Sadece günlük ibadetlerimi takip edeceğim", "Vakit, kıble ve günlük takibi sade tutalım."),
)

@Composable
fun OnboardingScreen(
    onProfileSelected: (UserProfile) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Seni nasıl hazırlayayım?", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Bu seçim hiçbir özelliği kilitlemez; yalnızca ana ekranı ve önerileri düzenler.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        choices.forEach { choice ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(choice.title, style = MaterialTheme.typography.titleMedium)
                    Text(choice.detail, style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = { onProfileSelected(choice.profile) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Bununla başla") }
                }
            }
        }
    }
}
