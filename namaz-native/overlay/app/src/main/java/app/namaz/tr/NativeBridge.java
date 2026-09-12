package app.namaz.tr;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONObject;

import java.util.Locale;

public final class NativeBridge {
    private final MainActivity activity;
    private final WebView webView;
    private final QiblaController qiblaController;
    private MediaPlayer quranPlayer;
    private TextToSpeech tts;
    private String pendingSpeech;

    public NativeBridge(MainActivity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
        this.qiblaController = new QiblaController(activity, webView);
    }

    @JavascriptInterface public void playQuran(String url, String title) {
        activity.runOnUiThread(() -> {
            releaseQuran();
            try {
                MediaPlayer p = new MediaPlayer();
                p.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build());
                p.setDataSource(url);
                p.setOnPreparedListener(mp -> {
                    mp.start();
                    emitAudioState("playing");
                });
                p.setOnCompletionListener(mp -> {
                    releaseQuran();
                    activity.evaluateJs("window.onNativeAudioEnded&&window.onNativeAudioEnded()");
                });
                p.setOnErrorListener((mp, what, extra) -> {
                    releaseQuran();
                    emitToast("Ses dosyası açılamadı. İnternet bağlantısını kontrol et.");
                    return true;
                });
                quranPlayer = p;
                p.prepareAsync();
            } catch (Exception e) {
                releaseQuran();
                emitToast("Ses başlatılamadı.");
            }
        });
    }

    @JavascriptInterface public void pauseResumeQuran() {
        activity.runOnUiThread(() -> {
            if (quranPlayer == null) return;
            try {
                if (quranPlayer.isPlaying()) {
                    quranPlayer.pause();
                    emitAudioState("paused");
                } else {
                    quranPlayer.start();
                    emitAudioState("playing");
                }
            } catch (IllegalStateException ignored) {}
        });
    }

    @JavascriptInterface public void stopQuran() {
        activity.runOnUiThread(() -> {
            releaseQuran();
            emitAudioState("stopped");
        });
    }

    @JavascriptInterface public void speakTurkish(String text) {
        if (text == null || text.trim().isEmpty()) return;
        activity.runOnUiThread(() -> {
            pendingSpeech = text;
            if (tts == null) {
                tts = new TextToSpeech(activity, status -> {
                    if (status != TextToSpeech.SUCCESS || tts == null) {
                        emitToast("Türkçe sesli okuma başlatılamadı.");
                        return;
                    }
                    int result = tts.setLanguage(new Locale("tr", "TR"));
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        emitToast("Telefonda Türkçe metin okuma sesi bulunamadı.");
                        return;
                    }
                    speakPending();
                });
            } else {
                speakPending();
            }
        });
    }

    private void speakPending() {
        if (tts == null || pendingSpeech == null) return;
        tts.setSpeechRate(0.9f);
        tts.speak(pendingSpeech, TextToSpeech.QUEUE_FLUSH, null, "namaz_meal");
    }

    @JavascriptInterface public void startQibla(double qiblaBearing) {
        activity.runOnUiThread(qiblaController::start);
    }

    @JavascriptInterface public void stopQibla() {
        activity.runOnUiThread(qiblaController::stop);
    }

    @JavascriptInterface public void schedulePrayers(String json) {
        try {
            activity.getSharedPreferences(PrayerScheduler.PREFS, 0)
                    .edit().putString(PrayerScheduler.KEY_SCHEDULE_JSON, json).apply();
            PrayerScheduler.scheduleFromJson(activity, json);
        } catch (Exception e) {
            emitToast("Arka plan namaz hatırlatmaları ayarlanamadı.");
        }
    }

    @JavascriptInterface public void updateWidget(String json) {
        try {
            JSONObject o = new JSONObject(json);
            activity.getSharedPreferences(PrayerScheduler.PREFS, 0).edit()
                    .putString("nextPrayerName", o.optString("name", "Namaz"))
                    .putString("nextPrayerTime", o.optString("time", "—"))
                    .putLong("nextPrayerEpoch", o.optLong("epoch", 0L))
                    .apply();
            PrayerWidgetProvider.updateAll(activity);
        } catch (Exception ignored) {}
    }

    @JavascriptInterface public void updateAyah(String json) {
        try {
            JSONObject o = new JSONObject(json);
            String text = o.optString("tr", "").trim();
            String source = o.optString("src", "").trim();
            if (text.length() > 180) text = text.substring(0, 177).trim() + "…";
            activity.getSharedPreferences(PrayerScheduler.PREFS, 0).edit()
                    .putString("dailyAyahText", text)
                    .putString("dailyAyahSource", source)
                    .apply();
            PrayerWidgetProvider.updateAll(activity);
        } catch (Exception ignored) {}
    }

    @JavascriptInterface public void stopAdhan() {
        android.content.Intent i = new android.content.Intent(activity, AdhanService.class)
                .setAction(AdhanService.ACTION_STOP);
        activity.startService(i);
    }

    private void emitAudioState(String state) {
        activity.evaluateJs("window.onNativeAudioState&&window.onNativeAudioState(" + JSONObject.quote(state) + ")");
    }

    private void emitToast(String message) {
        activity.evaluateJs("typeof toast==='function'&&toast(" + JSONObject.quote(message) + ")");
    }

    private void releaseQuran() {
        if (quranPlayer != null) {
            try { quranPlayer.stop(); } catch (Exception ignored) {}
            try { quranPlayer.reset(); } catch (Exception ignored) {}
            try { quranPlayer.release(); } catch (Exception ignored) {}
            quranPlayer = null;
        }
    }

    public void release() {
        qiblaController.stop();
        releaseQuran();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
}
