package app.namaz.tr;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

public class HijriDateWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateOne(context, manager, id);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, HijriDateWidgetProvider.class));
        for (int id : ids) updateOne(context, manager, id);
    }

    private static void updateOne(Context context, AppWidgetManager manager, int id) {
        WidgetDataRepository.Snapshot s = WidgetDataRepository.load(context);
        RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_hijri);
        v.setTextViewText(R.id.widgetHijriDay, WidgetRenderUtils.hijriDay(s.hijri));
        v.setTextViewText(R.id.widgetHijriRest, WidgetRenderUtils.hijriRest(s.hijri));
        v.setTextViewText(R.id.widgetGregorian, s.gregorian);
        v.setTextViewText(R.id.widgetHijriCity, s.city);
        v.setOnClickPendingIntent(R.id.widgetHijriRoot, WidgetRenderUtils.openAppIntent(context, "home", 7106));
        manager.updateAppWidget(id, v);
    }
}
