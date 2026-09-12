package app.namaz.tr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String json = context.getSharedPreferences(PrayerScheduler.PREFS, 0)
                .getString(PrayerScheduler.KEY_SCHEDULE_JSON, null);
        if (json != null && !json.isEmpty()) {
            try { PrayerScheduler.scheduleFromJson(context, json); } catch (Exception ignored) {}
        }
        PrayerWidgetProvider.updateAll(context);
    }
}
