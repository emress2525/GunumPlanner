package com.tevkil.radar;

import java.time.LocalDate;
import java.time.DateTimeException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Guard for explicit hearing/discovery dates in ad text; absence of a date is unknown, not expired. */
public final class TevkilDateGate {
    private static final Pattern APPOINTMENT = Pattern.compile(
        "(?iu)(?:duruşma\\s*tarihi|duruşma|keşif\\s*tarihi|keşif\\s*günü)\\s*:?\\s*(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4})");
    private TevkilDateGate() {}

    public static boolean isExpired(String text, LocalDate today) {
        if (text == null || today == null) return false;
        Matcher m = APPOINTMENT.matcher(text.replace('\n', ' '));
        while (m.find()) {
            try {
                LocalDate hearing = LocalDate.of(Integer.parseInt(m.group(3)),
                        Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
                if (hearing.isBefore(today)) return true;
            } catch (DateTimeException | NumberFormatException ignored) {
                // OCR may garble a date; uncertain input must not be asserted expired.
            }
        }
        return false;
    }
}
