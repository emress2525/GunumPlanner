package com.emre.nexusai;

public final class ChatHistoryEntry {
    public enum Kind { USER, TEXT, ERROR, IMAGE, VIDEO }

    public final Kind kind;
    public final String label;
    public final String content;
    public final String mode;

    public ChatHistoryEntry(Kind kind, String label, String content, String mode) {
        this.kind = kind == null ? Kind.TEXT : kind;
        this.label = label == null ? "" : label;
        this.content = content == null ? "" : content;
        this.mode = mode == null ? "" : mode;
    }
}
