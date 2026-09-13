package app.namaz.tr.v8.today

import app.namaz.tr.v8.prayer.Prayer
import app.namaz.tr.v8.prayer.PrayerDay
import app.namaz.tr.v8.prayer.PrayerScheduleEngine
import app.namaz.tr.v8.prayer.PrayerTime
import app.namaz.tr.v8.tracking.PrayerTrackState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayStateFactoryTest {
    private val zone = ZoneId.of("Europe/Istanbul")
    private val factory = TodayStateFactory(PrayerScheduleEngine())

    @Test
    fun onlyFivePrayersAreTrackableAndSunriseStaysInformational() {
        val date = LocalDate.of(2026, 9, 13)
        val today = fixture(date)
        val tomorrow = fixture(date.plusDays(1))
        val now = ZonedDateTime.of(date, LocalTime.of(14, 0), zone)

        val state = factory.create(today, tomorrow, now, emptyMap())

        assertEquals(5, state.prayerRows.size)
        assertTrue(state.prayerRows.none { it.prayer == Prayer.SUNRISE })
        assertEquals(LocalTime.of(6, 35), state.sunrise)
        assertEquals(Prayer.ASR, state.nextPrayer.prayer)
    }

    @Test
    fun trackingStateIsReflectedInCorrectPrayerRow() {
        val date = LocalDate.of(2026, 9, 13)
        val state = factory.create(
            fixture(date),
            fixture(date.plusDays(1)),
            ZonedDateTime.of(date, LocalTime.of(14, 0), zone),
            mapOf(Prayer.DHUHR to PrayerTrackState.PRAYED)
        )

        assertEquals(
            PrayerTrackState.PRAYED,
            state.prayerRows.single { it.prayer == Prayer.DHUHR }.trackState
        )
        assertEquals(
            PrayerTrackState.UNSET,
            state.prayerRows.single { it.prayer == Prayer.ASR }.trackState
        )
    }

    private fun fixture(date: LocalDate): PrayerDay = PrayerDay(
        date = date,
        city = "Ankara",
        latitude = 39.9334,
        longitude = 32.8597,
        zoneId = zone,
        times = listOf(
            PrayerTime(Prayer.FAJR, LocalDateTime.of(date, LocalTime.of(5, 10))),
            PrayerTime(Prayer.SUNRISE, LocalDateTime.of(date, LocalTime.of(6, 35))),
            PrayerTime(Prayer.DHUHR, LocalDateTime.of(date, LocalTime.of(13, 0))),
            PrayerTime(Prayer.ASR, LocalDateTime.of(date, LocalTime.of(16, 45))),
            PrayerTime(Prayer.MAGHRIB, LocalDateTime.of(date, LocalTime.of(19, 0))),
            PrayerTime(Prayer.ISHA, LocalDateTime.of(date, LocalTime.of(20, 25)))
        )
    )
}
