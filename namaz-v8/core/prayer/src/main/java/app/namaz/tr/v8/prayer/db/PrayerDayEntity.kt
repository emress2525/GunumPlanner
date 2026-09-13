package app.namaz.tr.v8.prayer.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.namaz.tr.v8.model.PrayerInstant
import app.namaz.tr.v8.model.PrayerLocation
import app.namaz.tr.v8.model.PrayerName
import app.namaz.tr.v8.model.PrayerSchedule
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "prayer_days",
    indices = [Index(value = ["localDate", "cacheKey"], unique = true)],
)
data class PrayerDayEntity(
    @PrimaryKey val id: String,
    val localDate: String,
    val cacheKey: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val zoneId: String,
    val methodLabel: String,
    val fajrEpochMs: Long,
    val sunriseEpochMs: Long,
    val dhuhrEpochMs: Long,
    val asrEpochMs: Long,
    val maghribEpochMs: Long,
    val ishaEpochMs: Long,
    val generatedAtEpochMs: Long,
) {
    fun toSchedule(): PrayerSchedule = PrayerSchedule(
        date = LocalDate.parse(localDate),
        zoneId = zoneId,
        location = PrayerLocation(city, latitude, longitude, zoneId),
        methodLabel = methodLabel,
        prayers = listOf(
            PrayerInstant(PrayerName.FAJR, Instant.ofEpochMilli(fajrEpochMs)),
            PrayerInstant(PrayerName.SUNRISE, Instant.ofEpochMilli(sunriseEpochMs)),
            PrayerInstant(PrayerName.DHUHR, Instant.ofEpochMilli(dhuhrEpochMs)),
            PrayerInstant(PrayerName.ASR, Instant.ofEpochMilli(asrEpochMs)),
            PrayerInstant(PrayerName.MAGHRIB, Instant.ofEpochMilli(maghribEpochMs)),
            PrayerInstant(PrayerName.ISHA, Instant.ofEpochMilli(ishaEpochMs)),
        ),
    )

    companion object {
        fun from(schedule: PrayerSchedule, cacheKey: String, generatedAtEpochMs: Long): PrayerDayEntity = PrayerDayEntity(
            id = "${schedule.date}|$cacheKey",
            localDate = schedule.date.toString(),
            cacheKey = cacheKey,
            city = schedule.location.city,
            latitude = schedule.location.latitude,
            longitude = schedule.location.longitude,
            zoneId = schedule.location.zoneId,
            methodLabel = schedule.methodLabel,
            fajrEpochMs = schedule.fajr.instant.toEpochMilli(),
            sunriseEpochMs = schedule.sunrise.instant.toEpochMilli(),
            dhuhrEpochMs = schedule.dhuhr.instant.toEpochMilli(),
            asrEpochMs = schedule.asr.instant.toEpochMilli(),
            maghribEpochMs = schedule.maghrib.instant.toEpochMilli(),
            ishaEpochMs = schedule.isha.instant.toEpochMilli(),
            generatedAtEpochMs = generatedAtEpochMs,
        )
    }
}
