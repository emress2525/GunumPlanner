package app.namaz.tr.v8.health

import android.Manifest
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat

enum class HealthStatus { GREEN, YELLOW, RED }

enum class HealthAction {
    OPEN_NOTIFICATION_SETTINGS,
    OPEN_EXACT_ALARM_SETTINGS,
    OPEN_BATTERY_SETTINGS,
    OPEN_DND_SETTINGS,
    OPEN_APP_SETTINGS,
    SEND_TEST_NOTIFICATION,
    SCHEDULE_TEST_ALARM,
}

data class HealthItem(
    val id: String,
    val title: String,
    val status: HealthStatus,
    val explanation: String,
    val action: HealthAction?,
)

class HealthStatusReducer {
    fun reduce(
        api: Int,
        notificationGranted: Boolean,
        exactAlarm: Boolean,
        batteryRestricted: Boolean,
        backgroundRestricted: Boolean = false,
        dndAccess: Boolean = false,
        manufacturer: String = "Android",
    ): List<HealthItem> = listOf(
        HealthItem(
            "notification",
            "Bildirim izni",
            if (notificationGranted || api < 33) HealthStatus.GREEN else HealthStatus.RED,
            if (notificationGranted || api < 33) "Bildirimler kullanılabilir." else "Android bildirim izni kapalı; ezan bildirimi gösterilemez.",
            if (notificationGranted || api < 33) null else HealthAction.OPEN_NOTIFICATION_SETTINGS,
        ),
        HealthItem(
            "exact_alarm",
            "Tam zamanlı alarm",
            if (api < 31 || exactAlarm) HealthStatus.GREEN else HealthStatus.RED,
            if (api < 31 || exactAlarm) "Exact alarm kullanılabilir." else "Tam zamanlı alarm izni yok; sistem alarmı geciktirebilir.",
            if (api < 31 || exactAlarm) null else HealthAction.OPEN_EXACT_ALARM_SETTINGS,
        ),
        HealthItem(
            "battery",
            "Pil optimizasyonu",
            if (batteryRestricted) HealthStatus.YELLOW else HealthStatus.GREEN,
            if (batteryRestricted) "Pil optimizasyonu arka plan ezanını geciktirebilir." else "Uygulama pil açısından kısıtlı görünmüyor.",
            if (batteryRestricted) HealthAction.OPEN_BATTERY_SETTINGS else null,
        ),
        HealthItem(
            "background",
            "Arka plan kısıtlaması",
            if (backgroundRestricted) HealthStatus.RED else HealthStatus.GREEN,
            if (backgroundRestricted) "Sistem uygulamayı arka planda kısıtlıyor." else "Arka plan kısıtlaması görünmüyor.",
            if (backgroundRestricted) HealthAction.OPEN_APP_SETTINGS else null,
        ),
        HealthItem(
            "dnd",
            "Rahatsız Etmeyin erişimi",
            if (dndAccess) HealthStatus.GREEN else HealthStatus.YELLOW,
            if (dndAccess) "DND erişimi açık." else "DND erişimi kapalı. Bu, normal bildirimleri tamamen engellemez; sessiz mod davranışını etkileyebilir.",
            if (dndAccess) null else HealthAction.OPEN_DND_SETTINGS,
        ),
        HealthItem(
            "oem",
            "$manufacturer otomatik başlatma",
            HealthStatus.YELLOW,
            "Üreticiye özel otomatik başlatma ayarı Android tarafından güvenilir biçimde okunamaz; gerektiğinde cihaz ayarından kontrol et.",
            HealthAction.OPEN_APP_SETTINGS,
        ),
    )
}

class NotificationHealthInspector(
    private val context: Context,
    private val reducer: HealthStatusReducer = HealthStatusReducer(),
) {
    fun inspect(): List<HealthItem> {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val powerManager = context.getSystemService(PowerManager::class.java)
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val notificationGranted = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else notificationManager.areNotificationsEnabled()
        val exact = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()
        val batteryRestricted = !powerManager.isIgnoringBatteryOptimizations(context.packageName)
        val backgroundRestricted = Build.VERSION.SDK_INT >= 28 && activityManager.isBackgroundRestricted
        val dnd = notificationManager.isNotificationPolicyAccessGranted
        return reducer.reduce(
            api = Build.VERSION.SDK_INT,
            notificationGranted = notificationGranted,
            exactAlarm = exact,
            batteryRestricted = batteryRestricted,
            backgroundRestricted = backgroundRestricted,
            dndAccess = dnd,
            manufacturer = Build.MANUFACTURER.ifBlank { "Android" },
        )
    }
}
