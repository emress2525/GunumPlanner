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

        Intent open = new Intent(context, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(context, 100000 + (int) itemId, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent done = new Intent(context, NotificationActionReceiver.class).setAction("COMPLETE");
        done.putExtra("itemId", itemId);
        PendingIntent donePi = PendingIntent.getBroadcast(context, 200000 + (int) itemId, done,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent snooze = new Intent(context, NotificationActionReceiver.class).setAction("SNOOZE");
        snooze.putExtra("itemId", itemId);
        PendingIntent snoozePi = PendingIntent.getBroadcast(context, 300000 + (int) itemId, snooze,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Günüm")
                .setContentText(item.title)
                .setAutoCancel(true)
                .setContentIntent(openPi)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_HIGH)
                .addAction(new Notification.Action.Builder(android.R.drawable.checkbox_on_background, "Tamamla", donePi).build())
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_media_ff, "10 dk ertele", snoozePi).build());
        nm.notify((int) (itemId & 0x7fffffff), b.build());
    }
}
