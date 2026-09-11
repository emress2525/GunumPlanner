package com.sesliasistan.jarvis.core;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandRouter {
    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");
    private static final Pattern WAKE_PATTERN = Pattern.compile(
            "(^|\\s)(jarvis|cervis|carvis|jarviz)(?=\\s|[,.:;!?]|$)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern FULL_TIME_PATTERN = Pattern.compile(
            "(?<!\\d)([01]?\\d|2[0-3])[:.\\s]([0-5]\\d)(?!\\d)");
    private static final Pattern HOUR_PATTERN = Pattern.compile(
            "(?<!\\d)([01]?\\d|2[0-3])(?!\\d)");

    private CommandRouter() {
    }

    public static final class WakeResult {
        private final boolean wakeDetected;
        private final String command;

        private WakeResult(boolean wakeDetected, String command) {
            this.wakeDetected = wakeDetected;
            this.command = command;
        }

        public boolean wakeDetected() {
            return wakeDetected;
        }

        public String command() {
            return command;
        }
    }

    public static WakeResult extractAfterWakeWord(String rawText) {
        String normalized = normalize(rawText);
        Matcher matcher = WAKE_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return new WakeResult(false, "");
        }
        String command = normalized.substring(matcher.end())
                .replaceFirst("^[\\s,.:;!?-]+", "")
                .trim();
        return new WakeResult(true, command);
    }

    public static CommandResult route(String rawCommand) {
        String command = normalize(rawCommand);
        if (command.isEmpty()) {
            return CommandResult.simple(CommandResult.Type.HELP);
        }

        if (isTimeCommand(command)) {
            return CommandResult.simple(CommandResult.Type.TIME);
        }
        if (isDateCommand(command)) {
            return CommandResult.simple(CommandResult.Type.DATE);
        }
        if (isAlarmCommand(command)) {
            return parseAlarm(command);
        }
        CommandResult settings = parseSettings(command);
        if (settings != null) {
            return settings;
        }
        if (isHelpCommand(command)) {
            return CommandResult.simple(CommandResult.Type.HELP);
        }
        CommandResult search = parseExplicitSearch(command);
        if (search != null) {
            return search;
        }
        CommandResult openApp = parseOpenApp(command);
        if (openApp != null) {
            return openApp;
        }

        return CommandResult.text(CommandResult.Type.WEB_SEARCH, command);
    }

    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace('’', '\'')
                .replace('`', '\'')
                .toLowerCase(TURKISH)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static boolean isTimeCommand(String command) {
        return command.contains("saat kaç")
                || command.contains("saati söyle")
                || command.equals("saat")
                || command.equals("saat ne");
    }

    private static boolean isDateCommand(String command) {
        return command.contains("tarih")
                || command.contains("ayın kaçı")
                || command.contains("hangi gün")
                || command.contains("bugün günlerden ne");
    }

    private static boolean isAlarmCommand(String command) {
        return command.contains("alarm")
                && (command.contains("kur") || command.contains("ayarla"));
    }

    private static CommandResult parseAlarm(String command) {
        Matcher full = FULL_TIME_PATTERN.matcher(command);
        if (full.find()) {
            return CommandResult.alarm(
                    Integer.parseInt(full.group(1)),
                    Integer.parseInt(full.group(2)));
        }

        Matcher hourOnly = HOUR_PATTERN.matcher(command);
        if (hourOnly.find()) {
            return CommandResult.alarm(Integer.parseInt(hourOnly.group(1)), 0);
        }

        return CommandResult.text(CommandResult.Type.WEB_SEARCH, command);
    }

    private static CommandResult parseSettings(String command) {
        if (!(command.contains("ayar") && command.contains("aç"))) {
            return null;
        }
        if (command.contains("bluetooth")) {
            return CommandResult.text(CommandResult.Type.OPEN_SETTINGS, "bluetooth");
        }
        if (command.contains("wifi") || command.contains("wi-fi") || command.contains("kablosuz")) {
            return CommandResult.text(CommandResult.Type.OPEN_SETTINGS, "wifi");
        }
        return CommandResult.text(CommandResult.Type.OPEN_SETTINGS, "general");
    }

    private static boolean isHelpCommand(String command) {
        return command.equals("yardım")
                || command.contains("neler yapabilirsin")
                || command.contains("ne yapabilirsin")
                || command.contains("komutları söyle");
    }

    private static CommandResult parseExplicitSearch(String command) {
        String query = null;

        if (command.startsWith("internette ") && command.endsWith(" ara")) {
            query = command.substring("internette ".length(), command.length() - " ara".length());
        } else if (command.startsWith("webde ") && command.endsWith(" ara")) {
            query = command.substring("webde ".length(), command.length() - " ara".length());
        } else if (command.startsWith("google'da ") && command.endsWith(" ara")) {
            query = command.substring("google'da ".length(), command.length() - " ara".length());
        } else if (command.startsWith("google da ") && command.endsWith(" ara")) {
            query = command.substring("google da ".length(), command.length() - " ara".length());
        } else if (command.endsWith(" internette ara")) {
            query = command.substring(0, command.length() - " internette ara".length());
        }

        if (query == null) {
            return null;
        }
        query = query.trim();
        return query.isEmpty()
                ? CommandResult.simple(CommandResult.Type.HELP)
                : CommandResult.text(CommandResult.Type.WEB_SEARCH, query);
    }

    private static CommandResult parseOpenApp(String command) {
        String target = null;
        if (command.startsWith("aç ")) {
            target = command.substring(3);
        } else if (command.endsWith(" aç")) {
            target = command.substring(0, command.length() - 3);
        } else if (command.endsWith(" uygulamasını aç")) {
            target = command.substring(0, command.length() - " uygulamasını aç".length());
        } else if (command.endsWith(" uygulamayı aç")) {
            target = command.substring(0, command.length() - " uygulamayı aç".length());
        }

        if (target == null) {
            return null;
        }

        target = target
                .replace("uygulamasını", "")
                .replace("uygulamayı", "")
                .replaceAll("['’][a-zçğıöşü]+$", "")
                .trim();

        return target.isEmpty()
                ? CommandResult.simple(CommandResult.Type.HELP)
                : CommandResult.text(CommandResult.Type.OPEN_APP, target);
    }
}
