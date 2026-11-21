package com.tevkil.radar;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.RemoteInput;
import java.util.concurrent.ConcurrentHashMap;

public class RadarListener extends NotificationListenerService {
    public static final String CHANNEL = "radar_matches";
    public static final String UPDATE = "com.tevkil.radar.UPDATE";
    static final class ReplyHandle {
        PendingIntent open;
        PendingIntent reply;
        RemoteInput[] inputs;
    }
    private static final ConcurrentHashMap<Long, ReplyHandle> handles = new ConcurrentHashMap<>();

    @Override public void onListenerConnected() {
        super.onListenerConnected();
        makeChannel();
    }
    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        try { process(sbn); } catch (Exception ignored) { /* malformed third-party notification */ }
    }
    private void process(StatusBarNotification sbn) {
        if (!Prefs.raw(this).getBoolean("monitor", false)) return;
        String source = FilterEngine.sourceName(sbn.getPackageName());
        if (source == null) return;
        Notification n = sbn.getNotification();
        if (n == null || (n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        Bundle extras = n.extras;
        if (extras == null) return;
        String title = chars(extras.getCharSequence(Notification.EXTRA_TITLE));
        String body = chars(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        if (body.isEmpty()) body = chars(extras.getCharSequence(Notification.EXTRA_TEXT));
        if (body.isEmpty()) {
            CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if (lines != null) {
                StringBuilder sb = new StringBuilder();
                for (CharSequence line : lines) if (line != null) sb.append(line).append('\n');
                body = sb.toString().trim();
            }
        }
        if (body.isEmpty()) return;
        // Only treat Instagram notifications as watched-profile messages when their
        // visible content actually identifies one of the configured usernames.
        if ("Instagram".equals(source) && !InstagramSources.matchesNotification(title, body,
                Prefs.raw(this).getString("instagram_sources", InstagramSources.DEFAULT))) return;
        if (!FilterEngine.matches(title, body, Prefs.rules(this))) return;
        long id;
        try (NoticeStore store = new NoticeStore(this)) { id = store.add(source, title, body); }
        if (id < 0) return;
        ReplyHandle handle = new ReplyHandle();
        handle.open = n.contentIntent;
        if (n.actions != null) for (Notification.Action a : n.actions) {
            if (a == null || a.actionIntent == null || a.getRemoteInputs() == null || a.getRemoteInputs().length == 0) continue;
            handle.reply = a.actionIntent;
            handle.inputs = a.getRemoteInputs();
            break;
        }
        handles.put(id, handle);
        if (handles.size() > 250) handles.clear();
        showAlert(id, source, title, body);
        Intent updated = new Intent(UPDATE); updated.setPackage(getPackageName()); sendBroadcast(updated);
    }
    private String chars(CharSequence x) { return x == null ? "" : x.toString().trim(); }
    private void makeChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "Yeni tevkil ilanları", NotificationManager.IMPORTANCE_HIGH);
            c.setDescription("Kriterlerine uyan tevkil ilanları");
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }
    private void showAlert(long id, String source, String title, String body) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        makeChannel();
        Intent open = new Intent(this, MainActivity.class).putExtra("notice_id", id).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent click = PendingIntent.getActivity(this, (int)(id & 0x7fffffff), open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(com.tevkil.radar.R.drawable.ic_radar)
                .setContentTitle("Yeni " + source + " tevkil ilanı")
                .setContentText(title.isEmpty() ? body : title + " · " + body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(click).setAutoCancel(true);
        getSystemService(NotificationManager.class).notify((int)(id & 0x7fffffff), b.build());
    }
    public static boolean canDirectReply(long id) {
        ReplyHandle h = handles.get(id);
        return h != null && h.reply != null && h.inputs != null;
    }
    /** Only invoked after a user explicitly confirms the text in the UI. */
    public static boolean sendApprovedReply(android.content.Context context, long id, String text) {
        ReplyHandle h = handles.get(id);
        if (h == null || h.reply == null || h.inputs == null || text == null || text.trim().isEmpty()) return false;
        try {
            Intent intent = new Intent();
            Bundle results = new Bundle();
            for (RemoteInput input : h.inputs) results.putCharSequence(input.getResultKey(), text);
            RemoteInput.addResultsToIntent(h.inputs, intent, results);
            h.reply.send(context, 0, intent);
            handles.remove(id);
            return true;
        } catch (Exception ex) { return false; }
    }
    public static boolean openOriginal(long id) {
        ReplyHandle h = handles.get(id);
        if (h == null || h.open == null) return false;
        try { h.open.send(); return true; } catch (Exception ex) { return false; }
    }
}
