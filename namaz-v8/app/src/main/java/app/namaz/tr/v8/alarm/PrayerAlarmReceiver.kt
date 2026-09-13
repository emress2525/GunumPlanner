package app.namaz.tr.v8.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import app.namaz.tr.v8.adhan.AdhanNotificationFactory
import app.namaz.tr.v8.adhan.AdhanService
import app.namaz.tr.v8.model.PrayerName

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = runCatching { AlarmKind.valueOf(intent.getStringExtra(PrayerAlarmScheduler.EXTRA_KIND).orEmpty()) }
            .getOrDefault(AlarmKind.ADHAN)
        if (kind == AlarmKind.DIAGNOSTIC) {
            AdhanNotificationFactory(context).postDiagnostic()
            return
        }
        val prayer = runCatching { PrayerName.valueOf(intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER).orEmpty()) }.getOrNull()
            ?: return
        val mode = runCatching { AdhanMode.valueOf(intent.getStringExtra(PrayerAlarmScheduler.EXTRA_MODE).orEmpty()) }
            .getOrDefault(AdhanMode.NOTIFICATION)

        if (kind == AlarmKind.PRE_REMINDER || mode == AdhanMode.NOTIFICATION) {
            AdhanNotificationFactory(context).postPrayer(prayer, preReminder = kind == AlarmKind.PRE_REMINDER)
            return
        }
        if (mode == AdhanMode.SHORT || mode == AdhanMode.FULL) {
            val service = Intent(context, AdhanService::class.java).apply {
                action = AdhanService.ACTION_PLAY
                putExtra(AdhanService.EXTRA_PRAYER, prayer.name)
                putExtra(AdhanService.EXTRA_SHORT, mode == AdhanMode.SHORT)
            }
            ContextCompat.startForegroundService(context, service)
        }
    }
}
