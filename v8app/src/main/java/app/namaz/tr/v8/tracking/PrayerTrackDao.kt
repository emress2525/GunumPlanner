package app.namaz.tr.v8.tracking

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerTrackDao {
    @Query("SELECT * FROM prayer_track WHERE dateIso = :dateIso")
    fun observeDate(dateIso: String): Flow<List<PrayerTrackEntity>>

    @Upsert
    suspend fun upsert(entity: PrayerTrackEntity)

    @Query("DELETE FROM prayer_track WHERE dateIso = :dateIso AND prayer = :prayer")
    suspend fun clear(dateIso: String, prayer: String)
}
