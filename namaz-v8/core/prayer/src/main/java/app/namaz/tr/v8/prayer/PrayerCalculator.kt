package app.namaz.tr.v8.prayer

import app.namaz.tr.v8.model.PrayerCalculationConfig
import app.namaz.tr.v8.model.PrayerLocation
import app.namaz.tr.v8.model.PrayerSchedule
import java.time.LocalDate

interface PrayerCalculator {
    fun calculate(
        date: LocalDate,
        location: PrayerLocation,
        config: PrayerCalculationConfig,
    ): PrayerSchedule
}
