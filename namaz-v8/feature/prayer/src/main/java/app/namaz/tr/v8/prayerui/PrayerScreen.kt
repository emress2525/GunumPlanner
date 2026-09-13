package app.namaz.tr.v8.prayerui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.namaz.tr.v8.model.MadhabChoice
import app.namaz.tr.v8.model.PrayerName
import app.namaz.tr.v8.prayer.PrayerRuntimeSettings

@Composable
fun PrayerScreen(
    settings: PrayerRuntimeSettings,
    qazaTotal: Int,
    onMadhabChange: (MadhabChoice) -> Unit,
    onAdjustmentChange: (PrayerName, Int) -> Unit,
    onQazaTotalChange: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Namaz ayarları", style = MaterialTheme.typography.headlineMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(settings.location.city, style = MaterialTheme.typography.titleLarge)
                Text("${settings.location.latitude}, ${settings.location.longitude}")
                Text("Diyanet’e yaklaşık hesap (Adhan Turkey)", style = MaterialTheme.typography.bodyMedium)
                Text("Bu etiket resmî Diyanet verisi anlamına gelmez.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Text("İkindi hesabı", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onMadhabChange(MadhabChoice.HANAFI) }, enabled = settings.config.madhab != MadhabChoice.HANAFI) { Text("Hanefî") }
            Button(onClick = { onMadhabChange(MadhabChoice.SHAFI) }, enabled = settings.config.madhab != MadhabChoice.SHAFI) { Text("Şafiî") }
        }

        Text("Manuel dakika düzeltmesi", style = MaterialTheme.typography.titleLarge)
        PrayerName.entries.forEach { prayer ->
            val current = settings.config.adjustmentsMinutes[prayer] ?: 0
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${prayer.displayName}: ${if (current >= 0) "+" else ""}$current dk")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { onAdjustmentChange(prayer, current - 1) }) { Text("−") }
                        OutlinedButton(onClick = { onAdjustmentChange(prayer, current + 1) }) { Text("+") }
                    }
                }
            }
        }

        Text("Kaza takibi", style = MaterialTheme.typography.titleLarge)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Başlangıç sayısını ben giriyorum", style = MaterialTheme.typography.titleMedium)
                Text("Uygulama geçmiş namazlardan kendi kendine borç hesaplamaz.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onQazaTotalChange((qazaTotal - 1).coerceAtLeast(0)) }) { Text("−") }
                    Text("$qazaTotal", style = MaterialTheme.typography.headlineSmall)
                    OutlinedButton(onClick = { onQazaTotalChange(qazaTotal + 1) }) { Text("+") }
                }
            }
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Geri") }
    }
}
