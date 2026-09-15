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
import app.namaz.tr.v8.model.PrayerLocation
import app.namaz.tr.v8.model.PrayerName
import app.namaz.tr.v8.prayer.PrayerRuntimeSettings

data class PrayerCityPreset(
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val zoneId: String = "Europe/Istanbul",
) {
    fun location(): PrayerLocation = PrayerLocation(city, latitude, longitude, zoneId)
}

object PrayerCityCatalog {
    val turkey = listOf(
        PrayerCityPreset("Ankara", 39.9334, 32.8597),
        PrayerCityPreset("İstanbul", 41.0082, 28.9784),
        PrayerCityPreset("İzmir", 38.4237, 27.1428),
        PrayerCityPreset("Bursa", 40.1950, 29.0600),
        PrayerCityPreset("Antalya", 36.8969, 30.7133),
        PrayerCityPreset("Adana", 37.0000, 35.3213),
        PrayerCityPreset("Konya", 37.8746, 32.4932),
        PrayerCityPreset("Gaziantep", 37.0662, 37.3833),
        PrayerCityPreset("Diyarbakır", 37.9144, 40.2306),
        PrayerCityPreset("Samsun", 41.2867, 36.3300),
        PrayerCityPreset("Trabzon", 41.0027, 39.7168),
        PrayerCityPreset("Erzurum", 39.9043, 41.2679),
    )
}

private val trackablePrayers = PrayerName.entries.filter { it.isTrackable }
private val alarmModes = listOf(
    "OFF" to "Kapalı",
    "NOTIFICATION" to "Bildirim",
    "SHORT" to "Kısa ses",
    "FULL" to "Tam ezan",
)
private val preReminderChoices = listOf(0, 5, 10, 15, 30)

@Composable
fun PrayerScreen(
    settings: PrayerRuntimeSettings,
    qazaTotal: Int,
    alarmModeByPrayer: Map<PrayerName, String>,
    preReminderByPrayer: Map<PrayerName, Int>,
    onLocationChange: (PrayerLocation) -> Unit,
    onMadhabChange: (MadhabChoice) -> Unit,
    onAdjustmentChange: (PrayerName, Int) -> Unit,
    onAlarmModeChange: (PrayerName, String) -> Unit,
    onPreReminderChange: (PrayerName, Int) -> Unit,
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

        Text("Şehir", style = MaterialTheme.typography.titleLarge)
        Text("Konum izni gerekmeden şehir seçebilirsin. Seçim; ana ekran, ezan ve widget verisini birlikte günceller.")
        PrayerCityCatalog.turkey.chunked(3).forEach { group ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                group.forEach { preset ->
                    OutlinedButton(
                        onClick = { onLocationChange(preset.location()) },
                        enabled = settings.location.city != preset.city,
                        modifier = Modifier.weight(1f),
                    ) { Text(preset.city) }
                }
            }
        }

        Text("İkindi hesabı", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onMadhabChange(MadhabChoice.HANAFI) }, enabled = settings.config.madhab != MadhabChoice.HANAFI) { Text("Hanefî") }
            Button(onClick = { onMadhabChange(MadhabChoice.SHAFI) }, enabled = settings.config.madhab != MadhabChoice.SHAFI) { Text("Şafiî") }
        }

        Text("Ezan ve ön uyarılar", style = MaterialTheme.typography.titleLarge)
        Text("Her farz vakti ayrı ayarlanır. Ön uyarı 0 seçilirse kapalıdır.")
        trackablePrayers.forEach { prayer ->
            val currentMode = alarmModeByPrayer[prayer] ?: "FULL"
            val currentReminder = preReminderByPrayer[prayer] ?: 10
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(prayer.displayName, style = MaterialTheme.typography.titleMedium)
                    alarmModes.chunked(2).forEach { group ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            group.forEach { (value, label) ->
                                OutlinedButton(
                                    onClick = { onAlarmModeChange(prayer, value) },
                                    enabled = currentMode != value,
                                    modifier = Modifier.weight(1f),
                                ) { Text(if (currentMode == value) "✓ $label" else label) }
                            }
                        }
                    }
                    Text("Ön uyarı: ${if (currentReminder == 0) "Kapalı" else "$currentReminder dk önce"}")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        preReminderChoices.forEach { minutes ->
                            OutlinedButton(
                                onClick = { onPreReminderChange(prayer, minutes) },
                                enabled = currentReminder != minutes,
                                modifier = Modifier.weight(1f),
                            ) { Text(if (minutes == 0) "0" else "$minutes") }
                        }
                    }
                }
            }
        }

        Text("Manuel dakika düzeltmesi", style = MaterialTheme.typography.titleLarge)
        PrayerName.entries.forEach { prayer ->
            val current = settings.config.adjustmentsMinutes[prayer] ?: 0
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${prayer.displayName}: ${if (current >= 0) "+" else ""}$current dk")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { onAdjustmentChange(prayer, (current - 1).coerceAtLeast(-120)) }, enabled = current > -120) { Text("−") }
                        OutlinedButton(onClick = { onAdjustmentChange(prayer, (current + 1).coerceAtMost(120)) }, enabled = current < 120) { Text("+") }
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
