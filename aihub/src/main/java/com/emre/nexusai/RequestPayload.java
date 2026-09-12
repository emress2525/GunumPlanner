package com.emre.nexusai;

import org.json.JSONArray;
import org.json.JSONObject;

public final class RequestPayload {
    private RequestPayload() { }

    public static String openAi(String model, String system, String user) {
        try {
            JSONObject root = new JSONObject();
            root.put("model", model);
            root.put("stream", false);
            root.put("temperature", 0.7);
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", system));
            messages.put(new JSONObject().put("role", "user").put("content", user));
            root.put("messages", messages);
            return root.toString();
        } catch (Exception ex) {
            throw new IllegalArgumentException("AI isteği hazırlanamadı", ex);
        }
    }
}
