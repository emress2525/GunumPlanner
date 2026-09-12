package app.namaz.tr;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class PrayerMath {
    private PrayerMath() {}

    public static double normalizeDegrees(double value) {
        value %= 360.0;
        return value < 0.0 ? value + 360.0 : value;
    }

    public static double qiblaBearing(double lat, double lon) {
        final double kaabaLat = Math.toRadians(21.4225);
        final double kaabaLon = Math.toRadians(39.8262);
        double p1 = Math.toRadians(lat);
        double l1 = Math.toRadians(lon);
        double dLon = kaabaLon - l1;
        double y = Math.sin(dLon) * Math.cos(kaabaLat);
        double x = Math.cos(p1) * Math.sin(kaabaLat)
                - Math.sin(p1) * Math.cos(kaabaLat) * Math.cos(dLon);
        return normalizeDegrees(Math.toDegrees(Math.atan2(y, x)));
    }

    public static long combineDayAndTime(long dayEpochMs, String hhmm) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate date = Instant.ofEpochMilli(dayEpochMs).atZone(zone).toLocalDate();
        String[] parts = String.valueOf(hhmm).trim().split(":");
        if (parts.length < 2) throw new IllegalArgumentException("Expected HH:mm");
        int hour = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
        String minuteRaw = parts[1].replaceAll("[^0-9].*", "");
        int minute = Integer.parseInt(minuteRaw);
        LocalTime time = LocalTime.of(hour, minute);
        ZonedDateTime zdt = ZonedDateTime.of(date, time, zone);
        return zdt.toInstant().toEpochMilli();
    }
}
