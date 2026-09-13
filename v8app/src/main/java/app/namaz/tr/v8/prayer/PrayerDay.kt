package app.namaz.tr.v8.prayer

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class PrayerDay(
    val date: LocalDate,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val zoneId: ZoneId,
    val times: List<PrayerTime>,
    val calculationLabel: String = "Astronomik hesaplama"
) {
    init {
        require(city.isNotBlank()) { "city must not be blank" }
        require(latitude in -90.0..90.0) { "latitude out of range" }
        require(longitude in -180.0..180.0) { "longitude out of range" }
        require(times.map { it.prayer }.toSet().size == times.size) { "duplicate prayer time" }
        require(times.all { it.at.toLocalDate() == date }) { "prayer times must belong to date" }
    }

    fun time(prayer: Prayer): PrayerTime =
        requireNotNull(times.firstOrNull { it.prayer == prayer }) { "missing ${prayer.name}" }

    fun timeOf(prayer: Prayer): LocalTime = time(prayer).at.toLocalTime()
}
