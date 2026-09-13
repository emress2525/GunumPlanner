package app.namaz.tr.v8.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.namaz.tr.v8.app.AppMode

private val Emerald = Color(0xFF0F5C4B)
private val Cream = Color(0xFFF6F1E6)

@Composable
fun OnboardingScreen(
    onComplete: (UserProfile, AppMode) -> Unit
) {
    var selectedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var selectedMode by remember { mutableStateOf(AppMode.SIMPLE) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Seni nasıl hazırlayayım?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Emerald,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Bu seçim sadece ana ekranı ve öğrenme sırasını kişiselleştirir. Hiçbir özellik kilitlenmez.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        items(UserProfile.entries) { profile ->
            val selected = selectedProfile == profile
            Card(
                onClick = { selectedProfile = profile },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) Emerald.copy(alpha = 0.10f) else Color.White
                ),
                border = if (selected) CardDefaults.outlinedCardBorder() else null
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(profile.title, fontWeight = FontWeight.SemiBold, color = Emerald)
                    Text(profile.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Görünüm", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = selectedMode == AppMode.SIMPLE,
                        onClick = { selectedMode = AppMode.SIMPLE },
                        label = { Text("Basit Mod") }
                    )
                    FilterChip(
                        selected = selectedMode == AppMode.FULL,
                        onClick = { selectedMode = AppMode.FULL },
                        label = { Text("Tam Mod") }
                    )
                }
            }
        }

        item {
            Button(
                onClick = { selectedProfile?.let { onComplete(it, selectedMode) } },
                enabled = selectedProfile != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Uygulamayı hazırla")
            }
        }
    }
}
