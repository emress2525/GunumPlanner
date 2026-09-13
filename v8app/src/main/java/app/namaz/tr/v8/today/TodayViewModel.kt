package app.namaz.tr.v8.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.namaz.tr.v8.prayer.Prayer
import app.namaz.tr.v8.prayer.PrayerRepository
import app.namaz.tr.v8.prayer.PrayerScheduleEngine
import app.namaz.tr.v8.prayer.PrayerSettingsStore
import app.namaz.tr.v8.tracking.NamazDatabase
import app.namaz.tr.v8.tracking.PrayerTrackState
import app.namaz.tr.v8.tracking.PrayerTrackerRepository
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TodayScreenState(
    val data: TodayUiState? = null,
    val errorMessage: String? = null
)

class TodayViewModel(application: Application) : AndroidViewModel(application) {
    private val prayerRepository = PrayerRepository(PrayerSettingsStore(application))
    private val trackerRepository = PrayerTrackerRepository(
        NamazDatabase.get(application).prayerTrackDao()
    )
    private val factory = TodayStateFactory(PrayerScheduleEngine())
    private val now = MutableStateFlow(Instant.now())

    init {
        viewModelScope.launch {
            while (isActive) {
                now.value = Instant.now()
                delay(1_000)
            }
        }
    }

    val state = combine(prayerRepository.settings, now) { settings, instant ->
        instant.atZone(settings.zoneId)
    }.flatMapLatest { zonedNow ->
        val date = zonedNow.toLocalDate()
        combine(
            prayerRepository.schedulesFor(date),
            trackerRepository.observe(date)
        ) { schedules, tracking ->
            TodayScreenState(
                data = factory.create(
                    today = schedules.today,
                    tomorrow = schedules.tomorrow,
                    now = zonedNow,
                    tracking = tracking
                )
            )
        }
    }.catch { throwable ->
        emit(
            TodayScreenState(
                errorMessage = throwable.message ?: "Namaz vakitleri hazırlanamadı."
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayScreenState()
    )

    fun togglePrayed(prayer: Prayer) {
        val data = state.value.data ?: return
        val row = data.prayerRows.firstOrNull { it.prayer == prayer } ?: return
        val nextState = if (row.trackState == PrayerTrackState.UNSET) {
            PrayerTrackState.PRAYED
        } else {
            PrayerTrackState.UNSET
        }
        setTrackState(prayer, nextState)
    }

    fun setTrackState(prayer: Prayer, trackState: PrayerTrackState) {
        val date = state.value.data?.date ?: return
        viewModelScope.launch {
            trackerRepository.set(date, prayer, trackState)
        }
    }
}
