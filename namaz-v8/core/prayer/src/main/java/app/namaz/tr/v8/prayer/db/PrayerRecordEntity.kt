package app.namaz.tr.v8.prayer.db

import androidx.room.Entity

@Entity(
    tableName = "prayer_records",
    primaryKeys = ["localDate", "prayerName"],
)
data class PrayerRecordEntity(
    val localDate: String,
    val prayerName: String,
    val completed: Boolean,
    val onTime: Boolean? = null,
    val congregation: Boolean? = null,
    val qaza: Boolean? = null,
)
