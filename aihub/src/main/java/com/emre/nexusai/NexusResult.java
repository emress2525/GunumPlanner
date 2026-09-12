package com.emre.nexusai;

public final class NexusResult {
    public enum Kind { TEXT, IMAGE, VIDEO, ERROR }

    public final Kind kind;
    public final String content;

    private NexusResult(Kind kind, String content) {
        this.kind = kind;
        this.content = content == null ? "" : content;
    }

    public static NexusResult text(String text) {
        return new NexusResult(Kind.TEXT, text);
    }

    public static NexusResult image(String url) {
        return new NexusResult(Kind.IMAGE, HttpsUrlValidator.requireSafe(url, "image url"));
    }

    public static NexusResult video(String url) {
        return new NexusResult(Kind.VIDEO, HttpsUrlValidator.requireSafe(url, "video url"));
    }

    public static NexusResult error(String message) {
        return new NexusResult(Kind.ERROR, message);
    }
}
