package com.emre.nexusai;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class NexusConfig {
    public int version = 1;
    public final List<TextEndpoint> textEndpoints = new ArrayList<>();
    public String imageEndpoint;
    public String videoEndpoint;

    public static final class TextEndpoint {
        public final String name;
        public final String url;
        public final String model;
        public final boolean requiresAuthorization;

        public TextEndpoint(String name, String url, String model) {
            this(name, url, model, false);
        }

        public TextEndpoint(String name, String url, String model, boolean requiresAuthorization) {
            this.name = requireText(name, "name");
            this.url = HttpsUrlValidator.requireSafe(url, "url");
            this.model = requireText(model, "model");
            this.requiresAuthorization = requiresAuthorization;
        }

        private static String requireText(String value, String field) {
            if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(field + " boş olamaz");
            return value.trim();
        }

        @Override public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof TextEndpoint)) return false;
            TextEndpoint other = (TextEndpoint) obj;
            return requiresAuthorization == other.requiresAuthorization
                    && name.equals(other.name)
                    && url.equals(other.url)
                    && model.equals(other.model);
        }

        @Override public int hashCode() {
            return Objects.hash(name, url, model, requiresAuthorization);
        }
    }
}
