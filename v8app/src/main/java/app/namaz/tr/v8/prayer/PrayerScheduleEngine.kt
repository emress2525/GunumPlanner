package app.namaz.tr.v8.prayer

import java.time.Duration
import java.time.ZonedDateTime

class PrayerScheduleEngine {
    fun next(
        day: PrayerDay,
        now: ZonedDateTime,
        nextDay: PrayerDay
    ): PrayerTime {
        val localNow = now.withZoneSameInstant(day.zoneId).toLocalDateTime()
        return day.times
            .asSequence()
            .filter { it.prayer.isTrackable }
            .sortedBy { it.at }
            .firstOrNull { !it.at.isBefore(localNow) }
            ?: nextDay.times
                .asSequence()
                .filter { it.prayer.isTrackable }
                .sortedBy { it.at }
                .firstOrNull()
            ?: error("next day has no trackable prayer")
    }

    fun applyOffsets(
        day: PrayerDay,
        offsetsMinutes: Map<Prayer, Int>
    ): PrayerDay {
        val adjusted = day.times.map { item ->
            val offset = offsetsMinutes[item.prayer] ?: 0
            item.copy(at = item.at.plusMinutes(offset.toLong()))
        }
        return day.copy(times = adjusted)
    }

    fun remaining(now: ZonedDateTime, prayerTime: PrayerTime, zoneId: java.time.ZoneId): Duration {
        val target = prayerTime.at.atZone(zoneId)
        val normalizedNow = now.withZoneSameInstant(zoneId)
        return Duration.between(normalizedNow, target).coerceAtLeast(Duration.ZERO)
    }

    private fun Duration.coerceAtLeast(minimum: Duration): Duration =
        if (isNegative) minimum else this
}
