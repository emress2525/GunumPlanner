package app.namaz.tr.v8.prayer

import app.namaz.tr.v8.model.PrayerInstant
import app.namaz.tr.v8.model.PrayerLocation
import app.namaz.tr.v8.model.PrayerName
import app.namaz.tr.v8.model.PrayerSchedule
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PrayerScheduleTest {
    private val base = Instant.parse("2026-09-13T02:00:00Z")

    private fun sampleSchedule() = PrayerSchedule(
        date = LocalDate.of(2026, 9, 13),
        zoneId = "Europe/Istanbul",
        location = PrayerLocation("Ankara", 39.9334, 32.8597, "Europe/Istanbul"),
        methodLabel = "Diyanet’e yaklaşık hesap (Adhan Turkey)",
        prayers = listOf(
            PrayerInstant(PrayerName.FAJR, base),
            PrayerInstant(PrayerName.SUNRISE, base.plusSeconds(3600)),
            PrayerInstant(PrayerName.DHUHR, base.plusSeconds(6 * 3600)),
            PrayerInstant(PrayerName.ASR, base.plusSeconds(10 * 3600)),
            PrayerInstant(PrayerName.MAGHRIB, base.plusSeconds(13 * 3600)),
            PrayerInstant(PrayerName.ISHA, base.plusSeconds(15 * 3600)),
        ),
    )

    @Test
    fun manualAdjustmentChangesOnlySelectedPrayer() {
        val original = sampleSchedule()
        val adjusted = original.withAdjustments(mapOf(PrayerName.ASR to 3))
        assertEquals(original.asr.instant.plusSeconds(180), adjusted.asr.instant)
        assertEquals(original.maghrib.instant, adjusted.maghrib.instant)
    }

    @Test
    fun nextPrayerSkipsSunriseAsTrackablePrayer() {
        val schedule = sampleSchedule()
        val afterSunrise = base.plusSeconds(2 * 3600)
        assertEquals(PrayerName.DHUHR, schedule.nextPrayer(afterSunrise)!!.name)
        assertFalse(schedule.sunrise.name.isTrackable)
    }
}
