package app.namaz.tr.v8.prayer

import app.namaz.tr.v8.model.MadhabChoice
import app.namaz.tr.v8.model.PrayerCalculationConfig
import app.namaz.tr.v8.model.PrayerLocation
import app.namaz.tr.v8.model.PrayerMethod
import app.namaz.tr.v8.model.PrayerName
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdhanPrayerCalculatorTest {
    private val calculator = AdhanPrayerCalculator()
    private val formatter = DateTimeFormatter.ofPattern("HH:mm")

    @Test
    fun turkeyMethodMatchesKnownIstanbulFixture() {
        val result = calculator.calculate(
            LocalDate.of(2020, 4, 16),
            PrayerLocation("İstanbul", 41.005616, 28.976380, "Europe/Istanbul"),
            PrayerCalculationConfig(PrayerMethod.TURKEY_APPROX, MadhabChoice.HANAFI),
        )
        assertEquals("04:44", localTime(result.fajr.instant, result.zoneId))
        assertEquals("13:09", localTime(result.dhuhr.instant, result.zoneId))
        assertEquals("19:52", localTime(result.maghrib.instant, result.zoneId))
        assertTrue(result.methodLabel.contains("yaklaşık"))
    }

    @Test
    fun hanafiAsrIsNotEarlierThanShafiAsr() {
        val location = PrayerLocation("Ankara", 39.9334, 32.8597, "Europe/Istanbul")
        val date = LocalDate.of(2026, 9, 13)
        val hanafi = calculator.calculate(date, location, PrayerCalculationConfig(madhab = MadhabChoice.HANAFI))
        val shafi = calculator.calculate(date, location, PrayerCalculationConfig(madhab = MadhabChoice.SHAFI))
        assertTrue(!hanafi.asr.instant.isBefore(shafi.asr.instant))
    }

    private fun localTime(instant: java.time.Instant, zoneId: String): String =
        formatter.format(instant.atZone(ZoneId.of(zoneId)))
}
