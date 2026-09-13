package app.namaz.tr.v8.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TodayScreen(
    state: TodayUiState?,
    onPrayerCompleted: (app.namaz.tr.v8.model.PrayerName, Boolean) -> Unit,
    onOpenPrayerDetails: () -> Unit,
) {
    if (state == null) {
        Column(Modifier.fillMaxSize().padding(24.dp)) { Text("Namaz vakitleri hazırlanıyor…") }
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Bugün", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Sıradaki Namaz", style = MaterialTheme.typography.labelLarge)
                if (state.nextPrayer != null) {
                    Text("${state.nextPrayer.name} · ${state.nextPrayer.timeText}", style = MaterialTheme.typography.headlineSmall)
                    Text("${state.nextPrayer.remainingText} kaldı", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text("Bugünün farz vakitleri tamamlandı", style = MaterialTheme.typography.titleMedium)
                }
                Text(state.city, style = MaterialTheme.typography.bodyLarge)
                Text(state.methodLabel, style = MaterialTheme.typography.bodySmall)
            }
        }

        Text("Vakitler", style = MaterialTheme.typography.titleLarge)
        state.trackablePrayers.forEach { prayer ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(prayer.title, style = MaterialTheme.typography.titleMedium)
                        Text(prayer.timeText, style = MaterialTheme.typography.bodyLarge)
                    }
                    Checkbox(
                        checked = false,
                        onCheckedChange = { onPrayerCompleted(prayer.name, it) },
                    )
                }
            }
        }

        state.sunrise?.let {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Güneş · ${it.timeText}", style = MaterialTheme.typography.titleMedium)
                    Text("Güneş bir namaz takip satırı değildir; bilgi olarak ayrı gösterilir.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Button(onClick = onOpenPrayerDetails, modifier = Modifier.fillMaxWidth()) {
            Text("Namaz ayarları ve ayrıntılar")
        }
    }
}
