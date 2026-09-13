package app.namaz.tr.v8.model

enum class UserProfile {
    RELIGION_FROM_ZERO,
    NEW_TO_PRAYER,
    LEARN_QURAN,
    DAILY_WORSHIP,
}

enum class AppMode { SIMPLE, FULL }

enum class AppDestination(val title: String, val route: String) {
    TODAY("Bugün", "today"),
    QURAN("Kur’an", "quran"),
    LEARN("Öğren", "learn"),
    WORSHIP("İbadet", "worship"),
    MORE("Daha Fazla", "more"),
}

data class UserSettings(
    val profile: UserProfile? = null,
    val mode: AppMode = AppMode.SIMPLE,
    val onboardingDone: Boolean = false,
)

fun defaultModeFor(profile: UserProfile): AppMode = AppMode.SIMPLE

fun availableDestinations(profile: UserProfile): List<AppDestination> = AppDestination.entries
