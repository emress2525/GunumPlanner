package com.tevkil.radar;

import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Official Meta Business Discovery API only; never scrapes Instagram HTML or uses passwords. */
public final class InstagramDiscoveryClient {
    // Upgrade API version after checking Meta's supported versions/deprecations.
    private static final String API_VERSION = "v25.0";

    public static final class Media {
        public final String id, caption, permalink;
        public final List<String> imageUrls;
        public Media(String id, String caption, String permalink, List<String> imageUrls) {
            this.id=id; this.caption=caption; this.permalink=permalink; this.imageUrls=imageUrls;
        }
    }
    private InstagramDiscoveryClient() {}

    public static List<Media> fetch(String professionalIgId, String token, String username) throws Exception {
        if (professionalIgId == null || !professionalIgId.matches("[0-9]{5,30}"))
            throw new IllegalArgumentException("Profesyonel IG kullanıcı kimliği geçersiz");
        if (token == null || token.trim().isEmpty()) throw new IllegalArgumentException("Meta erişim anahtarı eksik");
        String handle = InstagramSources.parse(username);
        if (handle == null) throw new IllegalArgumentException("Instagram hesap adı geçersiz");
        // Image URL can be missing for restricted content. Ask only for fields supported by Graph Media.
        String fields = "business_discovery.username(" + handle + "){username,media.limit(25){id,caption,permalink,timestamp,media_type,media_url,thumbnail_url,children.limit(5){media_type,media_url,thumbnail_url}}}";
        Uri uri = Uri.parse("https://graph.facebook.com/" + API_VERSION + "/" + professionalIgId)
                .buildUpon().appendQueryParameter("fields", fields).build();
        HttpURLConnection connection = (HttpURLConnection) new URL(uri.toString()).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(18000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + token.trim());
        connection.setRequestProperty("Accept", "application/json");
        try {
            int status = connection.getResponseCode();
            InputStream in = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String raw = readCapped(in, 384 * 1024);
            if (status != 200) {
                String description = "Meta HTTP " + status;
                try {
                    JSONObject err = new JSONObject(raw).optJSONObject("error");
                    if (err != null) description += " · hata kodu " + err.optInt("code", status);
                } catch (Exception ignored) {}
                throw new IllegalStateException(description + " (yetki, hesap türü veya API erişimini kontrol et)");
            }
            JSONObject root = new JSONObject(raw);
            JSONObject discovery = root.optJSONObject("business_discovery");
            if (discovery == null) throw new IllegalStateException("Meta bu hesaba Business Discovery erişimi vermedi");
            JSONObject media = discovery.optJSONObject("media");
            JSONArray data = media == null ? null : media.optJSONArray("data");
            List<Media> found = new ArrayList<>();
            if (data != null) for (int i=0; i<data.length(); i++) {
                JSONObject item = data.optJSONObject(i);
                if (item == null) continue;
                String id = item.optString("id", "");
                String caption = item.optString("caption", "");
                String permalink = item.optString("permalink", "");
                if (!id.isEmpty() && permalink.startsWith("https://www.instagram.com/")) {
                    List<String> images = new ArrayList<>();
                    collectImage(images, item);
                    JSONObject children = item.optJSONObject("children");
                    JSONArray nested = children == null ? null : children.optJSONArray("data");
                    if (nested != null) for (int j = 0; j < nested.length() && images.size() < 5; j++) {
                        JSONObject child = nested.optJSONObject(j);
                        if (child != null) collectImage(images, child);
                    }
                    found.add(new Media(id, caption, permalink, images));
                }
            }
            return found;
        } finally { connection.disconnect(); }
    }

    private static void collectImage(List<String> images, JSONObject item) {
        String kind = item.optString("media_type", "");
        String media = "IMAGE".equals(kind) ? item.optString("media_url", "") : "";
        // Covers can be OCR'd; video contents cannot be inferred from the cover.
        if (media.isEmpty()) media = item.optString("thumbnail_url", "");
        if (media.startsWith("https://") && !images.contains(media)) images.add(media);
    }

    private static String readCapped(InputStream in, int maxBytes) throws Exception {
        if (in == null) return "";
        try (InputStream input = in; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] block = new byte[4096]; int n;
            while ((n = input.read(block)) != -1) {
                if (output.size() + n > maxBytes) throw new IllegalStateException("API yanıt boyutu sınırı aşıldı");
                output.write(block, 0, n);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }
}
