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
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?<!\\d)(\\+?\\d[\\d\\s()\\-]{5,}\\d)(?!\\d)");
    private static final Pattern HOURS_DURATION_PATTERN = Pattern.compile("(\\d+)\\s*(?:saat|sa)(?:\\s|$)");
    private static final Pattern MINUTES_DURATION_PATTERN = Pattern.compile("(\\d+)\\s*(?:dakika|dk)(?:\\s|$)");
    private static final Pattern SECONDS_DURATION_PATTERN = Pattern.compile("(\\d+)\\s*(?:saniye|sn)(?:\\s|$)");

    private CommandRouter() { }

    public static final class WakeResult {
        private final boolean wakeDetected;
        private final String command;

        private WakeResult(boolean wakeDetected, String command) {
            this.wakeDetected = wakeDetected;
            this.command = command;
        }

        public boolean wakeDetected() { return wakeDetected; }
        public String command() { return command; }
    }

    public static WakeResult extractAfterWakeWord(String rawText) {
        String normalized = normalize(rawText);
        Matcher matcher = WAKE_PATTERN.matcher(normalized);
        if (!matcher.find()) return new WakeResult(false, "");
        String command = normalized.substring(matcher.end())
                .replaceFirst("^[\\s,.:;!?-]+", "")
                .trim();
        return new WakeResult(true, command);
    }

    public static CommandResult route(String rawCommand) {
        String command = normalize(rawCommand);
        if (command.isEmpty()) return CommandResult.simple(CommandResult.Type.HELP);

        if (isTimeCommand(command)) return CommandResult.simple(CommandResult.Type.TIME);
        if (isDateCommand(command)) return CommandResult.simple(CommandResult.Type.DATE);
        if (isBatteryCommand(command)) return CommandResult.simple(CommandResult.Type.BATTERY_STATUS);

        CommandResult timer = parseTimer(command);
        if (timer != null) return timer;
        if (isAlarmCommand(command)) return parseAlarm(command);

        CommandResult flashlight = parseFlashlight(command);
        if (flashlight != null) return flashlight;
        CommandResult volume = parseVolume(command);
        if (volume != null) return volume;
        CommandResult media = parseMedia(command);
        if (media != null) return media;

        if (isCameraCommand(command)) return CommandResult.simple(CommandResult.Type.OPEN_CAMERA);

        CommandResult sms = parseSms(command);
        if (sms != null) return sms;
        CommandResult dial = parseDial(command);
        if (dial != null) return dial;
        CommandResult navigation = parseNavigation(command);
        if (navigation != null) return navigation;
        CommandResult note = parseNote(command);
        if (note != null) return note;

        CommandResult settings = parseSettings(command);
        if (settings != null) return settings;
        if (isHelpCommand(command)) return CommandResult.simple(CommandResult.Type.HELP);

        CommandResult search = parseExplicitSearch(command);
        if (search != null) return search;
        CommandResult openApp = parseOpenApp(command);
        if (openApp != null) return openApp;

        return CommandResult.text(CommandResult.Type.WEB_SEARCH, command);
    }

    static String normalize(String text) {
        if (text == null) return "";
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

    private static boolean isBatteryCommand(String command) {
        boolean batteryWord = command.contains("pil") || command.contains("batarya");
        return batteryWord && (command.contains("kaç") || command.contains("yüzde") || command.contains("durum"));
    }

    private static boolean isAlarmCommand(String command) {
        return command.contains("alarm")
                && (command.contains("kur") || command.contains("ayarla") || command.contains("oluştur"));
    }

    private static CommandResult parseAlarm(String command) {
        Matcher full = FULL_TIME_PATTERN.matcher(command);
        if (full.find()) {
            return CommandResult.alarm(Integer.parseInt(full.group(1)), Integer.parseInt(full.group(2)));
        }
        Matcher hourOnly = HOUR_PATTERN.matcher(command);
        if (hourOnly.find()) return CommandResult.alarm(Integer.parseInt(hourOnly.group(1)), 0);
        return CommandResult.text(CommandResult.Type.WEB_SEARCH, command);
    }

    private static CommandResult parseTimer(String command) {
        if (!(command.contains("zamanlayıcı") || command.contains("timer"))) return null;
        int totalSeconds = 0;
        Matcher hours = HOURS_DURATION_PATTERN.matcher(command);
        if (hours.find()) totalSeconds += Integer.parseInt(hours.group(1)) * 3600;
        Matcher minutes = MINUTES_DURATION_PATTERN.matcher(command);
        if (minutes.find()) totalSeconds += Integer.parseInt(minutes.group(1)) * 60;
        Matcher seconds = SECONDS_DURATION_PATTERN.matcher(command);
        if (seconds.find()) totalSeconds += Integer.parseInt(seconds.group(1));
        if (totalSeconds <= 0) return CommandResult.simple(CommandResult.Type.HELP);
        return CommandResult.number(CommandResult.Type.SET_TIMER, totalSeconds);
    }

    private static CommandResult parseFlashlight(String command) {
        if (!(command.contains("fener") || command.contains("flash"))) return null;
        if (containsAny(command, "kapat", "söndür", "sondur")) return CommandResult.simple(CommandResult.Type.FLASHLIGHT_OFF);
        if (containsAny(command, "aç", "yak")) return CommandResult.simple(CommandResult.Type.FLASHLIGHT_ON);
        return null;
    }

    private static CommandResult parseVolume(String command) {
        if (!(command.contains("ses") || command.contains("volume"))) return null;
        if (containsAny(command, "fulle", "maksimum", "sonuna kadar")) return CommandResult.simple(CommandResult.Type.VOLUME_MAX);
        if (containsAny(command, "sessize", "sesi kapat", "mute")) return CommandResult.simple(CommandResult.Type.VOLUME_MUTE);
        if (containsAny(command, "azalt", "kıs", "düşür", "dusur")) return CommandResult.simple(CommandResult.Type.VOLUME_DOWN);
        if (containsAny(command, "yükselt", "artır", "arttir", "aç")) return CommandResult.simple(CommandResult.Type.VOLUME_UP);
        return null;
    }

    private static CommandResult parseMedia(String command) {
        if (containsAny(command, "sonraki şarkı", "sonraki parça", "şarkıyı değiştir")) {
            return CommandResult.simple(CommandResult.Type.MEDIA_NEXT);
        }
        if (containsAny(command, "önceki şarkı", "önceki parça", "geri şarkı")) {
            return CommandResult.simple(CommandResult.Type.MEDIA_PREVIOUS);
        }
        boolean mediaWord = command.contains("müzik") || command.contains("müziği") || command.contains("şarkı");
        if (mediaWord && containsAny(command, "durdur", "duraklat", "devam", "oynat", "başlat")) {
            return CommandResult.simple(CommandResult.Type.MEDIA_PLAY_PAUSE);
        }
        return null;
    }

    private static boolean isCameraCommand(String command) {
        return command.contains("kamera") && containsAny(command, "aç", "başlat");
    }

    private static CommandResult parseSms(String command) {
        if (!(command.contains("mesaj") || command.contains("sms"))) return null;
        Matcher phone = PHONE_PATTERN.matcher(command);
        if (!phone.find()) return null;
        String number = sanitizePhone(phone.group(1));
        String rest = (command.substring(0, phone.start()) + " " + command.substring(phone.end())).trim();
        rest = rest.replaceFirst("^numarasına\\s+", "");
        rest = rest.replaceFirst("^numarasina\\s+", "");
        rest = rest.replaceFirst("^(?:'?[ae]|'?(?:ya|ye))\\s+", "");
        rest = rest.replaceFirst("\\s+diye\\s+(?:mesaj|sms)\\s+(?:yaz|gönder)$", "");
        rest = rest.replaceFirst("\\s+(?:mesaj|sms)\\s+(?:yaz|gönder)$", "");
        rest = rest.replaceFirst("^(?:mesaj|sms)\\s+(?:yaz|gönder)\\s*", "");
        rest = rest.trim();
        return CommandResult.pair(CommandResult.Type.COMPOSE_SMS, number, rest);
    }

    private static CommandResult parseDial(String command) {
        if (!(containsAny(command, " ara", "ara ", "telefon et", "çevir", "cevir") || command.endsWith("ara"))) return null;
        Matcher phone = PHONE_PATTERN.matcher(command);
        if (!phone.find()) return null;
        return CommandResult.text(CommandResult.Type.DIAL_NUMBER, sanitizePhone(phone.group(1)));
    }

    private static CommandResult parseNavigation(String command) {
        String target = null;
        String[] suffixes = {" yol tarifi aç", " yol tarifi", " navigasyon başlat", " navigasyonu başlat"};
        for (String suffix : suffixes) {
            if (command.endsWith(suffix)) {
                target = command.substring(0, command.length() - suffix.length()).trim();
                break;
            }
        }
        if (target == null && command.startsWith("yol tarifi ")) target = command.substring("yol tarifi ".length()).trim();
        if (target == null || target.isEmpty()) return null;
        target = target.replaceFirst("['’](?:a|e|ya|ye)$", "").trim();
        return target.isEmpty() ? null : CommandResult.text(CommandResult.Type.NAVIGATE_TO, target);
    }

    private static CommandResult parseNote(String command) {
        if (containsAny(command, "son notumu oku", "son notu oku", "son not ne", "son notum ne")) {
            return CommandResult.simple(CommandResult.Type.READ_LAST_NOTE);
        }
        String[] prefixes = {"not al ", "not et ", "not yaz ", "şunu not al ", "bunu not al "};
        for (String prefix : prefixes) {
            if (command.startsWith(prefix)) {
                String note = command.substring(prefix.length()).trim();
                return note.isEmpty() ? CommandResult.simple(CommandResult.Type.HELP)
                        : CommandResult.text(CommandResult.Type.CREATE_NOTE, note);
            }
        }
        return null;
    }

    private static CommandResult parseSettings(String command) {
        if (!(command.contains("ayar") && command.contains("aç"))) return null;
        if (command.contains("bluetooth")) return CommandResult.text(CommandResult.Type.OPEN_SETTINGS, "bluetooth");
        if (command.contains("wifi") || command.contains("wi-fi") || command.contains("kablosuz")) {
            return CommandResult.text(CommandResult.Type.OPEN_SETTINGS, "wifi");
        }
        if (command.contains("konum") || command.contains("gps")) return CommandResult.text(CommandResult.Type.OPEN_SETTINGS, "location");
        if (command.contains("ekran") || command.contains("parlaklık")) return CommandResult.text(CommandResult.Type.OPEN_SETTINGS, "display");
        if (command.contains("ses")) return CommandResult.text(CommandResult.Type.OPEN_SETTINGS, "sound");
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
        if (query == null) return null;
        query = query.trim();
        return query.isEmpty() ? CommandResult.simple(CommandResult.Type.HELP)
                : CommandResult.text(CommandResult.Type.WEB_SEARCH, query);
    }

    private static CommandResult parseOpenApp(String command) {
        String target = null;
        if (command.startsWith("aç ")) {
            target = command.substring(3);
        } else if (command.endsWith(" uygulamasını aç")) {
            target = command.substring(0, command.length() - " uygulamasını aç".length());
        } else if (command.endsWith(" uygulamayı aç")) {
            target = command.substring(0, command.length() - " uygulamayı aç".length());
        } else if (command.endsWith(" aç")) {
            target = command.substring(0, command.length() - 3);
        }
        if (target == null) return null;
        target = target
                .replace("uygulamasını", "")
                .replace("uygulamayı", "")
                .replaceAll("['’][a-zçğıöşü]+$", "")
                .trim();
        return target.isEmpty() ? CommandResult.simple(CommandResult.Type.HELP)
                : CommandResult.text(CommandResult.Type.OPEN_APP, target);
    }

    private static String sanitizePhone(String value) {
        String trimmed = value == null ? "" : value.trim();
        boolean plus = trimmed.startsWith("+");
        String digits = trimmed.replaceAll("\\D+", "");
        return plus ? "+" + digits : digits;
    }

    private static boolean containsAny(String text, String... values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }
}
