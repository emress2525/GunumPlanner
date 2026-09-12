package com.sesliasistan.jarvis;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.graphics.drawable.Icon;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.KeyEvent;

import com.sesliasistan.jarvis.core.CommandPlanner;
import com.sesliasistan.jarvis.core.CommandResult;
import com.sesliasistan.jarvis.core.WakeSession;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class JarvisListeningService extends Service implements RecognitionListener {
    public static final String ACTION_START = "com.sesliasistan.jarvis.action.START";
    public static final String ACTION_STOP = "com.sesliasistan.jarvis.action.STOP";
    public static final String ACTION_STATUS = "com.sesliasistan.jarvis.action.STATUS";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_TRANSCRIPT = "transcript";
    public static final String EXTRA_ACTIVE = "active";
    public static final String PREFS = "jarvis_state";
    public static final String KEY_ACTIVE = "active";

    private static final String NOTES_PREFS = "jarvis_notes";
    private static final String KEY_LAST_NOTE = "last_note";
    private static final String KEY_LAST_NOTE_TIME = "last_note_time";
    private static final String ROUTINES_PREFS = "jarvis_routines_v2";
    private static final String HISTORY_PREFS = "jarvis_history_v2";
    private static final String KEY_HISTORY = "entries";
    private static final String CHANNEL_ID = "jarvis_listening";
    private static final int NOTIFICATION_ID = 1701;
    private static final long COMMAND_WINDOW_MS = 8000L;
    private static final String UTTERANCE_ID = "jarvis_reply";
    private static final int MAX_ROUTINE_DEPTH = 3;
    private static final int MAX_HISTORY = 20;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final WakeSession wakeSession = new WakeSession(COMMAND_WINDOW_MS);
    private final Map<String, String> knownPackages = new HashMap<>();

    private SpeechRecognizer recognizer;
    private Intent recognizerIntent;
    private TextToSpeech tts;
    private boolean ttsReady;
    private boolean active;
    private boolean listening;
    private boolean pausedForTts;
    private boolean destroyed;
    private int consecutiveErrors;

    @Override
    public void onCreate() {
        super.onCreate();
        initKnownPackages();
        createNotificationChannel();
        initTextToSpeech();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopAssistant();
            return START_NOT_STICKY;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            setRequested(false);
            broadcast("Mikrofon izni gerekli", null, false);
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } catch (RuntimeException error) {
            setRequested(false);
            broadcast("Mikrofon servisi başlatılamadı", null, false);
            stopSelf();
            return START_NOT_STICKY;
        }

        active = true;
        destroyed = false;
        setRequested(true);
        ensureRecognizer();
        if (!active) return START_NOT_STICKY;
        broadcast("Jarvis demeni bekliyorum", null, true);
        scheduleRecognition(250L);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        destroyed = true;
        active = false;
        listening = false;
        handler.removeCallbacksAndMessages(null);
        if (recognizer != null) {
            try {
                recognizer.cancel();
                recognizer.destroy();
            } catch (RuntimeException ignored) { }
            recognizer = null;
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        broadcast("Asistan durdu", null, false);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setRequested(boolean value) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ACTIVE, value).apply();
    }

    private void stopAssistant() {
        setRequested(false);
        active = false;
        destroyed = true;
        handler.removeCallbacksAndMessages(null);
        broadcast("Asistan kapalı", null, false);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void ensureRecognizer() {
        if (recognizer != null) return;
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            setRequested(false);
            active = false;
            broadcast("Bu telefonda konuşma tanıma servisi bulunamadı", null, false);
            stopSelf();
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
                recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
            } else {
                recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            }
            recognizer.setRecognitionListener(this);
            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "tr-TR");
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        } catch (RuntimeException error) {
            recognizer = null;
            setRequested(false);
            active = false;
            broadcast("Konuşma tanıma servisi başlatılamadı", null, false);
            stopSelf();
        }
    }

    private final Runnable startRecognitionRunnable = () -> {
        if (!active || destroyed || pausedForTts || listening) return;
        if (recognizer == null) {
            ensureRecognizer();
            if (recognizer == null) return;
        }
        try {
            listening = true;
            recognizer.startListening(recognizerIntent);
        } catch (SecurityException error) {
            listening = false;
            setRequested(false);
            broadcast("Mikrofon izni kayboldu", null, false);
            stopSelf();
        } catch (RuntimeException error) {
            listening = false;
            consecutiveErrors++;
            scheduleRecognition(retryDelay(consecutiveErrors));
        }
    };

    private void scheduleRecognition(long delayMs) {
        handler.removeCallbacks(startRecognitionRunnable);
        if (!active || destroyed || pausedForTts) return;
        handler.postDelayed(startRecognitionRunnable, delayMs);
    }

    private long retryDelay(int failures) {
        return Math.min(3500L, 350L * Math.max(1, failures));
    }

    private void initTextToSpeech() {
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.SUCCESS || tts == null) {
                ttsReady = false;
                return;
            }
            int result = tts.setLanguage(Locale.forLanguageTag("tr-TR"));
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
            tts.setSpeechRate(0.98f);
            tts.setPitch(0.95f);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String utteranceId) { }
                @Override public void onDone(String utteranceId) { resumeAfterSpeech(); }
                @Override public void onError(String utteranceId) { resumeAfterSpeech(); }
            });
        });
    }

    private void resumeAfterSpeech() {
        handler.post(() -> {
            pausedForTts = false;
            scheduleRecognition(450L);
        });
    }

    private void speak(String text) {
        broadcast(text, null, true);
        if (!ttsReady || tts == null) {
            scheduleRecognition(300L);
            return;
        }
        pausedForTts = true;
        listening = false;
        if (recognizer != null) {
            try { recognizer.cancel(); } catch (RuntimeException ignored) { }
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, new Bundle(), UTTERANCE_ID + System.nanoTime());
    }

    private void handleUtterance(String utterance) {
        WakeSession.Decision decision = wakeSession.onUtterance(utterance, System.currentTimeMillis());
        if (decision.type() == WakeSession.DecisionType.IGNORE) {
            broadcast("Jarvis demeni bekliyorum", utterance, true);
            scheduleRecognition(250L);
            return;
        }
        if (decision.type() == WakeSession.DecisionType.ARM) {
            broadcast("Dinliyorum…", utterance, true);
            speak("Dinliyorum");
            return;
        }
        executePlan(decision.command(), 0);
        appendHistory(decision.command());
    }

    private void executePlan(String rawCommand, int routineDepth) {
        List<CommandResult> actions = CommandPlanner.plan(rawCommand);
        for (CommandResult action : actions) {
            executeCommand(action, routineDepth);
        }
    }

    private void executeCommand(CommandResult command, int routineDepth) {
        switch (command.type()) {
            case TIME:
                speak("Saat " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
                break;
            case DATE:
                speak("Bugün " + LocalDate.now().format(DateTimeFormatter.ofPattern(
                        "d MMMM yyyy EEEE", Locale.forLanguageTag("tr-TR"))));
                break;
            case OPEN_APP:
                speak(openApp(command.text())
                        ? capitalize(command.text()) + " açılıyor"
                        : command.text() + " adlı uygulamayı bulamadım");
                break;
            case WEB_SEARCH:
                speak(openWebSearch(command.text())
                        ? "Arıyorum: " + command.text()
                        : "Web aramasını açamadım");
                break;
            case SET_ALARM:
                speak(openAlarm(command.hour(), command.minute())
                        ? String.format(Locale.forLanguageTag("tr-TR"),
                        "%02d:%02d için alarmı kuruyorum", command.hour(), command.minute())
                        : "Alarm uygulamasını açamadım");
                break;
            case SET_TIMER:
                speak(openTimer(command.value())
                        ? formatDuration(command.value()) + " zamanlayıcı başlatıldı"
                        : "Zamanlayıcıyı başlatamadım");
                break;
            case OPEN_SETTINGS:
                speak(openSettings(command.text()) ? "Ayarlar açılıyor" : "Ayarları açamadım");
                break;
            case FLASHLIGHT_ON:
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    speak("Fener için kamera izni gerekli");
                } else {
                    speak(setFlashlight(true) ? "Fener açıldı" : "Feneri açamadım");
                }
                break;
            case FLASHLIGHT_OFF:
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    speak("Fener için kamera izni gerekli");
                } else {
                    speak(setFlashlight(false) ? "Fener kapatıldı" : "Feneri kapatamadım");
                }
                break;
            case VOLUME_UP:
                speak(adjustVolume(AudioManager.ADJUST_RAISE, false) ? "Ses yükseltildi" : "Sesi değiştiremedim");
                break;
            case VOLUME_DOWN:
                speak(adjustVolume(AudioManager.ADJUST_LOWER, false) ? "Ses azaltıldı" : "Sesi değiştiremedim");
                break;
            case VOLUME_MUTE:
                speak(adjustVolume(AudioManager.ADJUST_SAME, true) ? "Medya sesi kapatıldı" : "Sesi kapatamadım");
                break;
            case VOLUME_MAX:
                speak(setVolumeMaximum() ? "Medya sesi maksimum" : "Sesi değiştiremedim");
                break;
            case BATTERY_STATUS:
                int battery = batteryPercentage();
                speak(battery >= 0 ? "Pil yüzde " + battery : "Pil durumunu okuyamadım");
                break;
            case OPEN_CAMERA:
                speak(openCamera() ? "Kamera açılıyor" : "Kamerayı açamadım");
                break;
            case NAVIGATE_TO:
                speak(openNavigation(command.text())
                        ? command.text() + " için yol tarifi açılıyor"
                        : "Haritayı açamadım");
                break;
            case DIAL_NUMBER:
                speak(openDialer(command.text())
                        ? command.text() + " aranmak üzere açıldı"
                        : "Telefon uygulamasını açamadım");
                break;
            case DIAL_CONTACT:
                dialContact(command.text());
                break;
            case COMPOSE_SMS:
                speak(composeSms(command.text(), command.secondaryText())
                        ? "Mesaj ekranı açılıyor"
                        : "Mesaj uygulamasını açamadım");
                break;
            case COMPOSE_SMS_CONTACT:
                messageContact(command.text(), command.secondaryText());
                break;
            case MEDIA_PLAY_PAUSE:
                speak(sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                        ? "Medya oynatma durumu değiştirildi"
                        : "Medya kontrol edilemedi");
                break;
            case MEDIA_NEXT:
                speak(sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT) ? "Sonraki parça" : "Medya kontrol edilemedi");
                break;
            case MEDIA_PREVIOUS:
                speak(sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS) ? "Önceki parça" : "Medya kontrol edilemedi");
                break;
            case CREATE_NOTE:
                saveNote(command.text());
                speak("Not aldım: " + command.text());
                break;
            case READ_LAST_NOTE:
                String note = readLastNote();
                speak(note.isEmpty() ? "Kayıtlı not bulamadım" : "Son notun: " + note);
                break;
            case SET_REMINDER:
                speak(scheduleReminder(command)
                        ? reminderConfirmation(command)
                        : "Hatırlatıcıyı oluşturamadım");
                break;
            case ADD_CALENDAR_EVENT:
                speak(openCalendarEvent(command)
                        ? "Takvim kaydı hazırlanıyor: " + command.text()
                        : "Takvimi açamadım");
                break;
            case SAVE_ROUTINE:
                speak(saveRoutine(command.text(), command.secondaryText())
                        ? command.text() + " rutini kaydedildi"
                        : "Rutini kaydedemedim");
                break;
            case RUN_ROUTINE:
                runRoutine(command.text(), routineDepth);
                break;
            case LIST_ROUTINES:
                speak(listRoutines());
                break;
            case DELETE_ROUTINE:
                speak(deleteRoutine(command.text())
                        ? command.text() + " rutini silindi"
                        : command.text() + " adlı rutin bulunamadı");
                break;
            case READ_HISTORY:
                speak(readHistory());
                break;
            case CLEAR_HISTORY:
                clearHistory();
                speak("Komut geçmişi temizlendi");
                break;
            case HELP:
                speak("Uygulama ve ayar açabilir, webde arayabilir, alarm, zamanlayıcı ve hatırlatıcı kurabilir, " +
                        "fener, ses ve müziği kontrol edebilir, pil durumunu söyleyebilir, kamera ve yol tarifi açabilir, " +
                        "kişilerinden arama ve mesaj hazırlayabilir, takvime etkinlik ekleyebilir, not alabilir, " +
                        "rutin kaydedebilir ve tek cümlede birden fazla komutu sırayla çalıştırabilirim");
                break;
        }
    }

    private boolean openApp(String requestedName) {
        try {
            PackageManager pm = getPackageManager();
            String requested = normalizeName(requestedName);
            String known = knownPackages.get(requested);
            if (known != null) {
                Intent launch = pm.getLaunchIntentForPackage(known);
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launch);
                    return true;
                }
            }

            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> candidates = Build.VERSION.SDK_INT >= 33
                    ? pm.queryIntentActivities(query, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL))
                    : pm.queryIntentActivities(query, PackageManager.MATCH_ALL);
            ResolveInfo best = null;
            int bestScore = Integer.MAX_VALUE;
            for (ResolveInfo info : candidates) {
                CharSequence labelSequence = info.loadLabel(pm);
                if (labelSequence == null) continue;
                int score = matchScore(requested, normalizeName(labelSequence.toString()));
                if (score < bestScore) {
                    best = info;
                    bestScore = score;
                }
            }
            if (best == null || bestScore >= 1000) return false;
            Intent launch = pm.getLaunchIntentForPackage(best.activityInfo.packageName);
            if (launch == null) return false;
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }

    private int matchScore(String requested, String label) {
        if (label.equals(requested)) return 0;
        if (label.startsWith(requested) || requested.startsWith(label)) {
            return 10 + Math.abs(label.length() - requested.length());
        }
        if (label.contains(requested) || requested.contains(label)) {
            return 100 + Math.abs(label.length() - requested.length());
        }
        return 1000;
    }

    private boolean openWebSearch(String query) {
        try {
            Intent webSearch = new Intent(Intent.ACTION_WEB_SEARCH)
                    .putExtra(SearchManager.QUERY, query)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (webSearch.resolveActivity(getPackageManager()) != null) {
                startActivity(webSearch);
                return true;
            }
            Intent browser = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (browser.resolveActivity(getPackageManager()) != null) {
                startActivity(browser);
                return true;
            }
        } catch (RuntimeException ignored) { }
        return false;
    }

    private boolean openAlarm(int hour, int minute) {
        try {
            Intent alarm = new Intent(AlarmClock.ACTION_SET_ALARM)
                    .putExtra(AlarmClock.EXTRA_HOUR, hour)
                    .putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    .putExtra(AlarmClock.EXTRA_MESSAGE, "Jarvis")
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (alarm.resolveActivity(getPackageManager()) == null) return false;
            startActivity(alarm);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean openTimer(int seconds) {
        if (seconds <= 0) return false;
        try {
            Intent timer = new Intent(AlarmClock.ACTION_SET_TIMER)
                    .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                    .putExtra(AlarmClock.EXTRA_MESSAGE, "Jarvis")
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (timer.resolveActivity(getPackageManager()) == null) return false;
            startActivity(timer);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int remaining = seconds % 60;
        StringBuilder builder = new StringBuilder();
        if (hours > 0) builder.append(hours).append(" saat ");
        if (minutes > 0) builder.append(minutes).append(" dakika ");
        if (remaining > 0) builder.append(remaining).append(" saniye");
        return builder.toString().trim();
    }

    private boolean setFlashlight(boolean enabled) {
        try {
            CameraManager manager = getSystemService(CameraManager.class);
            if (manager == null) return false;
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics c = manager.getCameraCharacteristics(id);
                Boolean flash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing = c.get(CameraCharacteristics.LENS_FACING);
                if (Boolean.TRUE.equals(flash)
                        && (facing == null || facing == CameraCharacteristics.LENS_FACING_BACK)) {
                    manager.setTorchMode(id, enabled);
                    return true;
                }
            }
        } catch (Exception ignored) { }
        return false;
    }

    private boolean adjustVolume(int direction, boolean mute) {
        try {
            AudioManager audio = getSystemService(AudioManager.class);
            if (audio == null) return false;
            if (mute) {
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);
            } else {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI);
            }
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean setVolumeMaximum() {
        try {
            AudioManager audio = getSystemService(AudioManager.class);
            if (audio == null) return false;
            audio.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    AudioManager.FLAG_SHOW_UI);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private int batteryPercentage() {
        try {
            BatteryManager manager = getSystemService(BatteryManager.class);
            if (manager != null) {
                int value = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                if (value >= 0 && value <= 100) return value;
            }
            Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery == null) return -1;
            int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            return level >= 0 && scale > 0 ? Math.round(level * 100f / scale) : -1;
        } catch (RuntimeException ignored) {
            return -1;
        }
    }

    private boolean openCamera() {
        try {
            Intent camera = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (camera.resolveActivity(getPackageManager()) == null) return false;
            startActivity(camera);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean openNavigation(String destination) {
        try {
            Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(destination));
            Intent map = new Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (map.resolveActivity(getPackageManager()) == null) return false;
            startActivity(map);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean openDialer(String number) {
        try {
            Intent dial = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (dial.resolveActivity(getPackageManager()) == null) return false;
            startActivity(dial);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean composeSms(String number, String message) {
        try {
            Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (message != null && !message.trim().isEmpty()) sms.putExtra("sms_body", message);
            if (sms.resolveActivity(getPackageManager()) == null) return false;
            startActivity(sms);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void dialContact(String requestedName) {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            speak("İsimden arama için kişiler izni gerekli");
            return;
        }
        String number = resolveContactPhone(requestedName);
        if (number.isEmpty()) {
            speak(requestedName + " adlı kişiyi bulamadım");
            return;
        }
        speak(openDialer(number)
                ? requestedName + " aranmak üzere açıldı"
                : "Telefon uygulamasını açamadım");
    }

    private void messageContact(String requestedName, String body) {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            speak("İsimden mesaj için kişiler izni gerekli");
            return;
        }
        String number = resolveContactPhone(requestedName);
        if (number.isEmpty()) {
            speak(requestedName + " adlı kişiyi bulamadım");
            return;
        }
        speak(composeSms(number, body) ? "Mesaj ekranı açılıyor" : "Mesaj uygulamasını açamadım");
    }

    private String resolveContactPhone(String requestedName) {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return "";
        String requested = normalizeContactAlias(requestedName);
        String firstCandidate = "";
        Cursor cursor = null;
        try {
            String[] projection = {
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
            };
            cursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE ASC");
            if (cursor == null) return "";
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            while (cursor.moveToNext()) {
                String name = nameIndex >= 0 ? cursor.getString(nameIndex) : null;
                String number = numberIndex >= 0 ? cursor.getString(numberIndex) : null;
                if (name == null || number == null || number.trim().isEmpty()) continue;
                String normalized = normalizeName(name);
                if (normalized.equals(requested)) return number;
                if (firstCandidate.isEmpty()
                        && (normalized.contains(requested) || requested.contains(normalized))) {
                    firstCandidate = number;
                }
            }
        } catch (SecurityException | RuntimeException ignored) {
            return "";
        } finally {
            if (cursor != null) cursor.close();
        }
        return firstCandidate;
    }

    private String normalizeContactAlias(String input) {
        String normalized = normalizeName(input);
        switch (normalized) {
            case "annem": return "anne";
            case "babam": return "baba";
            case "abim": return "abi";
            case "ablam": return "abla";
            case "esim": return "es";
            default: return normalized;
        }
    }

    private boolean sendMediaKey(int keyCode) {
        try {
            AudioManager audio = getSystemService(AudioManager.class);
            if (audio == null) return false;
            long now = android.os.SystemClock.uptimeMillis();
            audio.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
            audio.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0));
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void saveNote(String note) {
        try {
            getSharedPreferences(NOTES_PREFS, MODE_PRIVATE).edit()
                    .putString(KEY_LAST_NOTE, note)
                    .putLong(KEY_LAST_NOTE_TIME, System.currentTimeMillis())
                    .apply();
        } catch (RuntimeException ignored) { }
    }

    private String readLastNote() {
        try {
            return getSharedPreferences(NOTES_PREFS, MODE_PRIVATE).getString(KEY_LAST_NOTE, "");
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private boolean scheduleReminder(CommandResult command) {
        long triggerAt;
        if ("relative".equals(command.secondaryText())) {
            if (command.value() <= 0) return false;
            triggerAt = System.currentTimeMillis() + command.value() * 1000L;
        } else if ("absolute".equals(command.secondaryText())) {
            if (command.hour() < 0 || command.minute() < 0 || command.value() < 0) return false;
            LocalDateTime dateTime = LocalDate.now()
                    .plusDays(command.value())
                    .atTime(command.hour(), command.minute());
            triggerAt = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            if (triggerAt <= System.currentTimeMillis()) return false;
        } else {
            return false;
        }

        try {
            AlarmManager manager = getSystemService(AlarmManager.class);
            if (manager == null) return false;
            int requestCode = (int) (System.currentTimeMillis() & 0x7fffffff);
            Intent reminder = new Intent(this, ReminderReceiver.class)
                    .putExtra(ReminderReceiver.EXTRA_TEXT, command.text());
            PendingIntent pending = PendingIntent.getBroadcast(
                    this,
                    requestCode,
                    reminder,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending);
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }

    private String reminderConfirmation(CommandResult command) {
        if ("relative".equals(command.secondaryText())) {
            return formatDuration(command.value()) + " sonra hatırlatacağım: " + command.text();
        }
        return String.format(
                Locale.forLanguageTag("tr-TR"),
                "%02d:%02d için hatırlatıcı hazır: %s",
                command.hour(),
                command.minute(),
                command.text());
    }

    private boolean openCalendarEvent(CommandResult command) {
        try {
            Intent calendar = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, command.text())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            LocalDate date = LocalDate.now().plusDays(Math.max(0, command.value()));
            if (command.hour() >= 0 && command.minute() >= 0) {
                long begin = date.atTime(command.hour(), command.minute())
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                calendar.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin);
                calendar.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, begin + 60L * 60L * 1000L);
            } else {
                long begin = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                calendar.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin);
                calendar.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true);
            }
            startActivity(calendar);
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }

    private boolean saveRoutine(String name, String body) {
        String key = routineKey(name);
        if (key.isEmpty() || key.length() > 80 || body == null || body.trim().isEmpty() || body.length() > 1500) {
            return false;
        }
        try {
            getSharedPreferences(ROUTINES_PREFS, MODE_PRIVATE).edit().putString(key, body.trim()).apply();
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }

    private void runRoutine(String name, int routineDepth) {
        if (routineDepth >= MAX_ROUTINE_DEPTH) {
            speak("Rutin güvenlik sınırına ulaştı; sonsuz döngüyü durdurdum");
            return;
        }
        String body;
        try {
            body = getSharedPreferences(ROUTINES_PREFS, MODE_PRIVATE).getString(routineKey(name), "");
        } catch (RuntimeException error) {
            body = "";
        }
        if (body == null || body.trim().isEmpty()) {
            speak(name + " adlı rutin bulunamadı");
            return;
        }
        executePlan(body, routineDepth + 1);
    }

    private String listRoutines() {
        try {
            Map<String, ?> all = getSharedPreferences(ROUTINES_PREFS, MODE_PRIVATE).getAll();
            if (all.isEmpty()) return "Kayıtlı rutin yok";
            List<String> names = new ArrayList<>(all.keySet());
            Collections.sort(names);
            return "Rutinlerin: " + String.join(", ", names);
        } catch (RuntimeException error) {
            return "Rutinleri okuyamadım";
        }
    }

    private boolean deleteRoutine(String name) {
        String key = routineKey(name);
        try {
            SharedPreferences prefs = getSharedPreferences(ROUTINES_PREFS, MODE_PRIVATE);
            if (!prefs.contains(key)) return false;
            prefs.edit().remove(key).apply();
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }

    private String routineKey(String name) {
        if (name == null) return "";
        return name.toLowerCase(Locale.forLanguageTag("tr-TR"))
                .replace('\n', ' ')
                .trim()
                .replaceAll("\\s+", " ");
    }

    private void appendHistory(String rawCommand) {
        if (rawCommand == null) return;
        String clean = rawCommand.replace('\n', ' ').trim();
        if (clean.isEmpty()) return;
        if (clean.length() > 300) clean = clean.substring(0, 300);
        try {
            SharedPreferences prefs = getSharedPreferences(HISTORY_PREFS, MODE_PRIVATE);
            String existing = prefs.getString(KEY_HISTORY, "");
            List<String> entries = new ArrayList<>();
            if (existing != null && !existing.isEmpty()) {
                Collections.addAll(entries, existing.split("\\n"));
            }
            entries.add(clean);
            while (entries.size() > MAX_HISTORY) entries.remove(0);
            prefs.edit().putString(KEY_HISTORY, String.join("\n", entries)).apply();
        } catch (RuntimeException ignored) { }
    }

    private String readHistory() {
        try {
            String stored = getSharedPreferences(HISTORY_PREFS, MODE_PRIVATE).getString(KEY_HISTORY, "");
            if (stored == null || stored.trim().isEmpty()) return "Komut geçmişi boş";
            String[] entries = stored.split("\\n");
            int start = Math.max(0, entries.length - 5);
            StringBuilder out = new StringBuilder("Son komutların: ");
            for (int i = entries.length - 1; i >= start; i--) {
                if (out.length() > "Son komutların: ".length()) out.append(". ");
                out.append(entries[i]);
            }
            return out.toString();
        } catch (RuntimeException error) {
            return "Komut geçmişini okuyamadım";
        }
    }

    private void clearHistory() {
        try {
            getSharedPreferences(HISTORY_PREFS, MODE_PRIVATE).edit().remove(KEY_HISTORY).apply();
        } catch (RuntimeException ignored) { }
    }

    private boolean openSettings(String kind) {
        String action;
        switch (kind) {
            case "bluetooth": action = Settings.ACTION_BLUETOOTH_SETTINGS; break;
            case "wifi": action = Settings.ACTION_WIFI_SETTINGS; break;
            case "location": action = Settings.ACTION_LOCATION_SOURCE_SETTINGS; break;
            case "display": action = Settings.ACTION_DISPLAY_SETTINGS; break;
            case "sound": action = Settings.ACTION_SOUND_SETTINGS; break;
            default: action = Settings.ACTION_SETTINGS; break;
        }
        try {
            Intent settings = new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (settings.resolveActivity(getPackageManager()) == null) return false;
            startActivity(settings);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void initKnownPackages() {
        knownPackages.put("spotify", "com.spotify.music");
        knownPackages.put("youtube", "com.google.android.youtube");
        knownPackages.put("whatsapp", "com.whatsapp");
        knownPackages.put("instagram", "com.instagram.android");
        knownPackages.put("chrome", "com.android.chrome");
        knownPackages.put("haritalar", "com.google.android.apps.maps");
        knownPackages.put("maps", "com.google.android.apps.maps");
        knownPackages.put("gmail", "com.google.android.gm");
        knownPackages.put("telegram", "org.telegram.messenger");
        knownPackages.put("tiktok", "com.zhiliaoapp.musically");
        knownPackages.put("x", "com.twitter.android");
        knownPackages.put("twitter", "com.twitter.android");
    }

    private String normalizeName(String input) {
        return Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('ı', 'i')
                .replace('ş', 's')
                .replace('ğ', 'g')
                .replace('ç', 'c')
                .replace('ö', 'o')
                .replace('ü', 'u')
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private String capitalize(String text) {
        if (text == null || text.trim().isEmpty()) return "Uygulama";
        return text.substring(0, 1).toUpperCase(Locale.forLanguageTag("tr-TR")) + text.substring(1);
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.notification_channel_description));
        channel.setSound(null, null);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stopIntent = PendingIntent.getService(
                this,
                1,
                new Intent(this, JarvisListeningService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_jarvis)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.ic_jarvis),
                        getString(R.string.notification_stop),
                        stopIntent).build())
                .build();
    }

    private void broadcast(String status, String transcript, boolean isActive) {
        Intent intent = new Intent(ACTION_STATUS).setPackage(getPackageName());
        intent.putExtra(EXTRA_STATUS, status);
        intent.putExtra(EXTRA_ACTIVE, isActive);
        if (transcript != null) intent.putExtra(EXTRA_TRANSCRIPT, transcript);
        sendBroadcast(intent);
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        consecutiveErrors = 0;
        broadcast(
                wakeSession.isArmed(System.currentTimeMillis())
                        ? "Komutunu dinliyorum…"
                        : "Jarvis demeni bekliyorum",
                null,
                true);
    }

    @Override public void onBeginningOfSpeech() { }
    @Override public void onRmsChanged(float rmsdB) { }
    @Override public void onBufferReceived(byte[] buffer) { }
    @Override public void onEndOfSpeech() { }

    @Override
    public void onError(int error) {
        listening = false;
        if (!active || destroyed || pausedForTts) return;
        consecutiveErrors++;
        long delay;
        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            delay = 250L;
        } else if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            delay = 1400L;
        } else if (error == SpeechRecognizer.ERROR_NETWORK || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) {
            delay = 1800L;
            broadcast("Çevrimdışı tanıma hazır değilse internet gerekebilir", null, true);
        } else if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            setRequested(false);
            broadcast("Mikrofon izni gerekli", null, false);
            stopSelf();
            return;
        } else {
            delay = retryDelay(consecutiveErrors);
        }
        scheduleRecognition(delay);
    }

    @Override
    public void onResults(Bundle results) {
        listening = false;
        consecutiveErrors = 0;
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches == null || matches.isEmpty()) {
            scheduleRecognition(250L);
            return;
        }
        String best = matches.get(0);
        broadcast("Duydum", best, true);
        handleUtterance(best);
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            broadcast(
                    wakeSession.isArmed(System.currentTimeMillis())
                            ? "Komutunu dinliyorum…"
                            : "Dinliyorum…",
                    matches.get(0),
                    true);
        }
    }

    @Override public void onEvent(int eventType, Bundle params) { }
}
