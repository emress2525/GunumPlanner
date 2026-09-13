package app.namaz.tr.v8.prayer

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.prayerSettingsDataStore by preferencesDataStore(name = "v8_prayer_settings")

data class PrayerSettings(
    val city: String = "Ankara",
    val latitude: Double = 39.9334,
    val longitude: Double = 32.8597,
    val zoneId: ZoneId = ZoneId.of("Europe/Istanbul"),
    val asrMethod: AsrMethod = AsrMethod.HANAFI,
    val offsetsMinutes: Map<Prayer, Int> = emptyMap()
)

class PrayerSettingsStore(private val context: Context) {
    private object Keys {
        val city = stringPreferencesKey("city")
        val latitude = doublePreferencesKey("latitude")
        val longitude = doublePreferencesKey("longitude")
        val zoneId = stringPreferencesKey("zone_id")
        val asrMethod = stringPreferencesKey("asr_method")

        fun offset(prayer: Prayer) = intPreferencesKey("offset_${prayer.name.lowercase()}")
    }

    val settings: Flow<PrayerSettings> = context.prayerSettingsDataStore.data.map { prefs ->
        val offsets = Prayer.entries.associateWith { prayer ->
            prefs[Keys.offset(prayer)] ?: 0
        }.filterValues { it != 0 }

        PrayerSettings(
            city = prefs[Keys.city] ?: "Ankara",
            latitude = prefs[Keys.latitude] ?: 39.9334,
            longitude = prefs[Keys.longitude] ?: 32.8597,
            zoneId = runCatching { ZoneId.of(prefs[Keys.zoneId] ?: "Europe/Istanbul") }
                .getOrDefault(ZoneId.of("Europe/Istanbul")),
            asrMethod = prefs[Keys.asrMethod]
                ?.let { saved -> AsrMethod.entries.firstOrNull { it.name == saved } }
                ?: AsrMethod.HANAFI,
            offsetsMinutes = offsets
        )
    }

    suspend fun setLocation(
        city: String,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId
    ) {
        require(city.isNotBlank())
        require(latitude in -90.0..90.0)
        require(longitude in -180.0..180.0)
        context.prayerSettingsDataStore.edit { prefs ->
            prefs[Keys.city] = city.trim()
            prefs[Keys.latitude] = latitude
            prefs[Keys.longitude] = longitude
            prefs[Keys.zoneId] = zoneId.id
        }
    }

    suspend fun setAsrMethod(method: AsrMethod) {
        context.prayerSettingsDataStore.edit { it[Keys.asrMethod] = method.name }
    }

    suspend fun setOffset(prayer: Prayer, minutes: Int) {
        require(minutes in -120..120) { "Manual offset must be between -120 and +120 minutes" }
        context.prayerSettingsDataStore.edit { prefs ->
            if (minutes == 0) prefs.remove(Keys.offset(prayer))
            else prefs[Keys.offset(prayer)] = minutes
        }
    }
}
