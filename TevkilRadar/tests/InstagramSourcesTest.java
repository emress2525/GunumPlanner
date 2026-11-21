import com.tevkil.radar.InstagramSources;
import java.util.Objects;

public class InstagramSourcesTest {
  static int tests = 0;
  static void eq(Object want, Object got) { ++tests; if (!Objects.equals(want, got)) throw new AssertionError("Expected "+want+" but got "+got); }
  static void yes(boolean result) { eq(true, result); }
  public static void main(String[] args) {
    eq("tevkiliniz", InstagramSources.parse("https://www.instagram.com/tevkiliniz?vrfl=MWYycDRxcnYwMnU5MA=="));
    eq("tevkiliniz", InstagramSources.parse("@Tevkiliniz"));
    eq("tevkiliniz", InstagramSources.parse("instagram.com/tevkiliniz/"));
    eq(null, InstagramSources.parse("https://www.instagram.com/p/Cxyz/"));
    eq(null, InstagramSources.parse("https://www.instagram.com/reel/Cxyz/"));
    eq(null, InstagramSources.parse("https://other.example/tevkiliniz"));
    eq("https://www.instagram.com/tevkiliniz/", InstagramSources.profileUrl("@tevkiliniz"));
    eq("tevkiliniz", InstagramSources.add("tevkiliniz", "https://instagram.com/TEVKILINIZ/"));
    eq("tevkiliniz,other.account", InstagramSources.add("tevkiliniz", "@other.account"));
    eq("other.account", InstagramSources.remove("tevkiliniz,other.account", "tevkiliniz"));
    eq("", InstagramSources.remove("tevkiliniz", "tevkiliniz"));
    yes(InstagramSources.matchesNotification("tevkiliniz yeni gönderi paylaştı", "Ankara tevkil duruşma", "tevkiliniz"));
    yes(InstagramSources.matchesNotification("@TEVKILINIZ", "Ankara duruşma", "tevkiliniz"));
    eq(false, InstagramSources.matchesNotification("otheraccount", "Ankara duruşma", "tevkiliniz"));
    eq(false, InstagramSources.matchesNotification("tevkiliniz2", "Ankara duruşma", "tevkiliniz"));
    eq(false, InstagramSources.matchesNotification("", "Ankara duruşma", "tevkiliniz"));
    eq(false, InstagramSources.matchesNotification("tevkiliniz", "Ankara", ""));
    eq(null, InstagramSources.parse("../abc"));
    System.out.println(tests + " Instagram source tests passed");
  }
}
