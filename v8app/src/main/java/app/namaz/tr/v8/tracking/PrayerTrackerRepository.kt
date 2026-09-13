package app.namaz.tr.v8.tracking

import app.namaz.tr.v8.prayer.Prayer
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PrayerTrackerRepository(private val dao: PrayerTrackDao) {
    fun observe(date: LocalDate): Flow<Map<Prayer, PrayerTrackState>> =
        dao.observeDate(date.toString()).map { rows ->
            rows.mapNotNull { row ->
                val prayer = Prayer.entries.firstOrNull { it.name == row.prayer }
                val state = PrayerTrackState.entries.firstOrNull { it.name == row.state }
                if (prayer == null || state == null) null else prayer to state
            }.toMap()
        }

    suspend fun set(date: LocalDate, prayer: Prayer, state: PrayerTrackState) {
        require(prayer.isTrackable) { "Sunrise cannot be tracked as prayer" }
        if (state == PrayerTrackState.UNSET) {
            dao.clear(date.toString(), prayer.name)
        } else {
            dao.upsert(
                PrayerTrackEntity(
                    dateIso = date.toString(),
                    prayer = prayer.name,
                    state = state.name
                )
            )
        }
    }
}
