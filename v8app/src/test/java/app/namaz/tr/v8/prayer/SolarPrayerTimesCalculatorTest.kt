package app.namaz.tr.v8.prayer

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolarPrayerTimesCalculatorTest {
    private val calculator = SolarPrayerTimesCalculator()
    private val zone = ZoneId.of("Europe/Istanbul")

    @Test
    fun ankaraScheduleContainsAllSixEventsInOrder() {
        val day = calculator.calculate(
            date = LocalDate.of(2026, 9, 13),
            city = "Ankara",
            latitude = 39.9334,
            longitude = 32.8597,
            zoneId = zone,
            asrMethod = AsrMethod.HANAFI
        )

        assertEquals(Prayer.entries.toList(), day.times.map { it.prayer })
        assertTrue(day.times.zipWithNext().all { (a, b) -> a.at.isBefore(b.at) })
        assertTrue(day.timeOf(Prayer.FAJR).isBefore(day.timeOf(Prayer.SUNRISE)))
        assertTrue(day.timeOf(Prayer.MAGHRIB).isBefore(day.timeOf(Prayer.ISHA)))
    }

    @Test
    fun hanafiAsrIsLaterThanShafiiAsr() {
        val date = LocalDate.of(2026, 9, 13)
        val hanafi = calculator.calculate(date, "Ankara", 39.9334, 32.8597, zone, AsrMethod.HANAFI)
        val shafii = calculator.calculate(date, "Ankara", 39.9334, 32.8597, zone, AsrMethod.SHAFII)

        assertTrue(hanafi.timeOf(Prayer.ASR).isAfter(shafii.timeOf(Prayer.ASR)))
    }

    @Test
    fun calculationLabelDoesNotClaimOfficialDiyanetSource() {
        val day = calculator.calculate(
            LocalDate.of(2026, 9, 13), "Ankara", 39.9334, 32.8597, zone, AsrMethod.HANAFI
        )

        assertTrue(day.calculationLabel.contains("Astronomik"))
        assertTrue(!day.calculationLabel.contains("Diyanet", ignoreCase = true))
    }
}
