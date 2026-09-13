package app.namaz.tr.v8.prayer.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDayDao {
    @Query("SELECT * FROM prayer_days WHERE localDate = :localDate AND cacheKey = :cacheKey LIMIT 1")
    fun observe(localDate: String, cacheKey: String): Flow<PrayerDayEntity?>

    @Query("SELECT * FROM prayer_days WHERE localDate = :localDate AND cacheKey = :cacheKey LIMIT 1")
    suspend fun get(localDate: String, cacheKey: String): PrayerDayEntity?

    @Upsert
    suspend fun upsert(entity: PrayerDayEntity)

    @Upsert
    suspend fun upsertAll(entities: List<PrayerDayEntity>)

    @Query("DELETE FROM prayer_days WHERE localDate < :oldestLocalDate")
    suspend fun deleteBefore(oldestLocalDate: String)
}
