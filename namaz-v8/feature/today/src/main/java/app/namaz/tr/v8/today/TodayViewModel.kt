package app.namaz.tr.v8.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.namaz.tr.v8.model.PrayerCompletion
import app.namaz.tr.v8.model.PrayerInstant
import app.namaz.tr.v8.model.PrayerName
import app.namaz.tr.v8.model.PrayerSchedule
import app.namaz.tr.v8.prayer.PrayerRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class NextPrayerUi(val name: String, val timeText: String, val remainingText: String, val instant: Instant)
data class PrayerRowUi(val name: PrayerName, val title: String, val timeText: String)
data class TodayUiState(
    val city: String,
    val methodLabel: String,
    val nextPrayer: NextPrayerUi?,
    val trackablePrayers: List<PrayerRowUi>,
    val sunrise: PrayerRowUi?,
    val completed: Set<PrayerName> = emptySet(),
    val hijriPlaceholder: String? = null,
)

class TodayUiStateMapper {
    fun map(
        schedule: PrayerSchedule,
        now: Instant,
        completions: Map<PrayerName, PrayerCompletion> = emptyMap(),
    ): TodayUiState {
        val zone = ZoneId.of(schedule.zoneId)
        fun row(prayer: PrayerInstant) = PrayerRowUi(
            name = prayer.name,
            title = prayer.name.displayName,
            timeText = prayer.instant.atZone(zone).toLocalTime().toString().take(5),
        )
        val next = schedule.nextPrayer(now)
        return TodayUiState(
            city = schedule.location.city,
            methodLabel = schedule.methodLabel,
            nextPrayer = next?.let { prayer ->
                NextPrayerUi(prayer.name.displayName, row(prayer).timeText, remaining(now, prayer.instant), prayer.instant)
            },
            trackablePrayers = schedule.trackablePrayers.map(::row),
            sunrise = row(schedule.sunrise),
            completed = completions.filterValues { it.completed }.keys,
        )
    }

    private fun remaining(now: Instant, target: Instant): String {
        val duration = Duration.between(now, target).coerceAtLeast(Duration.ZERO)
        val hours = duration.toHours()
        val minutes = duration.minusHours(hours).toMinutes()
        return if (hours > 0) "${hours} sa ${minutes} dk" else "${minutes} dk"
    }
}

class TodayViewModel(
    private val repository: PrayerRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val zoneId: ZoneId = ZoneId.of("Europe/Istanbul"),
    private val mapper: TodayUiStateMapper = TodayUiStateMapper(),
) : ViewModel() {
    private val _state = MutableStateFlow<TodayUiState?>(null)
    val state: StateFlow<TodayUiState?> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val date = LocalDate.now(clock.withZone(zoneId))
            repository.observeDay(date)
                .combine(repository.observeCompletions(date)) { schedule, completions ->
                    schedule?.let { mapper.map(it, clock.instant(), completions) }
                }
                .collect { _state.value = it }
        }
    }

    fun setCompleted(prayer: PrayerName, completed: Boolean) {
        if (!prayer.isTrackable) return
        viewModelScope.launch {
            repository.setCompleted(LocalDate.now(clock.withZone(zoneId)), prayer, completed)
        }
    }
}
