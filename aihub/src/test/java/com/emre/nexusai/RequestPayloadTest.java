package com.emre.nexusai;

import org.junit.Test;
import org.json.JSONObject;
import static org.junit.Assert.assertEquals;

public class RequestPayloadTest {
    @Test public void buildsOpenAiCompatiblePayload() throws Exception {
        String body = RequestPayload.openAi("model-x", "system text", "hello");
        JSONObject json = new JSONObject(body);
        assertEquals("model-x", json.getString("model"));
        assertEquals(false, json.getBoolean("stream"));
        assertEquals("system", json.getJSONArray("messages").getJSONObject(0).getString("role"));
        assertEquals("hello", json.getJSONArray("messages").getJSONObject(1).getString("content"));
    }
}
