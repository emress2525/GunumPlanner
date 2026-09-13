package app.namaz.tr.v8.prayer

import app.namaz.tr.v8.model.MadhabChoice
import app.namaz.tr.v8.model.PrayerCalculationConfig
import app.namaz.tr.v8.model.PrayerInstant
import app.namaz.tr.v8.model.PrayerLocation
import app.namaz.tr.v8.model.PrayerMethod
import app.namaz.tr.v8.model.PrayerName
import app.namaz.tr.v8.model.PrayerSchedule
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerAdjustments
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
            PrayerMethod.TURKEY_APPROX -> turkeyParameters()
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

    /**
     * Adhan Java 1.2.1 predates the named TURKEY enum. Keep the verified modern
     * Adhan Turkey parameters explicit here instead of pretending another preset is Diyanet.
     * Modern Adhan defines Turkey as Fajr 18°, Isha 17° with method adjustments:
     * sunrise -7, dhuhr +5, asr +4, maghrib +7 minutes.
     */
    private fun turkeyParameters(): CalculationParameters =
        CalculationParameters(18.0, 17.0, CalculationMethod.OTHER)
            .withMethodAdjustments(PrayerAdjustments(0, -7, 5, 4, 7, 0))

    private fun methodLabel(method: PrayerMethod): String = when (method) {
        PrayerMethod.TURKEY_APPROX -> "Diyanet’e yaklaşık hesap (Adhan Turkey)"
        PrayerMethod.MUSLIM_WORLD_LEAGUE -> "Muslim World League"
        PrayerMethod.EGYPTIAN -> "Egyptian General Authority of Survey"
        PrayerMethod.KARACHI -> "University of Islamic Sciences, Karachi"
        PrayerMethod.UMM_AL_QURA -> "Umm al-Qura University, Makkah"
    }
}
