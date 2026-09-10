package com.emre.gunumplanner;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public class NotificationActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        long itemId = intent.getLongExtra("itemId", 0);
        if (itemId == 0) return;
        Db db = new Db(context);
        String action = intent.getAction();
        Db.Item item = db.getItem(itemId);
        if (item == null) return;

        if ("COMPLETE".equals(action)) {
            long nextId = db.completeItem(itemId);
            ReminderScheduler.cancel(context, itemId);
            LocationReminderManager.INSTANCE.cancel(context, itemId);
            if (nextId > 0) {
                FullRepository.INSTANCE.propagateSeries(db, itemId, nextId);
                Db.Item next = db.getItem(nextId);
                if (next != null && next.dueAt > System.currentTimeMillis()) {
                    ReminderScheduler.schedule(context, nextId, next.dueAt);
                }
            }
        } else if ("SNOOZE".equals(action) || "SNOOZE_10".equals(action)) {
            ReminderScheduler.schedule(context, itemId, System.currentTimeMillis() + 10 * 60_000L);
        } else if ("SNOOZE_60".equals(action)) {
            ReminderScheduler.schedule(context, itemId, System.currentTimeMillis() + 60 * 60_000L);
        } else if ("TOMORROW".equals(action)) {
            LocalDate oldDay;
            try { oldDay = LocalDate.parse(item.dayKey); } catch (Exception e) { oldDay = LocalDate.now(); }
            LocalDate newDay = oldDay.plusDays(1);
            long newDue = 0;
            if (item.dueAt > 0) {
                LocalTime tm = Instant.ofEpochMilli(item.dueAt).atZone(ZoneId.systemDefault()).toLocalTime();
                newDue = LocalDateTime.of(newDay, tm).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            ReminderScheduler.cancel(context, itemId);
            LocationReminderManager.INSTANCE.cancel(context, itemId);
            db.postponeItem(itemId, newDay.toString(), newDue);
            if (newDue > System.currentTimeMillis()) ReminderScheduler.schedule(context, itemId, newDue);
            ExtrasRepository.LocationRule rule = ExtrasRepository.INSTANCE.getLocation(db, itemId);
            if (rule != null && rule.getEnabled()) LocationReminderManager.INSTANCE.schedule(context, rule);
        }

        GunumWidgetProvider.Companion.refreshAll(context);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel((int) (itemId & 0x7fffffff));
    }
}
