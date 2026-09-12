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
            "(?<!\\d)([01]?\\d|2[0-3])[:.]([0-5]\\d)(?!\\d)");
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

        CommandResult history = parseHistory(command);
        if (history != null) return history;
        CommandResult routine = parseRoutine(command);
        if (routine != null) return routine;
        CommandResult reminder = parseReminder(command);
        if (reminder != null) return reminder;
        CommandResult calendar = parseCalendar(command);
        if (calendar != null) return calendar;

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
        CommandResult contactSms = parseContactSms(command);
        if (contactSms != null) return contactSms;
        CommandResult dial = parseDial(command);
        if (dial != null) return dial;
        CommandResult contactDial = parseContactDial(command);
        if (contactDial != null) return contactDial;
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

    private static CommandResult parseHistory(String command) {
        if (containsAny(command, "geçmişi temizle", "geçmişimi temizle", "komut geçmişini temizle")) {
            return CommandResult.simple(CommandResult.Type.CLEAR_HISTORY);
        }
        if (containsAny(command, "geçmişi oku", "geçmişimi oku", "komut geçmişini oku", "son komutları söyle")) {
            return CommandResult.simple(CommandResult.Type.READ_HISTORY);
        }
        return null;
    }

    private static CommandResult parseRoutine(String command) {
        if (containsAny(command, "rutinleri söyle", "rutinlerimi söyle", "rutinleri listele", "rutinlerimi listele")) {
            return CommandResult.simple(CommandResult.Type.LIST_ROUTINES);
        }
        String savePrefix = "rutin kaydet ";
        if (command.startsWith(savePrefix)) {
            String rest = command.substring(savePrefix.length()).trim();
            int colon = rest.indexOf(':');
            if (colon <= 0 || colon >= rest.length() - 1) return CommandResult.simple(CommandResult.Type.HELP);
            String name = rest.substring(0, colon).trim();
            String body = rest.substring(colon + 1).trim();
            return name.isEmpty() || body.isEmpty()
                    ? CommandResult.simple(CommandResult.Type.HELP)
                    : CommandResult.pair(CommandResult.Type.SAVE_ROUTINE, name, body);
        }
        String[] runSuffixes = {" rutinini çalıştır", " rutinini calistir", " rutini çalıştır", " rutini calistir"};
        for (String suffix : runSuffixes) {
            if (command.endsWith(suffix)) {
                String name = command.substring(0, command.length() - suffix.length()).trim();
                return name.isEmpty() ? CommandResult.simple(CommandResult.Type.HELP)
                        : CommandResult.text(CommandResult.Type.RUN_ROUTINE, name);
            }
        }
        String[] deleteSuffixes = {" rutinini sil", " rutini sil"};
        for (String suffix : deleteSuffixes) {
            if (command.endsWith(suffix)) {
                String name = command.substring(0, command.length() - suffix.length()).trim();
                return name.isEmpty() ? CommandResult.simple(CommandResult.Type.HELP)
                        : CommandResult.text(CommandResult.Type.DELETE_ROUTINE, name);
            }
        }
        return null;
    }

    private static CommandResult parseReminder(String command) {
        if (!command.contains("hatırlat")) return null;

        int sonra = command.indexOf(" sonra ");
        if (sonra > 0) {
            String durationText = command.substring(0, sonra).trim();
            int seconds = parseDurationSeconds(durationText);
            if (seconds > 0) {
                String reminderText = command.substring(sonra + " sonra ".length()).trim();
                reminderText = stripReminderSuffix(reminderText);
                if (!reminderText.isEmpty()) return CommandResult.relativeReminder(reminderText, seconds);
            }
        }

        int dayOffset = dayOffset(command);
        if (dayOffset >= 0) {
            Matcher time = FULL_TIME_PATTERN.matcher(command);
            if (time.find()) {
                int hour = Integer.parseInt(time.group(1));
                int minute = Integer.parseInt(time.group(2));
                String afterTime = command.substring(time.end()).trim();
                String reminderText = stripReminderSuffix(afterTime);
                if (!reminderText.isEmpty()) {
                    return CommandResult.absoluteReminder(reminderText, dayOffset, hour, minute);
                }
            }
        }
        return null;
    }

    private static String stripReminderSuffix(String value) {
        String text = value.trim();
        text = text.replaceFirst("\\s+diye\\s+hatırlat$", "");
        text = text.replaceFirst("\\s+hatırlat$", "");
        text = text.replaceFirst("^bana\\s+", "");
        return text.trim();
    }

    private static CommandResult parseCalendar(String command) {
        String suffix = null;
        if (command.endsWith(" takvime ekle")) suffix = " takvime ekle";
        else if (command.endsWith(" takvime kaydet")) suffix = " takvime kaydet";
        if (suffix == null) return null;

        String body = command.substring(0, command.length() - suffix.length()).trim();
        int dayOffset = dayOffset(body);
        if (dayOffset < 0) dayOffset = 0;
        body = body.replaceFirst("^(bugün|yarın)\\s+", "").trim();

        int hour = -1;
        int minute = -1;
        Matcher time = FULL_TIME_PATTERN.matcher(body);
        if (time.find()) {
            hour = Integer.parseInt(time.group(1));
            minute = Integer.parseInt(time.group(2));
            body = (body.substring(0, time.start()) + " " + body.substring(time.end())).trim();
            body = body.replaceFirst("^saat\\s+", "").trim();
        }
        return body.isEmpty() ? CommandResult.simple(CommandResult.Type.HELP)
                : CommandResult.calendarEvent(body, dayOffset, hour, minute);
    }

    private static int dayOffset(String command) {
        if (command.startsWith("yarın ") || command.equals("yarın")) return 1;
        if (command.startsWith("bugün ") || command.equals("bugün")) return 0;
        return -1;
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
        int totalSeconds = parseDurationSeconds(command);
        if (totalSeconds <= 0) return CommandResult.simple(CommandResult.Type.HELP);
        return CommandResult.number(CommandResult.Type.SET_TIMER, totalSeconds);
    }

    private static int parseDurationSeconds(String text) {
        long totalSeconds = 0L;
        Matcher hours = HOURS_DURATION_PATTERN.matcher(text);
        if (hours.find()) totalSeconds += Long.parseLong(hours.group(1)) * 3600L;
        Matcher minutes = MINUTES_DURATION_PATTERN.matcher(text);
        if (minutes.find()) totalSeconds += Long.parseLong(minutes.group(1)) * 60L;
        Matcher seconds = SECONDS_DURATION_PATTERN.matcher(text);
        if (seconds.find()) totalSeconds += Long.parseLong(seconds.group(1));
        return totalSeconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalSeconds;
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

    private static CommandResult parseContactSms(String command) {
        if (!(command.contains("mesaj") || command.contains("sms")) || PHONE_PATTERN.matcher(command).find()) return null;
        String[] endings = {" diye mesaj yaz", " diye mesaj gönder", " diye sms yaz", " diye sms gönder"};
        for (String ending : endings) {
            if (!command.endsWith(ending)) continue;
            String prefix = command.substring(0, command.length() - ending.length()).trim();
            int firstSpace = prefix.indexOf(' ');
            if (firstSpace <= 0 || firstSpace >= prefix.length() - 1) return null;
            String contact = cleanupDative(prefix.substring(0, firstSpace).trim());
            String body = prefix.substring(firstSpace + 1).trim();
            if (contact.isEmpty() || body.isEmpty()) return null;
            return CommandResult.pair(CommandResult.Type.COMPOSE_SMS_CONTACT, contact, body);
        }
        return null;
    }

    private static CommandResult parseDial(String command) {
        if (!(containsAny(command, " ara", "ara ", "telefon et", "çevir", "cevir") || command.endsWith("ara"))) return null;
        Matcher phone = PHONE_PATTERN.matcher(command);
        if (!phone.find()) return null;
        return CommandResult.text(CommandResult.Type.DIAL_NUMBER, sanitizePhone(phone.group(1)));
    }

    private static CommandResult parseContactDial(String command) {
        if (PHONE_PATTERN.matcher(command).find()) return null;
        if (command.startsWith("internette ") || command.startsWith("webde ") || command.startsWith("google")) return null;
        String target = null;
        if (command.endsWith(" ara")) target = command.substring(0, command.length() - " ara".length()).trim();
        else if (command.endsWith(" telefon et")) target = command.substring(0, command.length() - " telefon et".length()).trim();
        if (target == null || target.isEmpty()) return null;
        target = cleanupAccusative(target);
        if (target.isEmpty() || target.length() > 80) return null;
        return CommandResult.text(CommandResult.Type.DIAL_CONTACT, target);
    }

    private static String cleanupAccusative(String value) {
        String target = value.replaceFirst("['’](?:i|ı|u|ü|yi|yı|yu|yü)$", "");
        if (!target.equals(value)) return target.trim();
        if (target.matches(".*(?:yi|yı|yu|yü)$") && target.length() > 3) {
            return target.substring(0, target.length() - 2).trim();
        }
        if (target.length() > 4 && target.matches(".*[iıuü]$")) {
            return target.substring(0, target.length() - 1).trim();
        }
        return target.trim();
    }

    private static String cleanupDative(String value) {
        String target = value.replaceFirst("['’](?:e|a|ye|ya)$", "");
        if (!target.equals(value)) return target.trim();
        if (target.matches(".*(?:ye|ya)$") && target.length() > 3) {
            return target.substring(0, target.length() - 2).trim();
        }
        if (target.length() > 4 && target.matches(".*[ae]$")) {
            return target.substring(0, target.length() - 1).trim();
        }
        return target.trim();
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
