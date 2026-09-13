package app.namaz.tr;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class WidgetDataRepository {
    private WidgetDataRepository() {}

    public static final class Snapshot {
        public final String city;
        public final String hijri;
        public final String gregorian;
        public final String nextPrayerName;
        public final String nextPrayerTime;
        public final long nextPrayerEpoch;
        public final String imsak;
        public final String gunes;
        public final String ogle;
        public final String ikindi;
        public final String aksam;
        public final String yatsi;
        public final int tracked;
        public final String dailyAyahText;
        public final String dailyAyahSource;
        public final String quranResumeText;

        Snapshot(String city, String hijri, String gregorian,
                 String nextPrayerName, String nextPrayerTime, long nextPrayerEpoch,
                 String imsak, String gunes, String ogle, String ikindi, String aksam, String yatsi,
                 int tracked, String dailyAyahText, String dailyAyahSource, String quranResumeText) {
            this.city = city;
            this.hijri = hijri;
            this.gregorian = gregorian;
            this.nextPrayerName = nextPrayerName;
            this.nextPrayerTime = nextPrayerTime;
            this.nextPrayerEpoch = nextPrayerEpoch;
            this.imsak = imsak;
            this.gunes = gunes;
            this.ogle = ogle;
            this.ikindi = ikindi;
            this.aksam = aksam;
            this.yatsi = yatsi;
            this.tracked = tracked;
            this.dailyAyahText = dailyAyahText;
            this.dailyAyahSource = dailyAyahSource;
            this.quranResumeText = quranResumeText;
        }
    }

    public static Snapshot load(Context context) {
        SharedPreferences p = context.getSharedPreferences(PrayerScheduler.PREFS, Context.MODE_PRIVATE);
        String gregorian = new SimpleDateFormat("d MMMM yyyy", new Locale("tr", "TR")).format(new Date());
        int tracked = Math.max(0, Math.min(5, p.getInt("widgetTracked", 0)));
        return new Snapshot(
                value(p.getString("widgetCity", ""), "Konum seçilmedi"),
                value(p.getString("widgetHijri", ""), "Hicrî tarih güncellenecek"),
                gregorian,
                value(p.getString("nextPrayerName", ""), "Sıradaki namaz"),
                value(p.getString("nextPrayerTime", ""), "—"),
                p.getLong("nextPrayerEpoch", 0L),
                value(p.getString("widgetImsak", ""), "—"),
                value(p.getString("widgetGunes", ""), "—"),
                value(p.getString("widgetOgle", ""), "—"),
                value(p.getString("widgetIkindi", ""), "—"),
                value(p.getString("widgetAksam", ""), "—"),
                value(p.getString("widgetYatsi", ""), "—"),
                tracked,
                safe(p.getString("dailyAyahText", "")),
                safe(p.getString("dailyAyahSource", "")),
                value(p.getString("widgetQuranResume", ""), "Henüz okuma kaydı yok")
        );
    }

    public static String remainingText(long epoch) {
        long ms = epoch - System.currentTimeMillis();
        if (epoch <= 0L || ms <= 0L) return "Vakitleri güncellemek için Namaz'ı aç";
        long totalMinutes = Math.max(0L, ms / 60000L);
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        return hours > 0L ? hours + " sa " + minutes + " dk kaldı" : minutes + " dk kaldı";
    }

    private static String value(String value, String fallback) {
        String clean = safe(value);
        return clean.isEmpty() ? fallback : clean;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
