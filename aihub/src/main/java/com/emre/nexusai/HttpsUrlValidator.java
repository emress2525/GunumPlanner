package com.emre.nexusai;

import java.net.URI;

public final class HttpsUrlValidator {
    private HttpsUrlValidator() { }

    public static boolean isSafe(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        try {
            URI uri = URI.create(value.trim());
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && !uri.getHost().isEmpty();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public static String requireSafe(String value, String fieldName) {
        if (!isSafe(value)) throw new IllegalArgumentException(fieldName + " HTTPS olmalı");
        return value.trim();
    }
}
