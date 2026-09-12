package com.emre.nexusai;

import android.content.Context;

import java.util.List;

public final class NexusAiEngine {
    public static final int MAX_PROMPT_CHARS = 20_000;
    public static final int MAX_HISTORY_MESSAGES = 20;

    public interface Callback {
        void onResult(NexusResult result);
    }

    private final RemoteConfigRepository configRepository;
    private final TextGateway textGateway = new TextGateway();
    private final MediaGateway mediaGateway = new MediaGateway();
    private final WebResearchGateway researchGateway = new WebResearchGateway();

    public NexusAiEngine(Context context) {
        configRepository = new RemoteConfigRepository(context);
        configRepository.refreshAsync();
    }

    public void execute(String prompt, RequestMode requestedMode, Callback callback) {
        final String clean = normalizePrompt(prompt);
        if (callback == null) return;
        if (clean.isEmpty()) {
            callback.onResult(NexusResult.error("Önce ne yapmak istediğini yaz."));
            return;
        }
        if (clean.length() > MAX_PROMPT_CHARS) {
            callback.onResult(NexusResult.error("İstek çok uzun. En fazla " + MAX_PROMPT_CHARS + " karakter kullan."));
            return;
        }

        final RequestMode mode = requestedMode == null ? RequestClassifier.classify(clean) : requestedMode;
        new Thread(() -> {
            NexusResult result;
            NexusConfig config = configRepository.getCurrent();
            switch (mode) {
                case IMAGE:
                    result = mediaGateway.generateImage(config, clean);
                    break;
                case VIDEO:
                    result = mediaGateway.generateVideo(config, clean);
                    break;
                case AUDIO:
                    result = NexusResult.text(clean);
                    break;
                case RESEARCH:
                    List<WebSearchResult> webResults = researchGateway.search(clean);
                    String groundedPrompt = ResearchPromptBuilder.build(clean, webResults);
                    result = textGateway.completeResearch(config, clean, groundedPrompt);
                    result = attachSources(result, webResults);
                    break;
                case CODE:
                case CHAT:
                default:
                    result = textGateway.complete(config, mode, clean);
                    break;
            }
            callback.onResult(result);
        }, "nexus-ai-request").start();
    }

    static NexusResult attachSources(NexusResult result, List<WebSearchResult> sources) {
        if (result == null || result.kind != NexusResult.Kind.TEXT || sources == null || sources.isEmpty()) return result;
        StringBuilder text = new StringBuilder(result.content == null ? "" : result.content.trim());
        text.append("\n\nKaynaklar:\n");
        int i = 1;
        for (WebSearchResult source : sources) {
            if (source == null || source.url.isEmpty()) continue;
            text.append(i++).append(". ").append(source.title.isEmpty() ? source.url : source.title)
                    .append(" — ").append(source.url).append('\n');
        }
        return NexusResult.text(text.toString().trim());
    }

    public static String normalizePrompt(String prompt) {
        return prompt == null ? "" : prompt.trim();
    }

    public static boolean requiresCloudGeneration(RequestMode mode) {
        return mode != RequestMode.AUDIO;
    }
}
