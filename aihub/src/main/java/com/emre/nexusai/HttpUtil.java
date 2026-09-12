package com.emre.nexusai;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public final class HttpUtil {
    public static final int MAX_RESPONSE_BYTES = 1_000_000;
    public static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String USER_AGENT = "NexusAI/2.1 Android";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get(JSON_CONTENT_TYPE);

    private HttpUtil() { }

    public static Response get(String url, int connectTimeout, int readTimeout) throws Exception {
        HttpsUrlValidator.requireSafe(url, "url");
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .get()
                .build();
        return executeText(request, connectTimeout, readTimeout);
    }

    public static Response postJson(String url, String body, int connectTimeout, int readTimeout) throws Exception {
        HttpsUrlValidator.requireSafe(url, "url");
        RequestBody requestBody = RequestBody.create(body == null ? "" : body, JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .post(requestBody)
                .build();
        return executeText(request, connectTimeout, readTimeout);
    }

    public static byte[] getBytes(String url, int connectTimeout, int readTimeout) throws Exception {
        return getBytes(url, connectTimeout, readTimeout, MAX_RESPONSE_BYTES);
    }

    public static byte[] getBytes(String url, int connectTimeout, int readTimeout, int maxBytes) throws Exception {
        HttpsUrlValidator.requireSafe(url, "url");
        if (maxBytes <= 0) throw new IllegalArgumentException("maxBytes pozitif olmalı");

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .get()
                .build();

        OkHttpClient client = clientForTests(connectTimeout, readTimeout);
        try (okhttp3.Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IllegalStateException("HTTP " + response.code());
            ResponseBody body = response.body();
            if (body == null) return new byte[0];
            return readBytes(body.byteStream(), maxBytes);
        }
    }

    static OkHttpClient clientForTests(int connectTimeout, int readTimeout) {
        return NetworkStack.client(connectTimeout, readTimeout);
    }

    private static Response executeText(Request request, int connectTimeout, int readTimeout) throws Exception {
        OkHttpClient client = clientForTests(connectTimeout, readTimeout);
        try (okhttp3.Response response = client.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String body = responseBody == null
                    ? ""
                    : new String(readBytes(responseBody.byteStream(), MAX_RESPONSE_BYTES), StandardCharsets.UTF_8);
            return new Response(response.code(), body);
        }
    }

    private static byte[] readBytes(InputStream in, int maxBytes) throws Exception {
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) throw new IllegalStateException("Yanıt çok büyük");
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    public static final class Response {
        public final int code;
        public final String body;

        public Response(int code, String body) {
            this.code = code;
            this.body = body == null ? "" : body;
        }

        public boolean isSuccessful() {
            return code >= 200 && code < 300;
        }
    }
}
