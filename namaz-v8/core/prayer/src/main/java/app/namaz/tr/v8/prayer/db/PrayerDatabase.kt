package app.namaz.tr.v8.prayer.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PrayerDayEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PrayerDatabase : RoomDatabase() {
    abstract fun prayerDayDao(): PrayerDayDao
}
