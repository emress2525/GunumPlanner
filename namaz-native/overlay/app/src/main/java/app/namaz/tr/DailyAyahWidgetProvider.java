package app.namaz.tr;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

public class DailyAyahWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateOne(context, manager, id);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, DailyAyahWidgetProvider.class));
        for (int id : ids) updateOne(context, manager, id);
    }

    private static void updateOne(Context context, AppWidgetManager manager, int id) {
        WidgetDataRepository.Snapshot s = WidgetDataRepository.load(context);
        RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_daily_ayah);
        String text = s.dailyAyahText.isEmpty() ? "Günün ayeti uygulama açılınca güncellenecek." : s.dailyAyahText;
        v.setTextViewText(R.id.widgetAyahText, text);
        v.setTextViewText(R.id.widgetAyahSource, s.dailyAyahSource.isEmpty() ? "" : s.dailyAyahSource);
        v.setOnClickPendingIntent(R.id.widgetAyahRoot, WidgetRenderUtils.openAppIntent(context, "quran", 7105));
        manager.updateAppWidget(id, v);
    }
}
