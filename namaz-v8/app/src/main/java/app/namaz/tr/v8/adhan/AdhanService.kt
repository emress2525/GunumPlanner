package app.namaz.tr.v8.adhan

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import app.namaz.tr.v8.alarm.AdhanMode
import app.namaz.tr.v8.alarm.AlarmKind
import app.namaz.tr.v8.alarm.PrayerAlarmReceiver
import app.namaz.tr.v8.alarm.PrayerAlarmScheduler
import app.namaz.tr.v8.model.PrayerName
import java.time.LocalDate

class AdhanService : Service() {
    private var player: ExoPlayer? = null
    private var currentPrayer: PrayerName = PrayerName.DHUHR

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopPlayback()
            ACTION_SNOOZE -> {
                scheduleSnooze()
                stopPlayback()
            }
            ACTION_PLAY, null -> {
                currentPrayer = runCatching { PrayerName.valueOf(intent?.getStringExtra(EXTRA_PRAYER).orEmpty()) }
                    .getOrDefault(PrayerName.DHUHR)
                startForeground(AdhanNotificationFactory.FOREGROUND_ID, AdhanNotificationFactory(this).foreground(currentPrayer))
                play(short = intent?.getBooleanExtra(EXTRA_SHORT, false) == true)
            }
        }
        return START_NOT_STICKY
    }

    private fun play(short: Boolean) {
        player?.release()
        val rawId = resources.getIdentifier("adhan", "raw", packageName)
        if (rawId == 0) {
            stopSelf()
            return
        }
        val uri = Uri.parse("android.resource://$packageName/$rawId")
        player = ExoPlayer.Builder(this).build().also { exo ->
            exo.setMediaItem(MediaItem.fromUri(uri))
            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) stopPlayback()
                }
            })
            exo.prepare()
            exo.play()
        }
        if (short) Handler(Looper.getMainLooper()).postDelayed({ stopPlayback() }, 20_000L)
    }

    private fun scheduleSnooze() {
        val intent = Intent(this, PrayerAlarmReceiver::class.java).apply {
            putExtra(PrayerAlarmScheduler.EXTRA_PRAYER, currentPrayer.name)
            putExtra(PrayerAlarmScheduler.EXTRA_KIND, AlarmKind.ADHAN.name)
            putExtra(PrayerAlarmScheduler.EXTRA_MODE, AdhanMode.NOTIFICATION.name)
            putExtra(PrayerAlarmScheduler.EXTRA_DATE, LocalDate.now().toString())
        }
        val pending = PendingIntent.getBroadcast(
            this,
            777,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmManager = getSystemService(AlarmManager::class.java)
        val at = System.currentTimeMillis() + 5 * 60_000L
        if (android.os.Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    private fun stopPlayback() {
        player?.stop()
        player?.release()
        player = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "app.namaz.tr.v8.action.PLAY_ADHAN"
        const val ACTION_STOP = "app.namaz.tr.v8.action.STOP_ADHAN"
        const val ACTION_SNOOZE = "app.namaz.tr.v8.action.SNOOZE_ADHAN"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_SHORT = "short"
    }
}
