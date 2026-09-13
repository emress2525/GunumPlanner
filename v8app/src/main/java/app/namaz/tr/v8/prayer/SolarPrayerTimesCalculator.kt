package app.namaz.tr.v8.prayer

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

enum class AsrMethod(val shadowFactor: Double) {
    SHAFII(1.0),
    HANAFI(2.0)
}

class SolarPrayerTimesCalculator(
    private val fajrAngle: Double = 18.0,
    private val ishaAngle: Double = 17.0
) {
    fun calculate(
        date: LocalDate,
        city: String,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        asrMethod: AsrMethod
    ): PrayerDay {
        require(latitude in -90.0..90.0)
        require(longitude in -180.0..180.0)

        val dayOfYear = date.dayOfYear.toDouble()
        val gamma = 2.0 * PI / daysInYear(date) * (dayOfYear - 1.0)
        val equationOfTime = 229.18 * (
            0.000075 +
                0.001868 * cos(gamma) -
                0.032077 * sin(gamma) -
                0.014615 * cos(2.0 * gamma) -
                0.040849 * sin(2.0 * gamma)
            )
        val declination =
            0.006918 -
                0.399912 * cos(gamma) +
                0.070257 * sin(gamma) -
                0.006758 * cos(2.0 * gamma) +
                0.000907 * sin(2.0 * gamma) -
                0.002697 * cos(3.0 * gamma) +
                0.00148 * sin(3.0 * gamma)

        val zoneOffsetMinutes = date.atTime(12, 0).atZone(zoneId).offset.totalSeconds / 60.0
        val solarNoonMinutes = 720.0 - 4.0 * longitude - equationOfTime + zoneOffsetMinutes

        val sunriseHa = hourAngle(latitude, declination, altitudeDegrees = -0.833)
        val fajrHa = hourAngle(latitude, declination, altitudeDegrees = -fajrAngle)
        val ishaHa = hourAngle(latitude, declination, altitudeDegrees = -ishaAngle)
        val asrAltitude = asrAltitude(latitude, declination, asrMethod.shadowFactor)
        val asrHa = hourAngle(latitude, declination, altitudeDegrees = asrAltitude)

        val raw = linkedMapOf(
            Prayer.FAJR to solarNoonMinutes - 4.0 * fajrHa,
            Prayer.SUNRISE to solarNoonMinutes - 4.0 * sunriseHa,
            Prayer.DHUHR to solarNoonMinutes + 1.0,
            Prayer.ASR to solarNoonMinutes + 4.0 * asrHa,
            Prayer.MAGHRIB to solarNoonMinutes + 4.0 * sunriseHa,
            Prayer.ISHA to solarNoonMinutes + 4.0 * ishaHa
        )

        val times = raw.map { (prayer, minutes) ->
            PrayerTime(prayer, LocalDateTime.of(date, minutesToTime(minutes)))
        }

        require(times.zipWithNext().all { (a, b) -> a.at.isBefore(b.at) }) {
            "Calculated prayer events are not strictly ordered for $city on $date"
        }

        return PrayerDay(
            date = date,
            city = city,
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            times = times,
            calculationLabel = "Astronomik hesaplama · ${if (asrMethod == AsrMethod.HANAFI) "Hanefî" else "Şafiî"} ikindi"
        )
    }

    private fun hourAngle(
        latitudeDegrees: Double,
        declinationRadians: Double,
        altitudeDegrees: Double
    ): Double {
        val latitude = Math.toRadians(latitudeDegrees)
        val altitude = Math.toRadians(altitudeDegrees)
        val numerator = sin(altitude) - sin(latitude) * sin(declinationRadians)
        val denominator = cos(latitude) * cos(declinationRadians)
        val cosH = (numerator / denominator).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosH))
    }

    private fun asrAltitude(
        latitudeDegrees: Double,
        declinationRadians: Double,
        shadowFactor: Double
    ): Double {
        val declinationDegrees = Math.toDegrees(declinationRadians)
        val angle = Math.toDegrees(
            atan(1.0 / (shadowFactor + tan(Math.toRadians(abs(latitudeDegrees - declinationDegrees)))))
        )
        return angle
    }

    private fun minutesToTime(minutesFromMidnight: Double): LocalTime {
        val rounded = kotlin.math.round(minutesFromMidnight).toLong()
        val normalized = ((rounded % 1440L) + 1440L) % 1440L
        return LocalTime.of((normalized / 60L).toInt(), (normalized % 60L).toInt())
    }

    private fun daysInYear(date: LocalDate): Double = if (date.isLeapYear) 366.0 else 365.0
}
