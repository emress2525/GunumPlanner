package com.emre.nexusai;

import java.util.ArrayList;
import java.util.List;

public final class TextGateway {
    public static final int CONNECT_TIMEOUT_MS = 10_000;
    public static final int READ_TIMEOUT_MS = 40_000;
    public static final String ALL_ENDPOINTS_FAILED_MESSAGE = "AI bağlantısı kurulamadı. İnternet bağlantını kontrol edip tekrar dene.";

    public NexusResult complete(NexusConfig config, RequestMode mode, String prompt) {
        if (config == null || config.textEndpoints.isEmpty()) return NexusResult.error(ALL_ENDPOINTS_FAILED_MESSAGE);
        List<String> failures = new ArrayList<>();
        for (NexusConfig.TextEndpoint endpoint : FailoverPolicy.ordered(config.textEndpoints)) {
            try {
                String payload = RequestPayload.openAi(endpoint.model, SystemPrompts.forMode(mode), prompt);
                HttpUtil.Response response = HttpUtil.postJson(endpoint.url, payload, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
                if (!response.isSuccessful()) {
                    failures.add(endpoint.name + ": HTTP " + response.code);
                    continue;
                }
                String text = TextResponseParser.parseOpenAiResponse(response.body);
                if (!text.isEmpty()) return NexusResult.text(text);
            } catch (Exception ex) {
                failures.add(endpoint.name + ": " + safeMessage(ex));
            }
        }
        return NexusResult.error(ALL_ENDPOINTS_FAILED_MESSAGE + (failures.isEmpty() ? "" : "\n" + String.join("\n", failures)));
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.trim().isEmpty() ? ex.getClass().getSimpleName() : message;
    }
}
