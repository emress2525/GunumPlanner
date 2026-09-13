package app.namaz.tr.v8.onboarding

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.namaz.tr.v8.app.AppMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(name = "v8_onboarding")

data class OnboardingState(
    val profile: UserProfile?,
    val mode: AppMode
)

class OnboardingRepository(private val context: Context) {
    private object Keys {
        val profile = stringPreferencesKey("profile")
        val mode = stringPreferencesKey("mode")
    }

    val state: Flow<OnboardingState> = context.onboardingDataStore.data.map { preferences ->
        val profile = preferences[Keys.profile]
            ?.let { stored -> UserProfile.entries.firstOrNull { it.name == stored } }
        val mode = preferences[Keys.mode]
            ?.let { stored -> AppMode.entries.firstOrNull { it.name == stored } }
            ?: AppMode.SIMPLE
        OnboardingState(profile = profile, mode = mode)
    }

    suspend fun complete(profile: UserProfile, mode: AppMode) {
        context.onboardingDataStore.edit { preferences ->
            preferences[Keys.profile] = profile.name
            preferences[Keys.mode] = mode.name
        }
    }

    suspend fun setMode(mode: AppMode) {
        context.onboardingDataStore.edit { preferences ->
            preferences[Keys.mode] = mode.name
        }
    }
}
