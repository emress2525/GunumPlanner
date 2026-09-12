package com.emre.nexusai;

import org.json.JSONArray;
import org.json.JSONObject;

public final class ConfigParser {
    private ConfigParser() { }

    public static NexusConfig parse(String jsonText) {
        try {
            JSONObject root = new JSONObject(jsonText == null ? "" : jsonText);
            NexusConfig config = new NexusConfig();
            config.version = root.optInt("version", 1);

            JSONArray endpoints = root.optJSONArray("textEndpoints");
            if (endpoints == null || endpoints.length() == 0) {
                throw new IllegalArgumentException("En az bir text endpoint gerekli");
            }

            for (int i = 0; i < endpoints.length(); i++) {
                JSONObject item = endpoints.getJSONObject(i);
                String name = required(item, "name");
                String url = required(item, "url");
                String model = required(item, "model");
                boolean auth = item.optBoolean("requiresAuthorization", false);
                if (auth) throw new IllegalArgumentException("Nexus AI kullanıcı anahtarı gerektiren endpoint kullanamaz");
                config.textEndpoints.add(new NexusConfig.TextEndpoint(name, url, model, false));
            }

            String image = nullable(root, "imageEndpoint");
            if (image != null) config.imageEndpoint = HttpsUrlValidator.requireSafe(image, "imageEndpoint");
            String video = nullable(root, "videoEndpoint");
            if (video != null) config.videoEndpoint = HttpsUrlValidator.requireSafe(video, "videoEndpoint");
            return config;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Geçersiz Nexus config: " + ex.getMessage(), ex);
        }
    }

    private static String required(JSONObject object, String key) {
        String value = object.optString(key, "").trim();
        if (value.isEmpty()) throw new IllegalArgumentException(key + " gerekli");
        return value;
    }

    private static String nullable(JSONObject object, String key) {
        if (!object.has(key) || object.isNull(key)) return null;
        String value = object.optString(key, "").trim();
        return value.isEmpty() ? null : value;
    }
}
