package app.namaz.tr;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

public class CountdownWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateOne(context, manager, id);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, CountdownWidgetProvider.class));
        for (int id : ids) updateOne(context, manager, id);
    }

    private static void updateOne(Context context, AppWidgetManager manager, int id) {
        WidgetDataRepository.Snapshot s = WidgetDataRepository.load(context);
        RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_countdown);
        v.setTextViewText(R.id.widgetCountdownName, s.nextPrayerName);
        v.setTextViewText(R.id.widgetCountdownTime, s.nextPrayerTime);
        v.setTextViewText(R.id.widgetCountdownRemaining, WidgetDataRepository.remainingText(s.nextPrayerEpoch));
        v.setOnClickPendingIntent(R.id.widgetCountdownRoot, WidgetRenderUtils.openAppIntent(context, "home", 7102));
        manager.updateAppWidget(id, v);
    }
}
