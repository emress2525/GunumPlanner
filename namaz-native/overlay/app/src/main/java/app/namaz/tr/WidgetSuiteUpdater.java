package app.namaz.tr;

import android.content.Context;

public final class WidgetSuiteUpdater {
    private WidgetSuiteUpdater() {}

    public static void updateAll(Context context) {
        NextPrayerWidgetProvider.updateAll(context);
        CountdownWidgetProvider.updateAll(context);
        PrayerTimesWidgetProvider.updateAll(context);
        PrayerTrackerWidgetProvider.updateAll(context);
        DailyAyahWidgetProvider.updateAll(context);
        HijriDateWidgetProvider.updateAll(context);
        QuranResumeWidgetProvider.updateAll(context);
        PrayerWidgetProvider.updateAll(context);

        boolean enabled = context.getSharedPreferences(PrayerScheduler.PREFS, Context.MODE_PRIVATE)
                .getBoolean("prayerStatusEnabled", true);
        if (enabled) PrayerStatusNotification.update(context);
        else PrayerStatusNotification.cancel(context);
    }
}
