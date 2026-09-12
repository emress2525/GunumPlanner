package app.namaz.tr;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

public final class PrayerScheduler {
    public static final String PREFS = "namaz_native";
    public static final String KEY_SCHEDULE_JSON = "schedule_json";

    private PrayerScheduler() {}

    public static int requestCodeFor(String key) {
        return ("namaz_" + key).hashCode() & 0x7fffffff;
    }

    public static void scheduleFromJson(Context context, String json) throws Exception {
        JSONObject root = new JSONObject(json);
        String mode = root.optString("mode", "full");
        JSONArray prayers = root.optJSONArray("prayers");
        if (prayers == null) return;
        for (int i = 0; i < prayers.length(); i++) {
            JSONObject p = prayers.getJSONObject(i);
            schedule(context,
                    p.optString("key", "p" + i),
                    p.optString("name", "Namaz"),
                    p.optLong("epoch", 0L),
                    mode);
        }
    }

    public static void schedule(Context context, String key, String name, long epoch, String mode) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        Intent intent = new Intent(context, PrayerAlarmReceiver.class)
                .setAction("app.namaz.tr.PRAYER." + key)
                .putExtra("key", key)
                .putExtra("name", name)
                .putExtra("mode", mode);
        PendingIntent pi = PendingIntent.getBroadcast(context, requestCodeFor(key), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if ("off".equals(mode) || epoch <= System.currentTimeMillis() + 5000L) {
            alarm.cancel(pi);
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= 31 && !alarm.canScheduleExactAlarms()) {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epoch, pi);
            } else if (Build.VERSION.SDK_INT >= 23) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epoch, pi);
            } else {
                alarm.setExact(AlarmManager.RTC_WAKEUP, epoch, pi);
            }
        } catch (SecurityException ex) {
            if (Build.VERSION.SDK_INT >= 23) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epoch, pi);
            else alarm.set(AlarmManager.RTC_WAKEUP, epoch, pi);
        }
    }
}
