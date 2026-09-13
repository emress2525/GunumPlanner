package app.namaz.tr.v8.prayer

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PrayerScheduleEngineTest {
    private val zone = ZoneId.of("Europe/Istanbul")
    private val engine = PrayerScheduleEngine()

    @Test
    fun beforeFajrReturnsTodayFajr() {
        val today = fixture(LocalDate.of(2026, 9, 13))
        val tomorrow = fixture(LocalDate.of(2026, 9, 14))
        val now = ZonedDateTime.of(LocalDateTime.of(today.date, LocalTime.of(4, 0)), zone)

        assertEquals(Prayer.FAJR, engine.next(today, now, tomorrow).prayer)
    }

    @Test
    fun betweenDhuhrAndAsrReturnsAsr() {
        val today = fixture(LocalDate.of(2026, 9, 13))
        val tomorrow = fixture(LocalDate.of(2026, 9, 14))
        val now = ZonedDateTime.of(LocalDateTime.of(today.date, LocalTime.of(15, 0)), zone)

        assertEquals(Prayer.ASR, engine.next(today, now, tomorrow).prayer)
    }

    @Test
    fun afterIshaReturnsTomorrowFajr() {
        val today = fixture(LocalDate.of(2026, 9, 13))
        val tomorrow = fixture(LocalDate.of(2026, 9, 14))
        val now = ZonedDateTime.of(LocalDateTime.of(today.date, LocalTime.of(23, 50)), zone)

        val next = engine.next(today, now, tomorrow)
        assertEquals(Prayer.FAJR, next.prayer)
        assertEquals(tomorrow.date, next.at.toLocalDate())
    }

    @Test
    fun manualOffsetMovesOnlySelectedPrayer() {
        val day = fixture(LocalDate.of(2026, 9, 13))
        val adjusted = engine.applyOffsets(day, mapOf(Prayer.MAGHRIB to 5))

        assertEquals(LocalTime.of(19, 5), adjusted.timeOf(Prayer.MAGHRIB))
        assertEquals(day.timeOf(Prayer.ASR), adjusted.timeOf(Prayer.ASR))
        assertEquals(day.timeOf(Prayer.ISHA), adjusted.timeOf(Prayer.ISHA))
    }

    @Test
    fun sunriseIsNotTrackable() {
        assertFalse(Prayer.SUNRISE.isTrackable)
        assertEquals(5, Prayer.entries.count { it.isTrackable })
    }

    private fun fixture(date: LocalDate): PrayerDay {
        val times = listOf(
            PrayerTime(Prayer.FAJR, LocalDateTime.of(date, LocalTime.of(5, 10))),
            PrayerTime(Prayer.SUNRISE, LocalDateTime.of(date, LocalTime.of(6, 35))),
            PrayerTime(Prayer.DHUHR, LocalDateTime.of(date, LocalTime.of(13, 0))),
            PrayerTime(Prayer.ASR, LocalDateTime.of(date, LocalTime.of(16, 45))),
            PrayerTime(Prayer.MAGHRIB, LocalDateTime.of(date, LocalTime.of(19, 0))),
            PrayerTime(Prayer.ISHA, LocalDateTime.of(date, LocalTime.of(20, 25)))
        )
        return PrayerDay(
            date = date,
            city = "Ankara",
            latitude = 39.9334,
            longitude = 32.8597,
            zoneId = zone,
            times = times
        )
    }
}
