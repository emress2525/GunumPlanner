package com.sesliasistan.jarvis.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CommandRouterTest {
    @Test public void detectsWakeWordAndInlineCommand() {
        CommandRouter.WakeResult result = CommandRouter.extractAfterWakeWord("Jarvis Spotify'ı aç");
        assertTrue(result.wakeDetected());
        assertEquals("spotify'ı aç", result.command());
    }

    @Test public void ignoresSpeechWithoutWakeWord() {
        CommandRouter.WakeResult result = CommandRouter.extractAfterWakeWord("Spotify aç");
        assertFalse(result.wakeDetected());
        assertEquals("", result.command());
    }

    @Test public void routesOpenApp() {
        CommandResult result = CommandRouter.route("Spotify aç");
        assertEquals(CommandResult.Type.OPEN_APP, result.type());
        assertEquals("spotify", result.text());
    }

    @Test public void routesSearch() {
        CommandResult result = CommandRouter.route("internette Ankara hava durumu ara");
        assertEquals(CommandResult.Type.WEB_SEARCH, result.type());
        assertEquals("ankara hava durumu", result.text());
    }

    @Test public void routesAlarm() {
        CommandResult result = CommandRouter.route("07:30 alarm kur");
        assertEquals(CommandResult.Type.SET_ALARM, result.type());
        assertEquals(7, result.hour());
        assertEquals(30, result.minute());
    }

    @Test public void routesTimerMinutes() {
        CommandResult result = CommandRouter.route("5 dakika zamanlayıcı kur");
        assertEquals(CommandResult.Type.SET_TIMER, result.type());
        assertEquals(300, result.value());
    }

    @Test public void routesTimerHourAndMinutes() {
        CommandResult result = CommandRouter.route("1 saat 20 dakika timer başlat");
        assertEquals(CommandResult.Type.SET_TIMER, result.type());
        assertEquals(4800, result.value());
    }

    @Test public void routesFlashlightOnAndOff() {
        assertEquals(CommandResult.Type.FLASHLIGHT_ON, CommandRouter.route("feneri aç").type());
        assertEquals(CommandResult.Type.FLASHLIGHT_OFF, CommandRouter.route("feneri kapat").type());
    }

    @Test public void routesVolumeCommands() {
        assertEquals(CommandResult.Type.VOLUME_UP, CommandRouter.route("sesi yükselt").type());
        assertEquals(CommandResult.Type.VOLUME_DOWN, CommandRouter.route("sesi azalt").type());
        assertEquals(CommandResult.Type.VOLUME_MUTE, CommandRouter.route("sesi kapat").type());
        assertEquals(CommandResult.Type.VOLUME_MAX, CommandRouter.route("sesi fulle").type());
    }

    @Test public void routesBatteryStatus() {
        assertEquals(CommandResult.Type.BATTERY_STATUS, CommandRouter.route("pil yüzde kaç").type());
    }

    @Test public void routesCamera() {
        assertEquals(CommandResult.Type.OPEN_CAMERA, CommandRouter.route("kamerayı aç").type());
    }

    @Test public void routesNavigation() {
        CommandResult result = CommandRouter.route("Kızılay'a yol tarifi aç");
        assertEquals(CommandResult.Type.NAVIGATE_TO, result.type());
        assertEquals("kızılay", result.text());
    }

    @Test public void routesDialNumber() {
        CommandResult result = CommandRouter.route("0555 123 45 67 numarasını ara");
        assertEquals(CommandResult.Type.DIAL_NUMBER, result.type());
        assertEquals("05551234567", result.text());
    }

    @Test public void routesSmsCompose() {
        CommandResult result = CommandRouter.route("0555 123 45 67 numarasına geliyorum diye mesaj yaz");
        assertEquals(CommandResult.Type.COMPOSE_SMS, result.type());
        assertEquals("05551234567", result.text());
        assertEquals("geliyorum", result.secondaryText());
    }

    @Test public void routesMediaCommands() {
        assertEquals(CommandResult.Type.MEDIA_PLAY_PAUSE, CommandRouter.route("müziği durdur").type());
        assertEquals(CommandResult.Type.MEDIA_NEXT, CommandRouter.route("sonraki şarkı").type());
        assertEquals(CommandResult.Type.MEDIA_PREVIOUS, CommandRouter.route("önceki şarkı").type());
    }

    @Test public void routesNoteCommands() {
        CommandResult create = CommandRouter.route("not al yarın kaynakçıyla konuş");
        assertEquals(CommandResult.Type.CREATE_NOTE, create.type());
        assertEquals("yarın kaynakçıyla konuş", create.text());
        assertEquals(CommandResult.Type.READ_LAST_NOTE, CommandRouter.route("son notumu oku").type());
    }

    @Test public void routesSettingsVariants() {
        assertEquals("wifi", CommandRouter.route("wifi ayarlarını aç").text());
        assertEquals("bluetooth", CommandRouter.route("bluetooth ayarlarını aç").text());
        assertEquals("location", CommandRouter.route("konum ayarlarını aç").text());
        assertEquals("display", CommandRouter.route("ekran ayarlarını aç").text());
    }

    @Test public void unknownCommandFallsBackToSearch() {
        CommandResult result = CommandRouter.route("Ankara'nın nüfusu");
        assertEquals(CommandResult.Type.WEB_SEARCH, result.type());
        assertEquals("ankara'nın nüfusu", result.text());
    }
}
