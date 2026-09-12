package app.namaz.tr;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class PrayerAlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL = "prayer_reminders";

    @Override public void onReceive(Context context, Intent intent) {
        String name = intent.getStringExtra("name");
        String mode = intent.getStringExtra("mode");
        if (name == null) name = "Namaz";
        if (mode == null) mode = "full";
        if ("off".equals(mode)) return;
        if ("notification".equals(mode)) {
            showNotification(context, name);
            return;
        }
        Intent service = new Intent(context, AdhanService.class)
                .setAction(AdhanService.ACTION_PLAY)
                .putExtra("name", name)
                .putExtra("mode", mode);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service);
        else context.startService(service);
    }

    private static void showNotification(Context context, String name) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Namaz vakti bildirimleri", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Sesli ezan kapalıyken namaz vakti bildirimi");
            ch.setSound(null, null);
            nm.createNotificationChannel(ch);
        }
        Intent open = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 811, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new android.app.Notification.Builder(context, CHANNEL)
                : new android.app.Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(name + " vakti")
                .setContentText("Namaz vakti girdi.")
                .setAutoCancel(true)
                .setContentIntent(pi);
        nm.notify(812, b.build());
    }
}
