package app.namaz.tr;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class WidgetRenderUtils {
    private WidgetRenderUtils() {}

    public static PendingIntent openAppIntent(Context context, String destination, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (destination != null && !destination.trim().isEmpty()) {
            intent.putExtra("openScreen", destination.trim());
        }
        return PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static String trackerDots(int tracked) {
        int n = Math.max(0, Math.min(5, tracked));
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i > 0) b.append(' ');
            b.append(i < n ? '●' : '○');
        }
        return b.toString();
    }

    public static String hijriDay(String hijri) {
        if (hijri == null) return "—";
        String[] parts = hijri.trim().split("\\s+", 2);
        return parts.length == 0 || parts[0].isEmpty() ? "—" : parts[0];
    }

    public static String hijriRest(String hijri) {
        if (hijri == null) return "Hicrî tarih";
        String[] parts = hijri.trim().split("\\s+", 2);
        return parts.length > 1 ? parts[1] : "Hicrî tarih";
    }
}
