package app.namaz.tr.v8.prayer

import app.namaz.tr.v8.model.MadhabChoice
import app.namaz.tr.v8.model.PrayerCalculationConfig
import app.namaz.tr.v8.model.PrayerInstant
import app.namaz.tr.v8.model.PrayerLocation
import app.namaz.tr.v8.model.PrayerMethod
import app.namaz.tr.v8.model.PrayerName
import app.namaz.tr.v8.model.PrayerSchedule
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.time.LocalDate

class AdhanPrayerCalculator : PrayerCalculator {
    override fun calculate(
        date: LocalDate,
        location: PrayerLocation,
        config: PrayerCalculationConfig,
    ): PrayerSchedule {
        val coordinates = Coordinates(location.latitude, location.longitude)
        val parameters = when (config.method) {
            PrayerMethod.TURKEY_APPROX -> CalculationMethod.TURKEY.getParameters()
            PrayerMethod.MUSLIM_WORLD_LEAGUE -> CalculationMethod.MUSLIM_WORLD_LEAGUE.getParameters()
            PrayerMethod.EGYPTIAN -> CalculationMethod.EGYPTIAN.getParameters()
            PrayerMethod.KARACHI -> CalculationMethod.KARACHI.getParameters()
            PrayerMethod.UMM_AL_QURA -> CalculationMethod.UMM_AL_QURA.getParameters()
        }
        parameters.madhab = if (config.madhab == MadhabChoice.HANAFI) Madhab.HANAFI else Madhab.SHAFI
        val prayerTimes = PrayerTimes(
            coordinates,
            DateComponents(date.year, date.monthValue, date.dayOfMonth),
            parameters,
        )

        return PrayerSchedule(
            date = date,
            zoneId = location.zoneId,
            location = location,
            methodLabel = methodLabel(config.method),
            prayers = listOf(
                PrayerInstant(PrayerName.FAJR, prayerTimes.fajr.toInstant()),
                PrayerInstant(PrayerName.SUNRISE, prayerTimes.sunrise.toInstant()),
                PrayerInstant(PrayerName.DHUHR, prayerTimes.dhuhr.toInstant()),
                PrayerInstant(PrayerName.ASR, prayerTimes.asr.toInstant()),
                PrayerInstant(PrayerName.MAGHRIB, prayerTimes.maghrib.toInstant()),
                PrayerInstant(PrayerName.ISHA, prayerTimes.isha.toInstant()),
            ),
        )
    }

    private fun methodLabel(method: PrayerMethod): String = when (method) {
        PrayerMethod.TURKEY_APPROX -> "Diyanet’e yaklaşık hesap (Adhan Turkey)"
        PrayerMethod.MUSLIM_WORLD_LEAGUE -> "Muslim World League"
        PrayerMethod.EGYPTIAN -> "Egyptian General Authority of Survey"
        PrayerMethod.KARACHI -> "University of Islamic Sciences, Karachi"
        PrayerMethod.UMM_AL_QURA -> "Umm al-Qura University, Makkah"
    }
}
