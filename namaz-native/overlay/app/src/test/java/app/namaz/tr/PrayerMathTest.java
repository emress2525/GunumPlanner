package app.namaz.tr;

import org.junit.Test;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.Assert.*;

public class PrayerMathTest {
    @Test public void ankaraBearingIsSoutheast() {
        double bearing = PrayerMath.qiblaBearing(39.9334, 32.8597);
        assertTrue("Ankara qibla bearing should be around SSE", bearing > 150.0 && bearing < 170.0);
    }

    @Test public void normalizeDegreesWrapsBothDirections() {
        assertEquals(350.0, PrayerMath.normalizeDegrees(-10.0), 0.0001);
        assertEquals(10.0, PrayerMath.normalizeDegrees(370.0), 0.0001);
    }

    @Test public void combineDayAndTimeUsesLocalDay() {
        ZoneId zone = ZoneId.systemDefault();
        long noon = LocalDate.of(2026, 9, 13).atTime(12, 0).atZone(zone).toInstant().toEpochMilli();
        long actual = PrayerMath.combineDayAndTime(noon, "18:42");
        ZonedDateTime z = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(actual), zone);
        assertEquals(2026, z.getYear());
        assertEquals(9, z.getMonthValue());
        assertEquals(13, z.getDayOfMonth());
        assertEquals(18, z.getHour());
        assertEquals(42, z.getMinute());
    }
}
