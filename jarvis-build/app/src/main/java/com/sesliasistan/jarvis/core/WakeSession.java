package com.sesliasistan.jarvis.core;

public final class WakeSession {
    public enum DecisionType {
        IGNORE,
        ARM,
        COMMAND
    }

    public static final class Decision {
        private final DecisionType type;
        private final String command;

        private Decision(DecisionType type, String command) {
            this.type = type;
            this.command = command == null ? "" : command;
        }

        public DecisionType type() {
            return type;
        }

        public String command() {
            return command;
        }
    }

    private final long armedWindowMs;
    private long armedUntilMs;

    public WakeSession(long armedWindowMs) {
        if (armedWindowMs <= 0) {
            throw new IllegalArgumentException("armedWindowMs must be positive");
        }
        this.armedWindowMs = armedWindowMs;
    }

    public Decision onUtterance(String utterance, long nowMs) {
        CommandRouter.WakeResult wakeResult = CommandRouter.extractAfterWakeWord(utterance);
        if (wakeResult.wakeDetected()) {
            if (wakeResult.command().isEmpty()) {
                armedUntilMs = nowMs + armedWindowMs;
                return new Decision(DecisionType.ARM, "");
            }
            armedUntilMs = 0L;
            return new Decision(DecisionType.COMMAND, wakeResult.command());
        }

        if (isArmed(nowMs)) {
            armedUntilMs = 0L;
            String normalized = CommandRouter.normalize(utterance);
            if (normalized.isEmpty()) {
                return new Decision(DecisionType.IGNORE, "");
            }
            return new Decision(DecisionType.COMMAND, normalized);
        }

        armedUntilMs = 0L;
        return new Decision(DecisionType.IGNORE, "");
    }

    public boolean isArmed(long nowMs) {
        return armedUntilMs > 0L && nowMs <= armedUntilMs;
    }

    public void reset() {
        armedUntilMs = 0L;
    }
}
