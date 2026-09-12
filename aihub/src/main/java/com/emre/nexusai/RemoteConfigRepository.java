package com.emre.nexusai;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class RemoteConfigRepository {
    public static final String REMOTE_CONFIG_URL = "https://raw.githubusercontent.com/emress2525/GunumPlanner/ai-nexus-apk/nexus-runtime/nexus-config.json";
    public static final String DEFAULT_ASSET_NAME = "nexus-config.json";
    public static final String CACHE_FILE_NAME = "nexus-config-cache.json";
    public static final int TIMEOUT_MS = 8_000;

    private final Context appContext;
    private volatile NexusConfig current;

    public RemoteConfigRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.current = loadBestAvailable();
    }

    public NexusConfig getCurrent() {
        return current;
    }

    public void refreshAsync() {
        new Thread(() -> {
            try {
                HttpUtil.Response response = HttpUtil.get(REMOTE_CONFIG_URL, TIMEOUT_MS, TIMEOUT_MS);
                if (!response.isSuccessful()) return;
                NexusConfig parsed = ConfigParser.parse(response.body);
                writeCache(response.body);
                current = parsed;
            } catch (Exception ignored) {
                // Bundled/cached config remains active.
            }
        }, "nexus-config-refresh").start();
    }

    private NexusConfig loadBestAvailable() {
        try {
            File cache = new File(appContext.getFilesDir(), CACHE_FILE_NAME);
            if (cache.isFile()) {
                String json = readAll(new FileInputStream(cache));
                return ConfigParser.parse(json);
            }
        } catch (Exception ignored) { }

        try {
            String json = readAll(appContext.getAssets().open(DEFAULT_ASSET_NAME));
            return ConfigParser.parse(json);
        } catch (Exception ex) {
            throw new IllegalStateException("Nexus AI varsayılan yapılandırması yüklenemedi", ex);
        }
    }

    private void writeCache(String json) throws Exception {
        File target = new File(appContext.getFilesDir(), CACHE_FILE_NAME);
        try (FileOutputStream out = new FileOutputStream(target, false)) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String readAll(InputStream input) throws Exception {
        try (InputStream in = input) {
            byte[] buffer = new byte[8192];
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
