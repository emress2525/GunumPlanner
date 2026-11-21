import com.tevkil.radar.FilterEngine;

public class FilterEngineTest {
  static void check(boolean expected, boolean actual, String caseName) {
    if (expected != actual) throw new AssertionError(caseName + ": expected " + expected + " got " + actual);
  }
  static void fee(long expected, long actual, String name) {
    if (expected != actual) throw new AssertionError(name+": expected "+expected+" got "+actual);
  }
  public static void main(String[] args) {
    FilterEngine.Rules r = new FilterEngine.Rules();
    check(true, FilterEngine.matches("Ankara Tevkil", "Çankaya 5. İş Mahkemesi duruşma, 1.500 TL", r), "ankara positive");
    check(true, FilterEngine.matches("Avukat grubu", "Keçiören icra müdürlüğü için tevkil", r), "district positive");
    check(false, FilterEngine.matches("İstanbul tevkil", "Kadıköy mahkemesi için duruşma", r), "outside Ankara");
    check(false, FilterEngine.matches("Ankara","Bugün güneşli bir hava var",r), "irrelevant");
    check(true, FilterEngine.matches("İş", "Cankaya durusma vekil lazim",r), "ascii Turkish");
    r.exclude = "icra";
    check(false, FilterEngine.matches("Ankara icra", "İcra tevkil ilanı",r), "negative keyword");
    r.exclude = "";
    r.districts = "çankaya; etimesgut";
    check(false, FilterEngine.matches("Ankara", "Mamak icra duruşma",r), "district filter negative");
    check(true, FilterEngine.matches("Ankara", "Etimesgut icra duruşma",r), "district filter positive");
    r.districts = "";
    r.minFee = 1000;
    check(false, FilterEngine.matches("Ankara", "Duruşma ücreti 750 TL",r), "min fee negative");
    check(true, FilterEngine.matches("Ankara", "Duruşma ücreti 1.500 TL",r), "min fee positive");
    fee(1500, FilterEngine.advertisedFee("Duruşma ücreti 1.500 TL"), "fee punctuation");
    fee(2500, FilterEngine.advertisedFee("Duruşma ₺2500"), "fee symbol");
    check(true, FilterEngine.sourceName("com.whatsapp") != null, "WA package");
    check(true, FilterEngine.sourceName("org.telegram.messenger") != null, "TG package");
    check(true, FilterEngine.sourceName("com.instagram.android") != null, "IG package");
    System.out.println("12 filter tests passed");
  }
}
