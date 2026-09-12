package app.namaz.tr;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class AdhanService extends Service {
    public static final String ACTION_PLAY = "app.namaz.tr.action.PLAY_ADHAN";
    public static final String ACTION_STOP = "app.namaz.tr.action.STOP_ADHAN";
    private static final String CHANNEL = "adhan_audio";
    private static final int NOTIFICATION_ID = 9001;
    private MediaPlayer player;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopPlayback();
            return START_NOT_STICKY;
        }
        String name = intent == null ? "Namaz" : intent.getStringExtra("name");
        String mode = intent == null ? "full" : intent.getStringExtra("mode");
        if (name == null) name = "Namaz";
        if (mode == null) mode = "full";
        createChannel();
        startForeground(NOTIFICATION_ID, buildNotification(name));
        startPlayback("short".equals(mode));
        return START_NOT_STICKY;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Ezan sesi", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Namaz vakti ezan oynatımı");
        ch.setSound(null, null);
        nm.createNotificationChannel(ch);
    }

    private Notification buildNotification(String name) {
        Intent stop = new Intent(this, AdhanService.class).setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 901, stop, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(this, 902, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(name + " vakti")
                .setContentText("Ezan çalıyor · durdurmak için dokun")
                .setOngoing(true)
                .setContentIntent(openPi)
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_media_pause, "Durdur", stopPi).build())
                .build();
    }

    private void startPlayback(boolean shortMode) {
        stopPlayerOnly();
        try {
            MediaPlayer p = new MediaPlayer();
            p.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            android.content.res.AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.adhan);
            if (afd == null) throw new IllegalStateException("adhan resource missing");
            p.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            p.setOnCompletionListener(mp -> stopPlayback());
            p.prepare();
            p.start();
            player = p;
            if (shortMode) handler.postDelayed(this::stopPlayback, 18000L);
        } catch (Exception e) {
            stopPlayback();
        }
    }

    private void stopPlayerOnly() {
        handler.removeCallbacksAndMessages(null);
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
    }

    private void stopPlayback() {
        stopPlayerOnly();
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE);
        else stopForeground(true);
        stopSelf();
    }

    @Override public void onDestroy() {
        stopPlayerOnly();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
