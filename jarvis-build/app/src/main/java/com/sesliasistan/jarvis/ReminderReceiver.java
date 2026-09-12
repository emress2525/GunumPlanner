package com.sesliasistan.jarvis;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class ReminderReceiver extends BroadcastReceiver {
    public static final String EXTRA_TEXT = "reminder_text";
    private static final String CHANNEL_ID = "jarvis_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        String text = intent == null ? null : intent.getStringExtra(EXTRA_TEXT);
        if (text == null || text.trim().isEmpty()) text = "Hatırlatıcın var.";

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Jarvis Hatırlatıcıları",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Jarvis tarafından oluşturulan zamanlı hatırlatıcılar.");
        manager.createNotificationChannel(channel);

        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_jarvis)
                .setContentTitle("Jarvis Hatırlatıcı")
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .build();

        int id = (int) (System.currentTimeMillis() & 0x7fffffff);
        manager.notify(id, notification);
    }
}
