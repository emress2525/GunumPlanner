package app.namaz.tr;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class PrayerWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateOne(context, manager, id);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, PrayerWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        for (int id : ids) updateOne(context, manager, id);
    }

    private static void updateOne(Context context, AppWidgetManager manager, int id) {
        android.content.SharedPreferences p = context.getSharedPreferences(PrayerScheduler.PREFS, 0);
        String name = p.getString("nextPrayerName", "Namaz");
        String time = p.getString("nextPrayerTime", "—");
        long epoch = p.getLong("nextPrayerEpoch", 0L);
        RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.prayer_widget);
        v.setTextViewText(R.id.widgetPrayer, name + " · " + time);
        v.setTextViewText(R.id.widgetRemaining, remaining(epoch));
        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 991, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.widgetRoot, pi);
        manager.updateAppWidget(id, v);
    }

    private static String remaining(long epoch) {
        long ms = epoch - System.currentTimeMillis();
        if (epoch <= 0 || ms <= 0) return "Vakitleri güncellemek için uygulamayı aç";
        long totalMinutes = ms / 60000L;
        long h = totalMinutes / 60L;
        long m = totalMinutes % 60L;
        return h > 0 ? (h + " sa " + m + " dk kaldı") : (m + " dk kaldı");
    }
}
