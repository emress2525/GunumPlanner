package app.namaz.tr.v8.today

import app.namaz.tr.v8.model.PrayerInstant
import app.namaz.tr.v8.model.PrayerLocation
import app.namaz.tr.v8.model.PrayerName
import app.namaz.tr.v8.model.PrayerSchedule
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayViewModelTest {
    @Test
    fun todayStateShowsNextPrayerFiveTrackablePrayersAndSunriseInfo() {
        val base = Instant.parse("2026-09-13T02:00:00Z")
        val schedule = PrayerSchedule(
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
        val state = TodayUiStateMapper().map(schedule, base.plusSeconds(7 * 3600))
        assertEquals("İkindi", state.nextPrayer!!.name)
        assertEquals(5, state.trackablePrayers.size)
        assertNotNull(state.sunrise)
        assertTrue(state.methodLabel.contains("yaklaşık"))
    }
}
