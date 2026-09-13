package app.namaz.tr.v8.prayer.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PrayerDayEntity::class, PrayerRecordEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class PrayerDatabase : RoomDatabase() {
    abstract fun prayerDayDao(): PrayerDayDao
    abstract fun prayerRecordDao(): PrayerRecordDao
}
