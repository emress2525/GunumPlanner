package com.tevkil.radar;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure Java, best-effort extraction; never presents OCR as an authoritative case record. */
public final class TevkilOcrSummary {
    private static final Pattern COURT = Pattern.compile(
            "(?iu)([\\p{L}]{3,}\\s+\\d{1,3}\\.\\s+(?:Asliye\\s+)?(?:Ağır\\s+Ceza|Ceza|İş|Hukuk|Ticaret|İcra|Aile|İdare|Sulh\\s+Hukuk|Tüketici)\\s+Mahkemesi)");
    private static final Pattern FILE = Pattern.compile("(?iu)(?:Dosya\\s*(?:No|Numarası)|Esas\\s*No)\\s*:?\\s*(\\d{4}\\s*/\\s*\\d{1,7})");
    private static final Pattern DATE = Pattern.compile("(?iu)(?:Duruşma\\s*Tarihi|Duruşma|Tarih)\\s*:?\\s*(\\d{1,2}[./-]\\d{1,2}[./-]\\d{4}(?:\\s+\\d{1,2}[:.]\\d{2})?)");
    public final String court, fileNumber, hearingDate;
    public final long fee;

    private TevkilOcrSummary(String court, String fileNumber, String hearingDate, long fee) {
        this.court = court; this.fileNumber = fileNumber; this.hearingDate = hearingDate; this.fee = fee;
    }

    public static TevkilOcrSummary fromText(String input) {
        String clean = input == null ? "" : input.replace('\n', ' ').replace('\r',' ').replaceAll("\\s+", " ").trim();
        return new TevkilOcrSummary(find(COURT, clean), find(FILE, clean), find(DATE, clean), FilterEngine.advertisedFee(clean));
    }

    private static String find(Pattern pattern, String input) {
        Matcher match = pattern.matcher(input);
        return match.find() ? match.group(1).trim() : "";
    }

    public String asDisplay() {
        StringBuilder out = new StringBuilder();
        if (!court.isEmpty()) out.append("Mahkeme: ").append(court).append('\n');
        if (!fileNumber.isEmpty()) out.append("Dosya: ").append(fileNumber).append('\n');
        if (!hearingDate.isEmpty()) out.append("Tarih: ").append(hearingDate).append('\n');
        if (fee > 0) out.append("Ücret: ").append(fee).append(" TL\n");
        return out.toString();
    }
}
