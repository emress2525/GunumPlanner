package app.namaz.tr.v8.prayer

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.namaz.tr.v8.model.MadhabChoice
import app.namaz.tr.v8.model.PrayerCalculationConfig
import app.namaz.tr.v8.model.PrayerLocation
import app.namaz.tr.v8.model.PrayerMethod
import app.namaz.tr.v8.model.PrayerName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.prayerSettingsDataStore by preferencesDataStore(name = "prayer_settings")

data class PrayerRuntimeSettings(
    val location: PrayerLocation,
    val config: PrayerCalculationConfig,
)

interface PrayerSettingsStore {
    val settings: Flow<PrayerRuntimeSettings>
    suspend fun currentSettings(): PrayerRuntimeSettings
    suspend fun setLocation(location: PrayerLocation)
    suspend fun setMethod(method: PrayerMethod)
    suspend fun setMadhab(madhab: MadhabChoice)
    suspend fun setAdjustment(prayer: PrayerName, minutes: Int)
}

class DataStorePrayerSettingsStore(
    private val context: Context,
) : PrayerSettingsStore {
    private object Keys {
        val CITY = stringPreferencesKey("city")
        val LAT = stringPreferencesKey("latitude")
        val LON = stringPreferencesKey("longitude")
        val ZONE = stringPreferencesKey("zone_id")
        val METHOD = stringPreferencesKey("method")
        val MADHAB = stringPreferencesKey("madhab")
        fun adjustment(prayer: PrayerName) = intPreferencesKey("adjustment_${prayer.name.lowercase()}")
    }

    override val settings: Flow<PrayerRuntimeSettings> = context.prayerSettingsDataStore.data.map { prefs ->
        val method = prefs[Keys.METHOD]?.let { runCatching { PrayerMethod.valueOf(it) }.getOrNull() }
            ?: PrayerMethod.TURKEY_APPROX
        val madhab = prefs[Keys.MADHAB]?.let { runCatching { MadhabChoice.valueOf(it) }.getOrNull() }
            ?: MadhabChoice.HANAFI
        val location = PrayerLocation(
            city = prefs[Keys.CITY] ?: "Ankara",
            latitude = prefs[Keys.LAT]?.toDoubleOrNull() ?: 39.9334,
            longitude = prefs[Keys.LON]?.toDoubleOrNull() ?: 32.8597,
            zoneId = prefs[Keys.ZONE] ?: "Europe/Istanbul",
        )
        val adjustments = PrayerName.entries.associateWith { prefs[Keys.adjustment(it)] ?: 0 }
        PrayerRuntimeSettings(
            location = location,
            config = PrayerCalculationConfig(method, madhab, adjustments),
        )
    }

    override suspend fun currentSettings(): PrayerRuntimeSettings = settings.first()

    override suspend fun setLocation(location: PrayerLocation) {
        context.prayerSettingsDataStore.edit { prefs ->
            prefs[Keys.CITY] = location.city
            prefs[Keys.LAT] = location.latitude.toString()
            prefs[Keys.LON] = location.longitude.toString()
            prefs[Keys.ZONE] = location.zoneId
        }
    }

    override suspend fun setMethod(method: PrayerMethod) {
        context.prayerSettingsDataStore.edit { it[Keys.METHOD] = method.name }
    }

    override suspend fun setMadhab(madhab: MadhabChoice) {
        context.prayerSettingsDataStore.edit { it[Keys.MADHAB] = madhab.name }
    }

    override suspend fun setAdjustment(prayer: PrayerName, minutes: Int) {
        require(minutes in -120..120) { "Manual adjustment must be between -120 and 120 minutes" }
        context.prayerSettingsDataStore.edit { it[Keys.adjustment(prayer)] = minutes }
    }
}
