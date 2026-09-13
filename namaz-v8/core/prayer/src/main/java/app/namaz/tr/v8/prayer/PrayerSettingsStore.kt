package app.namaz.tr.v8.prayer

import app.namaz.tr.v8.model.PrayerCalculationConfig
import app.namaz.tr.v8.model.PrayerLocation

data class PrayerRuntimeSettings(
    val location: PrayerLocation,
    val config: PrayerCalculationConfig,
)

interface PrayerSettingsStore {
    suspend fun currentSettings(): PrayerRuntimeSettings
}
