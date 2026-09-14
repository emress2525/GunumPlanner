package app.namaz.tr.v8.alarm

import app.namaz.tr.v8.model.PrayerName
import app.namaz.tr.v8.model.PrayerSchedule
import java.time.LocalDate

enum class AdhanMode { OFF, NOTIFICATION, SHORT, FULL }
enum class AlarmKind { ADHAN, PRE_REMINDER, DIAGNOSTIC }

data class PrayerAlarmPreference(
    val mode: AdhanMode = AdhanMode.FULL,
    val preReminderMinutes: Int = 10,
)

data class AlarmPreferences(
    val perPrayer: Map<PrayerName, PrayerAlarmPreference> = PrayerName.entries
        .filter { it.isTrackable }
        .associateWith { PrayerAlarmPreference() },
) {
    fun forPrayer(prayer: PrayerName): PrayerAlarmPreference = perPrayer[prayer] ?: PrayerAlarmPreference()
}

data class PlannedAlarm(
    val date: LocalDate,
    val prayer: PrayerName?,
    val kind: AlarmKind,
    val atMillis: Long,
    val mode: AdhanMode,
)

class PrayerAlarmPlanner {
    fun plan(
        schedule: PrayerSchedule,
        preferences: AlarmPreferences,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<PlannedAlarm> = buildList {
        schedule.trackablePrayers.forEach { prayer ->
            val preference = preferences.forPrayer(prayer.name)
            if (preference.mode == AdhanMode.OFF) return@forEach
            val at = prayer.instant.toEpochMilli()
            if (preference.preReminderMinutes > 0) {
                val reminderAt = at - preference.preReminderMinutes * 60_000L
                if (reminderAt > nowMillis) {
                    add(
                        PlannedAlarm(
                            schedule.date,
                            prayer.name,
                            AlarmKind.PRE_REMINDER,
                            reminderAt,
                            AdhanMode.NOTIFICATION,
                        ),
                    )
                }
            }
            if (at > nowMillis) {
                add(PlannedAlarm(schedule.date, prayer.name, AlarmKind.ADHAN, at, preference.mode))
            }
        }
    }
}
