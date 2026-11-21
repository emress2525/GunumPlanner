import com.tevkil.radar.FilterEngine;
import com.tevkil.radar.TevkilOcrSummary;

public class TevkilOcrSummaryTest {
    private static void equalsText(String expected, String actual, String caseName) {
        if (!expected.equals(actual)) throw new AssertionError(caseName + ": expected '" + expected + "', got '" + actual + "'");
    }
    private static void check(boolean actual, String caseName) {
        if (!actual) throw new AssertionError(caseName);
    }
    public static void main(String[] args) {
        // Representative screenshot text: Istanbul case must not be misclassified as Ankara.
        String screenshot="Birim Adı  Bakırköy 6. Ağır Ceza Mahkemesi\n"+
                "Dosya No 2021/179\nDosya Türü Ceza Dava Dosyası\n"+
                "Duruşma Tarihi 18.11.2026 11:05\nİşlem Duruşma\n"+
                "Merhaba\nÜcret asgari tarife";
        TevkilOcrSummary summary=TevkilOcrSummary.fromText(screenshot);
        equalsText("Bakırköy 6. Ağır Ceza Mahkemesi",summary.court,"court");
        equalsText("2021/179",summary.fileNumber,"file");
        equalsText("18.11.2026 11:05",summary.hearingDate,"hearing");
        check(summary.fee == 0,"unknown fee must not be guessed");
        check(!FilterEngine.matches("Instagram · @tevkiliniz",screenshot,new FilterEngine.Rules()),"Istanbul must be filtered out");
        String ankara="Ankara Çankaya 3. İş Mahkemesi, Duruşma 19.11.2026 09:30 için tevkil isteniyor. Ücret 2.500 TL";
        TevkilOcrSummary s2=TevkilOcrSummary.fromText(ankara);
        equalsText("Çankaya 3. İş Mahkemesi",s2.court,"Ankara court");
        equalsText("19.11.2026 09:30",s2.hearingDate,"hearing without tarihi");
        check(s2.fee==2500,"fee TL");
        check(FilterEngine.matches("Instagram · @tevkiliniz",ankara,new FilterEngine.Rules()),"Ankara passes filters");
        String malformed="Duruşma günü verildi, yer bilinmiyor";
        TevkilOcrSummary missing=TevkilOcrSummary.fromText(malformed);
        check(missing.court.isEmpty() && missing.hearingDate.isEmpty(),"no fabricated data");
        System.out.println("10 OCR extraction/filter tests passed");
    }
}
