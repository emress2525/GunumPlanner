package app.namaz.tr.v8.tracking

import androidx.room.Entity

@Entity(
    tableName = "prayer_track",
    primaryKeys = ["dateIso", "prayer"]
)
data class PrayerTrackEntity(
    val dateIso: String,
    val prayer: String,
    val state: String
)
