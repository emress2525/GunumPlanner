package com.sesliasistan.jarvis.core;

import java.util.Objects;

public final class CommandResult {
    public enum Type {
        TIME,
        DATE,
        OPEN_APP,
        WEB_SEARCH,
        SET_ALARM,
        OPEN_SETTINGS,
        HELP
    }

    private final Type type;
    private final String text;
    private final int hour;
    private final int minute;

    private CommandResult(Type type, String text, int hour, int minute) {
        this.type = Objects.requireNonNull(type, "type");
        this.text = text == null ? "" : text;
        this.hour = hour;
        this.minute = minute;
    }

    public static CommandResult simple(Type type) {
        return new CommandResult(type, "", -1, -1);
    }

    public static CommandResult text(Type type, String text) {
        return new CommandResult(type, text, -1, -1);
    }

    public static CommandResult alarm(int hour, int minute) {
        return new CommandResult(Type.SET_ALARM, "", hour, minute);
    }

    public Type type() {
        return type;
    }

    public String text() {
        return text;
    }

    public int hour() {
        return hour;
    }

    public int minute() {
        return minute;
    }

    @Override
    public String toString() {
        return "CommandResult{" +
                "type=" + type +
                ", text='" + text + '\'' +
                ", hour=" + hour +
                ", minute=" + minute +
                '}';
    }
}
