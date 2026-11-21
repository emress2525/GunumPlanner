import com.tevkil.radar.TevkilDateGate;
import java.time.LocalDate;

public class TevkilDateGateTest {
    static void verify(boolean expected, boolean actual, String label) {
        if (expected != actual) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        LocalDate ref = LocalDate.of(2026, 11, 21);
        verify(true, TevkilDateGate.isExpired("Duruşma Tarihi 18.11.2026 11:05", ref), "expired hearing");
        verify(false, TevkilDateGate.isExpired("Duruşma 22.11.2026 10:00", ref), "future hearing");
        verify(false, TevkilDateGate.isExpired("Duruşma 21.11.2026", ref), "same-day hearing");
        verify(false, TevkilDateGate.isExpired("Görüşme lazım, tarih bilinmiyor", ref), "date missing");
        verify(false, TevkilDateGate.isExpired("Keşif 31.11.2026", ref), "malformed date");
        System.out.println("5 date checks passed");
    }
}
