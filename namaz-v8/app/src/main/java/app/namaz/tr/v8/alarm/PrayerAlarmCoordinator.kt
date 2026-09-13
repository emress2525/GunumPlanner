package app.namaz.tr.v8.alarm

import android.content.Context
import app.namaz.tr.v8.AppGraph
import java.time.LocalDate
import java.time.ZoneId

class PrayerAlarmCoordinator(
    context: Context,
    private val graph: AppGraph,
    private val scheduler: PrayerAlarmScheduler = PrayerAlarmScheduler(context.applicationContext),
) {
    suspend fun rescheduleUpcoming() {
        val runtime = graph.prayerSettings.currentSettings()
        val zone = ZoneId.of(runtime.location.zoneId)
        val today = LocalDate.now(zone)
        val preferences = graph.alarmPreferences.current()
        scheduler.schedule(graph.prayerRepository.schedule(today), preferences)
        scheduler.schedule(graph.prayerRepository.schedule(today.plusDays(1)), preferences)
    }
}
