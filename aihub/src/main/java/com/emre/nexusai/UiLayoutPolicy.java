package com.emre.nexusai;

public final class UiLayoutPolicy {
    private UiLayoutPolicy() { }

    public static int safeTopPadding(int baseDpOrPx, int statusInsetPx) {
        return baseDpOrPx + Math.max(0, statusInsetPx);
    }
}
