package app.namaz.tr;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

public class PrayerTimesWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateOne(context, manager, id);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, PrayerTimesWidgetProvider.class));
        for (int id : ids) updateOne(context, manager, id);
    }

    private static void updateOne(Context context, AppWidgetManager manager, int id) {
        WidgetDataRepository.Snapshot s = WidgetDataRepository.load(context);
        RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_prayer_times);
        v.setTextViewText(R.id.widgetTimesCity, s.city);
        v.setTextViewText(R.id.widgetTimesHijri, s.hijri);
        v.setTextViewText(R.id.widgetTimesImsak, s.imsak);
        v.setTextViewText(R.id.widgetTimesGunes, s.gunes);
        v.setTextViewText(R.id.widgetTimesOgle, s.ogle);
        v.setTextViewText(R.id.widgetTimesIkindi, s.ikindi);
        v.setTextViewText(R.id.widgetTimesAksam, s.aksam);
        v.setTextViewText(R.id.widgetTimesYatsi, s.yatsi);
        v.setOnClickPendingIntent(R.id.widgetTimesRoot, WidgetRenderUtils.openAppIntent(context, "home", 7103));
        manager.updateAppWidget(id, v);
    }
}
