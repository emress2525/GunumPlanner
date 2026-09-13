package app.namaz.tr.v8.tracking

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PrayerTrackEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NamazDatabase : RoomDatabase() {
    abstract fun prayerTrackDao(): PrayerTrackDao

    companion object {
        @Volatile
        private var instance: NamazDatabase? = null

        fun get(context: Context): NamazDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NamazDatabase::class.java,
                    "namaz-v8.db"
                ).build().also { instance = it }
            }
    }
}
