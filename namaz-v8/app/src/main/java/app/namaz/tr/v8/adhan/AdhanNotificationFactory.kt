package app.namaz.tr.v8.adhan

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import app.namaz.tr.v8.MainActivity
import app.namaz.tr.v8.model.PrayerName

class AdhanNotificationFactory(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    init { ensureChannels() }

    fun foreground(prayer: PrayerName): Notification = baseBuilder(CHANNEL_ADHAN)
        .setContentTitle("${prayer.displayName} vakti")
        .setContentText("Ezan çalıyor")
        .setOngoing(true)
        .addAction(action("Durdur", AdhanService.ACTION_STOP, 401))
        .addAction(action("5 dk sonra hatırlat", AdhanService.ACTION_SNOOZE, 402))
        .build()

    fun postPrayer(prayer: PrayerName, preReminder: Boolean) {
        if (!canNotify()) return
        val notification = baseBuilder(CHANNEL_REMINDER)
            .setContentTitle(if (preReminder) "${prayer.displayName} yaklaşıyor" else "${prayer.displayName} vakti")
            .setContentText(if (preReminder) "Namaz vaktine az kaldı." else "Namaz vakti girdi.")
            .build()
        manager.notify(500 + prayer.ordinal, notification)
    }

    fun postDiagnostic() {
        if (!canNotify()) return
        manager.notify(
            599,
            baseBuilder(CHANNEL_REMINDER)
                .setContentTitle("Namaz V8 test alarmı")
                .setContentText("Alarm ve bildirim yolu çalıştı.")
                .build(),
        )
    }

    private fun baseBuilder(channel: String): Notification.Builder {
        val open = PendingIntent.getActivity(
            context,
            400,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(open)
            .setAutoCancel(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
    }

    private fun action(label: String, action: String, requestCode: Int): Notification.Action {
        val pending = PendingIntent.getService(
            context,
            requestCode,
            Intent(context, AdhanService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Action.Builder(null, label, pending).build()
    }

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannels() {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ADHAN, "Ezan", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Namaz vaktinde ezan oynatma bildirimi"
                setSound(null, null)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDER, "Namaz hatırlatmaları", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Namaz vakti ve test hatırlatmaları"
            },
        )
    }

    companion object {
        const val CHANNEL_ADHAN = "adhan_playback"
        const val CHANNEL_REMINDER = "prayer_reminders"
        const val FOREGROUND_ID = 410
    }
}
