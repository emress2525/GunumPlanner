package app.namaz.tr.v8.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.namaz.tr.v8.tracking.PrayerTrackState
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Emerald = Color(0xFF0F5C4B)
private val SoftEmerald = Color(0xFFE7F1ED)
private val Muted = Color(0xFF64706B)
private val TurkishLocale = Locale("tr", "TR")
private val ClockFormatter = DateTimeFormatter.ofPattern("HH:mm", TurkishLocale)
private val DateFormatter = DateTimeFormatter.ofPattern("d MMMM EEEE", TurkishLocale)

@Composable
fun TodayScreen(viewModel: TodayViewModel = viewModel()) {
    val screenState by viewModel.state.collectAsStateWithLifecycle()
    val data = screenState.data

    LazyColumn(
        modifier = Modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = 18.dp, bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "Bugün",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Emerald,
                    fontWeight = FontWeight.Bold
                )
                if (data != null) {
                    Text(
                        text = "${data.date.format(DateFormatter)} · ${data.city}",
                        color = Muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        when {
            screenState.errorMessage != null -> item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = screenState.errorMessage ?: "Namaz vakitleri hazırlanamadı.",
                        modifier = Modifier.padding(18.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            data == null -> item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Emerald)
                }
            }

            else -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Emerald)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Sıradaki namaz",
                                color = Color.White.copy(alpha = 0.78f),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = data.nextPrayer.prayer.displayName,
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = data.nextPrayer.at.format(ClockFormatter),
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${formatRemaining(data.remaining)} kaldı",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = data.calculationLabel,
                                color = Color.White.copy(alpha = 0.72f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                            data.prayerRows.forEachIndexed { index, row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = row.prayer.displayName,
                                            fontWeight = if (row.prayer == data.nextPrayer.prayer) FontWeight.Bold else FontWeight.Medium,
                                            color = if (row.prayer == data.nextPrayer.prayer) Emerald else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = row.time.format(ClockFormatter),
                                            color = Muted,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Button(onClick = { viewModel.togglePrayed(row.prayer) }) {
                                        Text(if (row.trackState == PrayerTrackState.UNSET) "Kıldım" else "✓ ${row.trackState.label}")
                                    }
                                }
                                if (index < data.prayerRows.lastIndex) {
                                    HorizontalDivider(color = Color(0xFFE8ECEA))
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftEmerald)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Güneş", fontWeight = FontWeight.SemiBold, color = Emerald)
                                Text("Namaz takibine dahil değildir", color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = data.sunrise.format(ClockFormatter),
                                fontWeight = FontWeight.Bold,
                                color = Emerald,
                                fontSize = 20.sp
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text("Bugün öğren", fontWeight = FontWeight.Bold, color = Emerald)
                            Text(
                                "Namaz vakitleri tek merkezden hesaplanıyor. İkindi yöntemi ve manuel dakika ayarları bir sonraki ayar ekranından yönetilecek.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatRemaining(duration: Duration): String {
    val total = duration.seconds.coerceAtLeast(0L)
    val hours = total / 3600L
    val minutes = (total % 3600L) / 60L
    val seconds = total % 60L
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
