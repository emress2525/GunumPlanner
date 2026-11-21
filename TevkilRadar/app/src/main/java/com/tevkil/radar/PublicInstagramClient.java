package com.tevkil.radar;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Experimental, logged-out read of pages that Instagram voluntarily serves publicly.
 * No credentials, undocumented API, proxy evasion, session impersonation or CAPTCHA bypass.
 */
public final class PublicInstagramClient {
    public static final class Post {
        public final String caption, image;
        public Post(String caption, String image) { this.caption=caption; this.image=image; }
    }

    private PublicInstagramClient() {}

    public static List<PublicInstagramParser.Link> fetchIndex(String username) throws Exception {
        String handle=InstagramSources.parse(username);
        if (handle == null) throw new IllegalArgumentException("Geçersiz Instagram kullanıcı adı");
        String page=get("https://www.instagram.com/" + handle + "/");
        List<PublicInstagramParser.Link> links=PublicInstagramParser.links(page);
        if (links.isEmpty()) {
            throw new IllegalStateException("Profilde girişsiz okunabilen gönderi bağlantısı yok (Instagram erişimi sınırlıyor olabilir)");
        }
        return links;
    }

    public static Post fetchPost(PublicInstagramParser.Link link) throws Exception {
        if (link == null || link.url == null ||
            !link.url.matches("https://www\\.instagram\\.com/(?:p|reel)/[A-Za-z0-9_-]{5,24}/")) {
            throw new IllegalArgumentException("Gönderi bağlantısı güvenli biçimde doğrulanamadı");
        }
        String page=get(link.url);
        String caption=PublicInstagramParser.caption(page);
        String image=PublicInstagramParser.image(page);
        if (caption.trim().isEmpty() && image.isEmpty())
            throw new IllegalStateException("Gönderinin açıklaması veya görseli girişsiz okunamıyor");
        return new Post(caption,image);
    }

    private static String get(String address) throws Exception {
        URL url=new URL(address);
        for (int redirects=0; redirects<3; redirects++) {
            if (!"https".equalsIgnoreCase(url.getProtocol()) ||
                !("www.instagram.com".equalsIgnoreCase(url.getHost()) || "instagram.com".equalsIgnoreCase(url.getHost())))
                throw new SecurityException("Yalnızca resmî Instagram sayfaları okunabilir");
            HttpURLConnection con=(HttpURLConnection)url.openConnection();
            con.setInstanceFollowRedirects(false);
            con.setConnectTimeout(9000);
            con.setReadTimeout(14000);
            con.setRequestProperty("Accept", "text/html,application/xhtml+xml");
            con.setRequestProperty("Accept-Language", "tr-TR,tr;q=0.9,en;q=0.7");
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/125.0.0.0 Mobile Safari/537.36");
            try {
                int code=con.getResponseCode();
                if (code>=300 && code<400) {
                    String loc=con.getHeaderField("Location");
                    if (loc==null) throw new IllegalStateException("Instagram yönlendirme adresi bulunamadı");
                    URL next=new URL(url,loc);
                    if (next.toString().contains("/accounts/login"))
                        throw new IllegalStateException("Instagram giriş yapmadan gönderi erişimini engelledi");
                    url=next;
                    continue;
                }
                if (code==401 || code==403 || code==429)
                    throw new IllegalStateException("Instagram erişim izni vermedi: HTTP " + code);
                if (code!=200)
                    throw new IllegalStateException("Instagram sayfası açılamadı: HTTP " + code);
                String type=con.getContentType();
                if (type==null || !type.toLowerCase(Locale.ROOT).contains("text/html"))
                    throw new IllegalStateException("Instagram beklenmedik içerik döndürdü");
                try(InputStream in=con.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                    byte[] chunk=new byte[8192]; int n;
                    while((n=in.read(chunk))!=-1) {
                        if (out.size()+n>1800000)
                            throw new IllegalStateException("Instagram HTML sınırı aşıldı");
                        out.write(chunk,0,n);
                    }
                    String html=out.toString(StandardCharsets.UTF_8.name());
                    if (html.toLowerCase(Locale.ROOT).contains("<title>login • instagram</title>"))
                        throw new IllegalStateException("Instagram giriş duvarı gösterdi");
                    return html;
                }
            } finally { con.disconnect(); }
        }
        throw new IllegalStateException("Instagram çok fazla yönlendirme yaptı");
    }
}
