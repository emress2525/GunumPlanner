package app.namaz.tr.v8.alarm

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.namaz.tr.v8.model.PrayerName
import kotlinx.coroutines.flow.first

private val Context.alarmPreferencesDataStore by preferencesDataStore(name = "prayer_alarm_preferences")

interface PrayerAlarmPreferencesStore {
    suspend fun current(): AlarmPreferences
    suspend fun setMode(prayer: PrayerName, mode: AdhanMode)
    suspend fun setPreReminder(prayer: PrayerName, minutes: Int)
}

class DataStorePrayerAlarmPreferencesStore(private val context: Context) : PrayerAlarmPreferencesStore {
    private fun modeKey(prayer: PrayerName) = stringPreferencesKey("mode_${prayer.name.lowercase()}")
    private fun reminderKey(prayer: PrayerName) = intPreferencesKey("pre_${prayer.name.lowercase()}")

    override suspend fun current(): AlarmPreferences {
        val prefs = context.alarmPreferencesDataStore.data.first()
        val map = PrayerName.entries.filter { it.isTrackable }.associateWith { prayer ->
            PrayerAlarmPreference(
                mode = prefs[modeKey(prayer)]?.let { runCatching { AdhanMode.valueOf(it) }.getOrNull() } ?: AdhanMode.FULL,
                preReminderMinutes = (prefs[reminderKey(prayer)] ?: 10).coerceIn(0, 60),
            )
        }
        return AlarmPreferences(map)
    }

    override suspend fun setMode(prayer: PrayerName, mode: AdhanMode) {
        require(prayer.isTrackable)
        context.alarmPreferencesDataStore.edit { it[modeKey(prayer)] = mode.name }
    }

    override suspend fun setPreReminder(prayer: PrayerName, minutes: Int) {
        require(prayer.isTrackable)
        require(minutes in 0..60)
        context.alarmPreferencesDataStore.edit { it[reminderKey(prayer)] = minutes }
    }
}
