package com.emre.gunumplanner;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "gunum_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        long itemId = intent.getLongExtra("itemId", 0);
        if (itemId == 0) return;
        Db db = new Db(context);
        Db.Item item = db.getItem(itemId);
        if (item == null || !Db.STATUS_OPEN.equals(item.status)) return;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Günüm Hatırlatmaları", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Görev hatırlatmaları");
            nm.createNotificationChannel(ch);
        }

        Intent open = new Intent(context, PremiumV2Activity.class);
        PendingIntent openPi = PendingIntent.getActivity(context, 100000 + (int) itemId, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent donePi = action(context, itemId, "COMPLETE", 200000);
        PendingIntent snooze10Pi = action(context, itemId, "SNOOZE_10", 300000);
        PendingIntent snooze60Pi = action(context, itemId, "SNOOZE_60", 400000);
        PendingIntent tomorrowPi = action(context, itemId, "TOMORROW", 500000);

        String extra = item.durationMinutes > 0 ? " • " + item.durationMinutes + " dk" : "";
        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(item.title)
                .setContentText("Günüm" + extra)
                .setStyle(new Notification.BigTextStyle().bigText((item.body == null || item.body.isEmpty() ? "Görev zamanı geldi" : item.body) + extra))
                .setAutoCancel(true)
                .setContentIntent(openPi)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_HIGH)
                .addAction(new Notification.Action.Builder(android.R.drawable.checkbox_on_background, "Tamamla", donePi).build())
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_lock_idle_alarm, "10 dk", snooze10Pi).build())
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_lock_idle_alarm, "1 saat", snooze60Pi).build())
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_media_next, "Yarın", tomorrowPi).build());
        nm.notify((int) (itemId & 0x7fffffff), b.build());
    }

    private PendingIntent action(Context context, long itemId, String action, int base) {
        Intent i = new Intent(context, NotificationActionReceiver.class).setAction(action);
        i.putExtra("itemId", itemId);
        return PendingIntent.getBroadcast(context, base + (int) itemId, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
