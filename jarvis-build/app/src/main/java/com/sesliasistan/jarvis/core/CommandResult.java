package com.sesliasistan.jarvis.core;

import java.util.Objects;

public final class CommandResult {
    public enum Type {
        TIME,
        DATE,
        OPEN_APP,
        WEB_SEARCH,
        SET_ALARM,
        SET_TIMER,
        OPEN_SETTINGS,
        FLASHLIGHT_ON,
        FLASHLIGHT_OFF,
        VOLUME_UP,
        VOLUME_DOWN,
        VOLUME_MUTE,
        VOLUME_MAX,
        BATTERY_STATUS,
        OPEN_CAMERA,
        NAVIGATE_TO,
        DIAL_NUMBER,
        COMPOSE_SMS,
        MEDIA_PLAY_PAUSE,
        MEDIA_NEXT,
        MEDIA_PREVIOUS,
        CREATE_NOTE,
        READ_LAST_NOTE,
        HELP
    }

    private final Type type;
    private final String text;
    private final String secondaryText;
    private final int hour;
    private final int minute;
    private final int value;

    private CommandResult(Type type, String text, String secondaryText, int hour, int minute, int value) {
        this.type = Objects.requireNonNull(type, "type");
        this.text = text == null ? "" : text;
        this.secondaryText = secondaryText == null ? "" : secondaryText;
        this.hour = hour;
        this.minute = minute;
        this.value = value;
    }

    public static CommandResult simple(Type type) {
        return new CommandResult(type, "", "", -1, -1, -1);
    }

    public static CommandResult text(Type type, String text) {
        return new CommandResult(type, text, "", -1, -1, -1);
    }

    public static CommandResult pair(Type type, String text, String secondaryText) {
        return new CommandResult(type, text, secondaryText, -1, -1, -1);
    }

    public static CommandResult number(Type type, int value) {
        return new CommandResult(type, "", "", -1, -1, value);
    }

    public static CommandResult alarm(int hour, int minute) {
        return new CommandResult(Type.SET_ALARM, "", "", hour, minute, -1);
    }

    public Type type() { return type; }
    public String text() { return text; }
    public String secondaryText() { return secondaryText; }
    public int hour() { return hour; }
    public int minute() { return minute; }
    public int value() { return value; }

    @Override
    public String toString() {
        return "CommandResult{" +
                "type=" + type +
                ", text='" + text + '\'' +
                ", secondaryText='" + secondaryText + '\'' +
                ", hour=" + hour +
                ", minute=" + minute +
                ", value=" + value +
                '}';
    }
}
