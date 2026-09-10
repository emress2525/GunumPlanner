package com.emre.gunumplanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        Db db = new Db(context);
        FullRepository.INSTANCE.ensureSchema(db);
        for (Db.Item item : db.getOpenReminderItems()) {
            ReminderScheduler.schedule(context, item.id, item.dueAt);
        }
        LocationReminderManager.INSTANCE.rescheduleAll(context);
        GunumWidgetProvider.Companion.refreshAll(context);
    }
}
