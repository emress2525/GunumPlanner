package app.namaz.tr.v8.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.namaz.tr.v8.model.AppMode
import app.namaz.tr.v8.model.UserProfile
import app.namaz.tr.v8.model.UserSettings
import app.namaz.tr.v8.model.defaultModeFor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userSettingsDataStore by preferencesDataStore(name = "user_settings")

interface UserSettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun setProfile(profile: UserProfile)
    suspend fun setMode(mode: AppMode)
    suspend fun resetOnboarding()
}

class DataStoreUserSettingsRepository(
    private val context: Context,
) : UserSettingsRepository {
    private object Keys {
        val PROFILE = stringPreferencesKey("profile")
        val APP_MODE = stringPreferencesKey("app_mode")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    override val settings: Flow<UserSettings> = context.userSettingsDataStore.data.map { prefs ->
        val profile = prefs[Keys.PROFILE]?.let { value ->
            runCatching { UserProfile.valueOf(value) }.getOrNull()
        }
        val mode = prefs[Keys.APP_MODE]?.let { value ->
            runCatching { AppMode.valueOf(value) }.getOrNull()
        } ?: profile?.let(::defaultModeFor) ?: AppMode.SIMPLE
        UserSettings(
            profile = profile,
            mode = mode,
            onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false,
        )
    }

    override suspend fun setProfile(profile: UserProfile) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.PROFILE] = profile.name
            if (prefs[Keys.APP_MODE] == null) prefs[Keys.APP_MODE] = defaultModeFor(profile).name
            prefs[Keys.ONBOARDING_DONE] = true
        }
    }

    override suspend fun setMode(mode: AppMode) {
        context.userSettingsDataStore.edit { it[Keys.APP_MODE] = mode.name }
    }

    override suspend fun resetOnboarding() {
        context.userSettingsDataStore.edit { it[Keys.ONBOARDING_DONE] = false }
    }
}
