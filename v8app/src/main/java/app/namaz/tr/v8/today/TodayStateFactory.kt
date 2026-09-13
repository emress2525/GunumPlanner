package app.namaz.tr.v8.today

import app.namaz.tr.v8.prayer.Prayer
import app.namaz.tr.v8.prayer.PrayerDay
import app.namaz.tr.v8.prayer.PrayerScheduleEngine
import app.namaz.tr.v8.prayer.PrayerTime
import app.namaz.tr.v8.tracking.PrayerTrackState
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

data class PrayerRowUi(
    val prayer: Prayer,
    val time: LocalTime,
    val trackState: PrayerTrackState
)

data class TodayUiState(
    val city: String,
    val calculationLabel: String,
    val nextPrayer: PrayerTime,
    val remaining: Duration,
    val sunrise: LocalTime,
    val prayerRows: List<PrayerRowUi>
)

class TodayStateFactory(
    private val engine: PrayerScheduleEngine
) {
    fun create(
        today: PrayerDay,
        tomorrow: PrayerDay,
        now: ZonedDateTime,
        tracking: Map<Prayer, PrayerTrackState>
    ): TodayUiState {
        val next = engine.next(today, now, tomorrow)
        val rows = today.times
            .filter { it.prayer.isTrackable }
            .map { item ->
                PrayerRowUi(
                    prayer = item.prayer,
                    time = item.at.toLocalTime(),
                    trackState = tracking[item.prayer] ?: PrayerTrackState.UNSET
                )
            }

        return TodayUiState(
            city = today.city,
            calculationLabel = today.calculationLabel,
            nextPrayer = next,
            remaining = engine.remaining(now, next, today.zoneId),
            sunrise = today.timeOf(Prayer.SUNRISE),
            prayerRows = rows
        )
    }
}
