package app.namaz.tr.v8.alarm

import app.namaz.tr.v8.model.PrayerInstant
import app.namaz.tr.v8.model.PrayerLocation
import app.namaz.tr.v8.model.PrayerName
import app.namaz.tr.v8.model.PrayerSchedule
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerAlarmPlannerTest {
    @Test
    fun plannerCreatesPrayerAndEnabledPreReminderOnly() {
        val preferences = AlarmPreferences(
            PrayerName.entries.filter { it.isTrackable }.associateWith { prayer ->
                if (prayer == PrayerName.ISHA) PrayerAlarmPreference(AdhanMode.OFF, 10)
                else PrayerAlarmPreference(AdhanMode.FULL, 10)
            },
        )
        val alarms = PrayerAlarmPlanner().plan(sampleSchedule(), preferences)
        assertTrue(alarms.any { it.prayer == PrayerName.MAGHRIB && it.kind == AlarmKind.ADHAN })
        assertTrue(alarms.any { it.prayer == PrayerName.MAGHRIB && it.kind == AlarmKind.PRE_REMINDER })
        assertFalse(alarms.any { it.prayer == PrayerName.ISHA })
        assertFalse(alarms.any { it.prayer == PrayerName.SUNRISE })
    }

    private fun sampleSchedule(): PrayerSchedule {
        val base = Instant.parse("2026-09-13T02:00:00Z")
        return PrayerSchedule(
            LocalDate.of(2026, 9, 13),
            "Europe/Istanbul",
            PrayerLocation("Ankara", 39.9334, 32.8597, "Europe/Istanbul"),
            "Diyanet’e yaklaşık hesap (Adhan Turkey)",
            listOf(
                PrayerInstant(PrayerName.FAJR, base),
                PrayerInstant(PrayerName.SUNRISE, base.plusSeconds(3600)),
                PrayerInstant(PrayerName.DHUHR, base.plusSeconds(6 * 3600)),
                PrayerInstant(PrayerName.ASR, base.plusSeconds(10 * 3600)),
                PrayerInstant(PrayerName.MAGHRIB, base.plusSeconds(13 * 3600)),
                PrayerInstant(PrayerName.ISHA, base.plusSeconds(15 * 3600)),
            ),
        )
    }
}
