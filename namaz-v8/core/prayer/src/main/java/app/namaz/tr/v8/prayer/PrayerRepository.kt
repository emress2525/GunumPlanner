package app.namaz.tr.v8.prayer

import app.namaz.tr.v8.model.PrayerCompletion
import app.namaz.tr.v8.model.PrayerName
import app.namaz.tr.v8.model.PrayerSchedule
import app.namaz.tr.v8.prayer.db.PrayerDayDao
import app.namaz.tr.v8.prayer.db.PrayerDayEntity
import app.namaz.tr.v8.prayer.db.PrayerRecordDao
import app.namaz.tr.v8.prayer.db.PrayerRecordEntity
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

interface PrayerRepository {
    fun observeDay(date: LocalDate): Flow<PrayerSchedule?>
    fun observeCompletions(date: LocalDate): Flow<Map<PrayerName, PrayerCompletion>>
    suspend fun schedule(date: LocalDate): PrayerSchedule
    suspend fun refresh(range: ClosedRange<LocalDate>): Result<Unit>
    suspend fun setCompleted(date: LocalDate, prayer: PrayerName, completed: Boolean)
    suspend fun setCompletion(date: LocalDate, prayer: PrayerName, completion: PrayerCompletion)
}

class DefaultPrayerRepository(
    private val dao: PrayerDayDao,
    private val recordDao: PrayerRecordDao,
    private val calculator: PrayerCalculator,
    private val settings: PrayerSettingsStore,
    private val clock: Clock = Clock.systemUTC(),
) : PrayerRepository {
    override fun observeDay(date: LocalDate): Flow<PrayerSchedule?> = flow { emit(schedule(date)) }

    override fun observeCompletions(date: LocalDate): Flow<Map<PrayerName, PrayerCompletion>> =
        recordDao.observeDay(date.toString()).map { rows ->
            rows.mapNotNull { row ->
                val prayer = runCatching { PrayerName.valueOf(row.prayerName) }.getOrNull() ?: return@mapNotNull null
                if (!prayer.isTrackable) return@mapNotNull null
                prayer to PrayerCompletion(row.completed, row.onTime, row.congregation, row.qaza)
            }.toMap()
        }

    override suspend fun schedule(date: LocalDate): PrayerSchedule {
        val runtime = settings.currentSettings()
        val cacheKey = cacheKey(runtime)
        val raw = dao.get(date.toString(), cacheKey)?.toSchedule() ?: run {
            val calculated = calculator.calculate(
                date = date,
                location = runtime.location,
                config = runtime.config.copy(adjustmentsMinutes = emptyMap()),
            )
            dao.upsert(PrayerDayEntity.from(calculated, cacheKey, clock.millis()))
            calculated
        }
        return raw.withAdjustments(runtime.config.adjustmentsMinutes)
    }

    override suspend fun refresh(range: ClosedRange<LocalDate>): Result<Unit> = runCatching {
        val runtime = settings.currentSettings()
        val cacheKey = cacheKey(runtime)
        var date = range.start
        val entities = mutableListOf<PrayerDayEntity>()
        while (!date.isAfter(range.endInclusive)) {
            val raw = calculator.calculate(date, runtime.location, runtime.config.copy(adjustmentsMinutes = emptyMap()))
            entities += PrayerDayEntity.from(raw, cacheKey, clock.millis())
            date = date.plusDays(1)
        }
        dao.upsertAll(entities)
    }

    override suspend fun setCompleted(date: LocalDate, prayer: PrayerName, completed: Boolean) {
        require(prayer.isTrackable) { "Sunrise is not a trackable prayer" }
        val old = recordDao.get(date.toString(), prayer.name)
        recordDao.upsert(
            PrayerRecordEntity(
                localDate = date.toString(),
                prayerName = prayer.name,
                completed = completed,
                onTime = old?.onTime,
                congregation = old?.congregation,
                qaza = old?.qaza,
            ),
        )
    }

    override suspend fun setCompletion(date: LocalDate, prayer: PrayerName, completion: PrayerCompletion) {
        require(prayer.isTrackable) { "Sunrise is not a trackable prayer" }
        recordDao.upsert(
            PrayerRecordEntity(
                localDate = date.toString(),
                prayerName = prayer.name,
                completed = completion.completed,
                onTime = completion.onTime,
                congregation = completion.congregation,
                qaza = completion.qaza,
            ),
        )
    }

    private fun cacheKey(settings: PrayerRuntimeSettings): String = buildString {
        append(settings.location.latitude)
        append(',')
        append(settings.location.longitude)
        append('|')
        append(settings.config.method.name)
        append('|')
        append(settings.config.madhab.name)
    }
}
