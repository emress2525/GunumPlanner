package com.tevkil.radar;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.List;
import java.time.LocalDate;

/** Fetches only API-authorized professional accounts and filters captions locally. */
public final class InstagramRemoteScanner {
    public static final class Result {
        public final int accounts, posts, newNotices;
        public final String status;
        Result(int accounts, int posts, int newNotices, String status) {
            this.accounts=accounts; this.posts=posts; this.newNotices=newNotices; this.status=status;
        }
    }
    private static final String CHANNEL = "radar_remote_matches";
    private InstagramRemoteScanner() {}

    /** Must be called off the Android main thread. No background sync without permission/credentials. */
    public static Result scan(Context context) {
        String professionalId = Prefs.raw(context).getString("meta_ig_id", "");
        String token = InstagramSecureConfig.getToken(context);
        if (token.isEmpty() || !professionalId.matches("[0-9]{5,30}"))
            return finish(context, new Result(0, 0, 0, "Meta bağlantısı kurulmadı"));
        List<String> usernames = InstagramSources.accounts(Prefs.raw(context).getString("instagram_sources", InstagramSources.DEFAULT));
        int checked = 0, posts = 0, count = 0, errors = 0, imageFailures = 0, imagesRead = 0;
        String lastError = "";
        try (NoticeStore store = new NoticeStore(context)) {
            for (String username : usernames) {
                try {
                    List<InstagramDiscoveryClient.Media> media = InstagramDiscoveryClient.fetch(professionalId, token, username);
                    checked++;
                    for (InstagramDiscoveryClient.Media m : media) {
                        posts++;
                        String title = "Instagram · @" + username;
                        StringBuilder combined = new StringBuilder(m.caption);
                        // Without media URL permissions, process caption only, never invent OCR findings.
                        for (String imageUrl : m.imageUrls) {
                            try {
                                String ocr = ImageTextReader.readInstagramImage(imageUrl);
                                imagesRead++;
                                if (!ocr.trim().isEmpty()) combined.append("\n[Görselden okunan metin - doğrulayın]\n").append(ocr);
                            } catch (Exception unavailable) {
                                imageFailures++; // Bad CDN URL, expired token, unavailable media or OCR failure.
                            }
                        }
                        String text = combined.toString();
                        if (!FilterEngine.matches(title, text, Prefs.rules(context))) continue;
                        if (TevkilDateGate.isExpired(text, LocalDate.now())) continue;
                        long id = store.addRemote("Instagram API · Görsel / Metin", title, text, m.permalink, m.id);
                        if (id >= 0) { count++; notifyMatch(context, id, username, text); }
                    }
                } catch (Exception e) {
                    errors++;
                    lastError = e.getMessage() == null ? "Bilinmeyen hata" : e.getMessage();
                }
            }
        } catch (Exception e) { errors++; lastError="Yerel veritabanı hatası"; }
        String status = errors == 0 ? "Tarama tamamlandı: " + checked + " hesap, " + count + " yeni ilan, " + imagesRead + " görsel okundu" + (imageFailures > 0 ? ", " + imageFailures + " görsele erişilemedi" : "")
                : "Tarama eksik: " + errors + " hesap başarısız. " + lastError + " · " + imageFailures + " görsel hatası";
        return finish(context, new Result(checked, posts, count, status));
    }

    private static Result finish(Context context, Result r) {
        Prefs.raw(context).edit().putString("meta_last_status", r.status)
                .putLong("meta_last_time", System.currentTimeMillis()).apply();
        Intent updated = new Intent(RadarListener.UPDATE); updated.setPackage(context.getPackageName());
        context.sendBroadcast(updated);
        return r;
    }

    private static void notifyMatch(Context context, long id, String username, String caption) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL,
                "Instagram API tevkil ilanları", NotificationManager.IMPORTANCE_HIGH));
        Intent intent = new Intent(context, MainActivity.class).putExtra("notice_id", id)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, (int) (id & 0x7fffffff), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        // The notification action is an explicit user gesture. It prepares a clipboard
        // comment and opens the original post, never claims to have submitted a comment.
        Intent quick = new Intent(context, MainActivity.class)
                .putExtra("notice_id", id).putExtra("quick_comment", true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent quickPending = PendingIntent.getActivity(context, ((int)(id & 0x7fffffff)) ^ 0x55000000,
                quick, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.notify((int)(id & 0x7fffffff), new Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_radar).setContentTitle("Yeni tevkil ilanı · @" + username)
                .setContentText(caption).setStyle(new Notification.BigTextStyle().bigText(caption))
                .setAutoCancel(true).setContentIntent(pending)
                .addAction(R.drawable.ic_radar, "Yoruma hazırla", quickPending)
                .build());
    }
}
