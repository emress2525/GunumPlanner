package app.namaz.tr.v8

import android.content.Context
import androidx.room.Room
import app.namaz.tr.v8.prayer.AdhanPrayerCalculator
import app.namaz.tr.v8.prayer.DataStorePrayerSettingsStore
import app.namaz.tr.v8.prayer.DefaultPrayerRepository
import app.namaz.tr.v8.prayer.PrayerRepository
import app.namaz.tr.v8.prayer.PrayerSettingsStore
import app.namaz.tr.v8.prayer.db.PrayerDatabase

class AppGraph(context: Context) {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(appContext, PrayerDatabase::class.java, "namaz-v8.db")
        .fallbackToDestructiveMigration()
        .build()

    val prayerSettings: PrayerSettingsStore = DataStorePrayerSettingsStore(appContext)
    val prayerRepository: PrayerRepository = DefaultPrayerRepository(
        dao = database.prayerDayDao(),
        calculator = AdhanPrayerCalculator(),
        settings = prayerSettings,
    )
}
