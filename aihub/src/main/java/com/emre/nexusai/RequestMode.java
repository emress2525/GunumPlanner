package com.emre.nexusai;

public enum RequestMode {
    CHAT("Sohbet"),
    CODE("Kodlama"),
    RESEARCH("Araştırma"),
    IMAGE("Görsel"),
    VIDEO("Video"),
    AUDIO("Ses");

    public final String label;

    RequestMode(String label) {
        this.label = label;
    }
}
