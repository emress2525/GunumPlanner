package com.emre.gunumplanner;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        long itemId = intent.getLongExtra("itemId", 0);
        if (itemId == 0) return;
        Db db = new Db(context);
        String action = intent.getAction();
        if ("COMPLETE".equals(action)) {
            long nextId = db.completeItem(itemId);
            ReminderScheduler.cancel(context, itemId);
            LocationReminderManager.INSTANCE.cancel(context, itemId);
            if (nextId > 0) {
                Db.Item next = db.getItem(nextId);
                if (next != null && next.dueAt > System.currentTimeMillis()) {
                    ReminderScheduler.schedule(context, nextId, next.dueAt);
                }
            }
        } else if ("SNOOZE".equals(action)) {
            ReminderScheduler.schedule(context, itemId, System.currentTimeMillis() + 10 * 60_000L);
        }
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel((int) (itemId & 0x7fffffff));
    }
}
