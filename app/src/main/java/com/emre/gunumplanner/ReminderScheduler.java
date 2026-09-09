package com.emre.gunumplanner;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class ReminderScheduler {
    private ReminderScheduler() {}

    public static void schedule(Context context, long itemId, long whenMillis) {
        if (whenMillis <= System.currentTimeMillis()) return;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = pending(context, itemId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pi);
        }
    }

    public static void cancel(Context context, long itemId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pending(context, itemId));
    }

    private static PendingIntent pending(Context context, long itemId) {
        Intent i = new Intent(context, ReminderReceiver.class);
        i.putExtra("itemId", itemId);
        return PendingIntent.getBroadcast(
                context,
                (int) (itemId & 0x7fffffff),
                i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
