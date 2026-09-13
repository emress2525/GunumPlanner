package app.namaz.tr.v8.model

import java.time.Instant
import java.time.LocalDate

enum class PrayerName(val displayName: String, val isTrackable: Boolean) {
    FAJR("Sabah", true),
    SUNRISE("Güneş", false),
    DHUHR("Öğle", true),
    ASR("İkindi", true),
    MAGHRIB("Akşam", true),
    ISHA("Yatsı", true),
}

data class PrayerLocation(
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val zoneId: String,
)

enum class PrayerMethod {
    TURKEY_APPROX,
    MUSLIM_WORLD_LEAGUE,
    EGYPTIAN,
    KARACHI,
    UMM_AL_QURA,
}

enum class MadhabChoice { HANAFI, SHAFI }

data class PrayerCalculationConfig(
    val method: PrayerMethod = PrayerMethod.TURKEY_APPROX,
    val madhab: MadhabChoice = MadhabChoice.HANAFI,
    val adjustmentsMinutes: Map<PrayerName, Int> = emptyMap(),
)

data class PrayerInstant(
    val name: PrayerName,
    val instant: Instant,
)

data class PrayerSchedule(
    val date: LocalDate,
    val zoneId: String,
    val location: PrayerLocation,
    val methodLabel: String,
    val prayers: List<PrayerInstant>,
) {
    init {
        require(prayers.map { it.name }.toSet().size == prayers.size) { "Prayer names must be unique" }
    }

    val fajr: PrayerInstant get() = requirePrayer(PrayerName.FAJR)
    val sunrise: PrayerInstant get() = requirePrayer(PrayerName.SUNRISE)
    val dhuhr: PrayerInstant get() = requirePrayer(PrayerName.DHUHR)
    val asr: PrayerInstant get() = requirePrayer(PrayerName.ASR)
    val maghrib: PrayerInstant get() = requirePrayer(PrayerName.MAGHRIB)
    val isha: PrayerInstant get() = requirePrayer(PrayerName.ISHA)

    val trackablePrayers: List<PrayerInstant>
        get() = prayers.filter { it.name.isTrackable }

    fun withAdjustments(minutes: Map<PrayerName, Int>): PrayerSchedule = copy(
        prayers = prayers.map { prayer ->
            val offset = minutes[prayer.name] ?: 0
            prayer.copy(instant = prayer.instant.plusSeconds(offset.toLong() * 60L))
        },
    )

    fun nextPrayer(after: Instant): PrayerInstant? = trackablePrayers
        .filter { it.instant.isAfter(after) }
        .minByOrNull { it.instant }

    private fun requirePrayer(name: PrayerName): PrayerInstant =
        prayers.firstOrNull { it.name == name } ?: error("Missing prayer: $name")
}

data class PrayerCompletion(
    val completed: Boolean,
    val onTime: Boolean? = null,
    val congregation: Boolean? = null,
    val qaza: Boolean? = null,
)

data class QazaState(val userEntered: Int) {
    init { require(userEntered >= 0) }
    val total: Int get() = userEntered
}
