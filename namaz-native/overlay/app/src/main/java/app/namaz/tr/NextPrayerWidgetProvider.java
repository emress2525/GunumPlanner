package app.namaz.tr;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

public class NextPrayerWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateOne(context, manager, id);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, NextPrayerWidgetProvider.class));
        for (int id : ids) updateOne(context, manager, id);
    }

    private static void updateOne(Context context, AppWidgetManager manager, int id) {
        WidgetDataRepository.Snapshot s = WidgetDataRepository.load(context);
        RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_next_prayer);
        v.setTextViewText(R.id.widgetNextCity, s.city);
        v.setTextViewText(R.id.widgetNextName, s.nextPrayerName);
        v.setTextViewText(R.id.widgetNextTime, s.nextPrayerTime);
        v.setTextViewText(R.id.widgetNextRemaining, WidgetDataRepository.remainingText(s.nextPrayerEpoch));
        v.setOnClickPendingIntent(R.id.widgetNextRoot, WidgetRenderUtils.openAppIntent(context, "home", 7101));
        manager.updateAppWidget(id, v);
    }
}
