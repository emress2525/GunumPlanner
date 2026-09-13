package app.namaz.tr.v8.prayer.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerRecordDao {
    @Query("SELECT * FROM prayer_records WHERE localDate = :localDate ORDER BY prayerName")
    fun observeDay(localDate: String): Flow<List<PrayerRecordEntity>>

    @Query("SELECT * FROM prayer_records WHERE localDate = :localDate AND prayerName = :prayerName LIMIT 1")
    suspend fun get(localDate: String, prayerName: String): PrayerRecordEntity?

    @Upsert
    suspend fun upsert(entity: PrayerRecordEntity)
}
