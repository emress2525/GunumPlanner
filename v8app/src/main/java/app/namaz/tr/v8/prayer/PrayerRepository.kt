package app.namaz.tr.v8.prayer

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class PrayerSchedulePair(
    val today: PrayerDay,
    val tomorrow: PrayerDay
)

class PrayerRepository(
    private val settingsStore: PrayerSettingsStore,
    private val calculator: SolarPrayerTimesCalculator = SolarPrayerTimesCalculator(),
    private val scheduleEngine: PrayerScheduleEngine = PrayerScheduleEngine()
) {
    val settings: Flow<PrayerSettings> = settingsStore.settings

    fun schedulesFor(date: LocalDate): Flow<PrayerSchedulePair> = settings.map { settings ->
        PrayerSchedulePair(
            today = calculate(date, settings),
            tomorrow = calculate(date.plusDays(1), settings)
        )
    }

    suspend fun setLocation(city: String, latitude: Double, longitude: Double, zoneId: java.time.ZoneId) {
        settingsStore.setLocation(city, latitude, longitude, zoneId)
    }

    suspend fun setAsrMethod(method: AsrMethod) {
        settingsStore.setAsrMethod(method)
    }

    suspend fun setOffset(prayer: Prayer, minutes: Int) {
        settingsStore.setOffset(prayer, minutes)
    }

    private fun calculate(date: LocalDate, settings: PrayerSettings): PrayerDay {
        val raw = calculator.calculate(
            date = date,
            city = settings.city,
            latitude = settings.latitude,
            longitude = settings.longitude,
            zoneId = settings.zoneId,
            asrMethod = settings.asrMethod
        )
        return scheduleEngine.applyOffsets(raw, settings.offsetsMinutes)
    }
}
