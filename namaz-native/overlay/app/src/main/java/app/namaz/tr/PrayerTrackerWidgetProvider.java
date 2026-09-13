package app.namaz.tr;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

public class PrayerTrackerWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateOne(context, manager, id);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, PrayerTrackerWidgetProvider.class));
        for (int id : ids) updateOne(context, manager, id);
    }

    private static void updateOne(Context context, AppWidgetManager manager, int id) {
        WidgetDataRepository.Snapshot s = WidgetDataRepository.load(context);
        RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_tracker);
        v.setTextViewText(R.id.widgetTrackerProgress, s.tracked + "/5");
        v.setTextViewText(R.id.widgetTrackerDots, WidgetRenderUtils.trackerDots(s.tracked));
        v.setOnClickPendingIntent(R.id.widgetTrackerRoot, WidgetRenderUtils.openAppIntent(context, "tracker", 7104));
        manager.updateAppWidget(id, v);
    }
}
