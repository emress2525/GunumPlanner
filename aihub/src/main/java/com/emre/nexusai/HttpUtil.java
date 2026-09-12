package com.emre.nexusai;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class HttpUtil {
    public static final int MAX_RESPONSE_BYTES = 1_000_000;
    public static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

    private HttpUtil() { }

    public static Response get(String url, int connectTimeout, int readTimeout) throws Exception {
        HttpsUrlValidator.requireSafe(url, "url");
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        connection.setInstanceFollowRedirects(true);
        return read(connection);
    }

    public static Response postJson(String url, String body, int connectTimeout, int readTimeout) throws Exception {
        HttpsUrlValidator.requireSafe(url, "url");
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", JSON_CONTENT_TYPE);
        connection.setRequestProperty("Accept", "application/json");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream out = connection.getOutputStream()) {
            out.write(bytes);
        }
        return read(connection);
    }

    public static byte[] getBytes(String url, int connectTimeout, int readTimeout) throws Exception {
        return getBytes(url, connectTimeout, readTimeout, MAX_RESPONSE_BYTES);
    }

    public static byte[] getBytes(String url, int connectTimeout, int readTimeout, int maxBytes) throws Exception {
        HttpsUrlValidator.requireSafe(url, "url");
        if (maxBytes <= 0) throw new IllegalArgumentException("maxBytes pozitif olmalı");
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        connection.setInstanceFollowRedirects(true);
        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            connection.disconnect();
            throw new IllegalStateException("HTTP " + code);
        }
        try (InputStream in = connection.getInputStream()) {
            return readBytes(in, maxBytes);
        } finally {
            connection.disconnect();
        }
    }

    private static Response read(HttpURLConnection connection) throws Exception {
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 400 ? connection.getInputStream() : connection.getErrorStream();
        String body = stream == null ? "" : new String(readBytes(stream, MAX_RESPONSE_BYTES), StandardCharsets.UTF_8);
        connection.disconnect();
        return new Response(code, body);
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
