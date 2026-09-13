package app.namaz.tr.v8.prayerui

import app.namaz.tr.v8.model.PrayerCompletion
import app.namaz.tr.v8.model.PrayerName
import app.namaz.tr.v8.prayer.PrayerRepository
import app.namaz.tr.v8.prayer.QazaStore
import java.time.LocalDate

class PrayerTrackingViewModel(
    private val repository: PrayerRepository,
    private val qazaStore: QazaStore,
) {
    val qazaTotal = qazaStore.total

    suspend fun setSimple(date: LocalDate, prayer: PrayerName, completed: Boolean) {
        repository.setCompleted(date, prayer, completed)
    }

    suspend fun setDetailed(
        date: LocalDate,
        prayer: PrayerName,
        completed: Boolean,
        onTime: Boolean?,
        congregation: Boolean?,
        qaza: Boolean?,
    ) {
        repository.setCompletion(date, prayer, PrayerCompletion(completed, onTime, congregation, qaza))
    }

    suspend fun setUserEnteredQazaTotal(value: Int) = qazaStore.setTotal(value)
}
