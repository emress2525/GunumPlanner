package app.namaz.tr.v8.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.namaz.tr.v8.model.PrayerSchedule

class PrayerAlarmScheduler(
    private val context: Context,
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java),
    private val planner: PrayerAlarmPlanner = PrayerAlarmPlanner(),
) {
    fun schedule(schedule: PrayerSchedule, preferences: AlarmPreferences) {
        planner.plan(schedule, preferences).forEach(::schedule)
    }

    fun scheduleDiagnostic(atMillis: Long) {
        schedule(PlannedAlarm(java.time.LocalDate.now(), null, AlarmKind.DIAGNOSTIC, atMillis, AdhanMode.NOTIFICATION))
    }

    fun exactCapabilityAvailable(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun schedule(alarm: PlannedAlarm) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra(EXTRA_PRAYER, alarm.prayer?.name)
            putExtra(EXTRA_KIND, alarm.kind.name)
            putExtra(EXTRA_MODE, alarm.mode.name)
            putExtra(EXTRA_DATE, alarm.date.toString())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(alarm),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (exactCapabilityAvailable()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.atMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.atMillis, pendingIntent)
        }
    }

    private fun requestCode(alarm: PlannedAlarm): Int =
        ("${alarm.date}|${alarm.prayer?.name ?: "diagnostic"}|${alarm.kind.name}".hashCode() and 0x7fffffff)

    companion object {
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_KIND = "kind"
        const val EXTRA_MODE = "mode"
        const val EXTRA_DATE = "date"
    }
}
