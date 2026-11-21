package com.tevkil.radar;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypts the user's own Meta API token with the non-exportable Android Keystore. */
public final class InstagramSecureConfig {
    private static final String ALIAS = "tevkil_radar_meta_api_key_v1";
    private static final String KEY = "meta_token_encrypted";
    private InstagramSecureConfig() {}

    private static SecretKey aesKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build());
        return generator.generateKey();
    }

    public static void saveToken(Context context, String token) throws Exception {
        if (token == null || token.trim().isEmpty()) throw new IllegalArgumentException("Token boş olamaz");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey());
        byte[] encrypted = cipher.doFinal(token.trim().getBytes(StandardCharsets.UTF_8));
        String stored = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + ":" +
                Base64.encodeToString(encrypted, Base64.NO_WRAP);
        Prefs.raw(context).edit().putString(KEY, stored).apply();
    }

    public static String getToken(Context context) {
        String stored = Prefs.raw(context).getString(KEY, "");
        if (stored.isEmpty()) return "";
        try {
            String[] parts = stored.split(":", 2);
            if (parts.length != 2) return "";
            byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] value = Base64.decode(parts[1], Base64.NO_WRAP);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(value), StandardCharsets.UTF_8);
        } catch (Exception e) { return ""; }
    }

    public static boolean configured(Context context) {
        return !getToken(context).isEmpty() &&
                Prefs.raw(context).getString("meta_ig_id", "").matches("[0-9]{5,30}");
    }

    public static void clear(Context context) {
        Prefs.raw(context).edit().remove(KEY).remove("meta_ig_id").putBoolean("meta_enabled", false).apply();
        InstagramScanJob.schedule(context);
    }
}
