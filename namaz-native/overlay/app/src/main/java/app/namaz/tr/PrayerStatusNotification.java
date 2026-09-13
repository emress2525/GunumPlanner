package app.namaz.tr;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

public final class PrayerStatusNotification {
    public static final String CHANNEL_ID = "prayer_status";
    private static final int NOTIFICATION_ID = 7707;

    private PrayerStatusNotification() {}

    public static void update(Context context) {
        if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Namaz durumu / kilit ekranı",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Sıradaki namazı kilit ekranında sessiz olarak gösterir.");
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);

        WidgetDataRepository.Snapshot s = WidgetDataRepository.load(context);
        PendingIntent open = WidgetRenderUtils.openAppIntent(context, "home", 7707);
        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(s.nextPrayerName + " · " + s.nextPrayerTime)
                .setContentText(WidgetDataRepository.remainingText(s.nextPrayerEpoch))
                .setSubText(s.city)
                .setContentIntent(open)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build();
        manager.notify(NOTIFICATION_ID, notification);
    }

    public static void cancel(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.cancel(NOTIFICATION_ID);
    }
}
