package app.namaz.tr.v8.health

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NotificationHealthScreen(
    items: List<HealthItem>,
    onAction: (HealthAction) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Bildirim Sağlık Merkezi", style = MaterialTheme.typography.headlineMedium)
        Text("Ezan veya bildirim gelmezse burada hangi sistem ayarının engel olduğunu görebilirsin.")
        items.forEach { item ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(statusGlyph(item.status), style = MaterialTheme.typography.titleLarge)
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(item.explanation, style = MaterialTheme.typography.bodyMedium)
                    item.action?.let { action ->
                        OutlinedButton(onClick = { onAction(action) }) { Text(actionLabel(action)) }
                    }
                }
            }
        }
        Button(onClick = { onAction(HealthAction.SEND_TEST_NOTIFICATION) }, modifier = Modifier.fillMaxWidth()) {
            Text("Test bildirimi gönder")
        }
        Button(onClick = { onAction(HealthAction.SCHEDULE_TEST_ALARM) }, modifier = Modifier.fillMaxWidth()) {
            Text("1 dk sonra test alarmı")
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Geri") }
    }
}

private fun statusGlyph(status: HealthStatus): String = when (status) {
    HealthStatus.GREEN -> "✓"
    HealthStatus.YELLOW -> "!"
    HealthStatus.RED -> "×"
}

private fun actionLabel(action: HealthAction): String = when (action) {
    HealthAction.OPEN_NOTIFICATION_SETTINGS -> "Bildirim ayarını aç"
    HealthAction.OPEN_EXACT_ALARM_SETTINGS -> "Alarm iznini aç"
    HealthAction.OPEN_BATTERY_SETTINGS -> "Pil ayarına git"
    HealthAction.OPEN_DND_SETTINGS -> "DND ayarına git"
    HealthAction.OPEN_APP_SETTINGS -> "Uygulama ayarına git"
    HealthAction.SEND_TEST_NOTIFICATION -> "Test bildirimi gönder"
    HealthAction.SCHEDULE_TEST_ALARM -> "Test alarmı kur"
}
