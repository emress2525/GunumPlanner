import com.tevkil.radar.PublicInstagramParser;
import java.util.List;

public class PublicInstagramParserTest {
    static void check(boolean b,String what) { if (!b) throw new AssertionError(what); }
    public static void main(String[] args) {
        String profile = "<a href=\"/p/CDAF012_abc/\">A</a> <a href=\"/reel/DKaf9MM3-11/\">B</a>"
                + "<a href=\"/p/CDAF012_abc/\">dup</a>";
        List<PublicInstagramParser.Link> links=PublicInstagramParser.links(profile);
        check(links.size()==2, "distinct links");
        check(links.get(0).code.equals("CDAF012_abc"), "first code");
        check(links.get(0).url.equals("https://www.instagram.com/p/CDAF012_abc/"), "canonical URL");
        check(PublicInstagramParser.links("<html>Please log in</html>").isEmpty(), "login no fabricated posts");
        check(PublicInstagramParser.links("\"shortcode\":\"CDeF5678\"").size()==1, "old metadata");
        String post="<meta content=\"Ankara Çankaya duruşma &amp; tevkil\" property=\"og:description\">"
            + "<meta property='og:image' content='https://scontent.cdninstagram.com/test.jpg'>";
        check(PublicInstagramParser.caption(post).equals("Ankara Çankaya duruşma & tevkil"),"description");
        check(PublicInstagramParser.image(post).endsWith("test.jpg"),"image");
        check(PublicInstagramParser.caption("<title>login</title>").isEmpty(),"no caption");
        System.out.println("7 public Instagram parser checks passed");
    }
}
