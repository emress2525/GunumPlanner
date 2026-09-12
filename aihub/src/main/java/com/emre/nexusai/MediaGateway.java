package com.emre.nexusai;

import android.util.Base64;

import org.json.JSONObject;

public final class MediaGateway {
    public static final int CONNECT_TIMEOUT_MS = 10_000;
    public static final int READ_TIMEOUT_MS = 120_000;
    public static final int MAX_SOURCE_IMAGE_BYTES = 3_500_000;

    public static boolean hasSafeImageEndpoint(NexusConfig config) {
        return config != null && HttpsUrlValidator.isSafe(config.imageEndpoint);
    }

    public static boolean hasSafeVideoEndpoint(NexusConfig config) {
        return config != null && HttpsUrlValidator.isSafe(config.videoEndpoint);
    }

    public NexusResult generateImage(NexusConfig config, String prompt) {
        if (!hasSafeImageEndpoint(config)) return NexusResult.error("Görsel motoru şu an yapılandırılmamış.");
        try {
            JSONObject request = new JSONObject()
                    .put("prompt", prompt)
                    .put("ratio", "1:1");
            HttpUtil.Response response = HttpUtil.postJson(config.imageEndpoint, request.toString(), CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
            if (!response.isSuccessful()) return NexusResult.error("Görsel üretimi başarısız: HTTP " + response.code);
            JSONObject json = new JSONObject(response.body);
            if (!json.optBoolean("success", true)) return NexusResult.error("Görsel servisi isteği reddetti.");
            String imageUrl = json.optString("imageUrl", "").trim();
            if (!HttpsUrlValidator.isSafe(imageUrl)) return NexusResult.error("Görsel servisi geçerli bir sonuç döndürmedi.");
            return NexusResult.image(imageUrl);
        } catch (Exception ex) {
            return NexusResult.error("Görsel üretilemedi: " + safe(ex));
        }
    }

    public NexusResult generateVideo(NexusConfig config, String prompt) {
        if (!hasSafeImageEndpoint(config) || !hasSafeVideoEndpoint(config)) {
            return NexusResult.error("Video motoru şu an yapılandırılmamış.");
        }
        try {
            NexusResult image = generateImage(config, prompt);
            if (image.kind != NexusResult.Kind.IMAGE) return image;

            byte[] source = HttpUtil.getBytes(image.content, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS, MAX_SOURCE_IMAGE_BYTES);
            String imageBase64 = Base64.encodeToString(source, Base64.NO_WRAP);
            JSONObject request = new JSONObject()
                    .put("prompt", prompt)
                    .put("ratio", "9:16")
                    .put("duration", 5)
                    .put("imageBase64", imageBase64);
            HttpUtil.Response response = HttpUtil.postJson(config.videoEndpoint, request.toString(), CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
            if (!response.isSuccessful()) return NexusResult.error("Video üretimi başarısız: HTTP " + response.code);
            JSONObject json = new JSONObject(response.body);
            if (!json.optBoolean("success", true)) return NexusResult.error("Video servisi isteği reddetti.");
            String videoUrl = json.optString("videoUrl", "").trim();
            if (!HttpsUrlValidator.isSafe(videoUrl)) return NexusResult.error("Video servisi geçerli bir sonuç döndürmedi.");
            return NexusResult.video(videoUrl);
        } catch (Exception ex) {
            return NexusResult.error("Video üretilemedi: " + safe(ex));
        }
    }

    private static String safe(Exception ex) {
        String value = ex.getMessage();
        return value == null || value.trim().isEmpty() ? ex.getClass().getSimpleName() : value;
    }
}
