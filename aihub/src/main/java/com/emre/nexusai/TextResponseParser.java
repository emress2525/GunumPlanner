package com.emre.nexusai;

import org.json.JSONArray;
import org.json.JSONObject;

public final class TextResponseParser {
    private TextResponseParser() { }

    public static String parseOpenAiResponse(String jsonText) {
        try {
            JSONObject root = new JSONObject(jsonText == null ? "" : jsonText);
            JSONArray choices = root.optJSONArray("choices");
            if (choices == null || choices.length() == 0) throw new IllegalArgumentException("choices boş");
            JSONObject message = choices.getJSONObject(0).optJSONObject("message");
            if (message == null) throw new IllegalArgumentException("message yok");
            String content = message.optString("content", "").trim();
            if (content.isEmpty()) throw new IllegalArgumentException("content boş");
            return content;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("AI cevabı okunamadı", ex);
        }
    }
}
