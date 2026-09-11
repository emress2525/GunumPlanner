package com.sesliasistan.jarvis.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CommandRouterTest {
    @Test
    public void detectsWakeWordAndInlineCommand() {
        CommandRouter.WakeResult result = CommandRouter.extractAfterWakeWord("Jarvis Spotify'ı aç");
        assertTrue(result.wakeDetected());
        assertEquals("spotify'ı aç", result.command());
    }

    @Test
    public void ignoresSpeechWithoutWakeWord() {
        CommandRouter.WakeResult result = CommandRouter.extractAfterWakeWord("Spotify aç");
        assertFalse(result.wakeDetected());
        assertEquals("", result.command());
    }

    @Test
    public void routesOpenApp() {
        CommandResult result = CommandRouter.route("Spotify aç");
        assertEquals(CommandResult.Type.OPEN_APP, result.type());
        assertEquals("spotify", result.text());
    }

    @Test
    public void routesSearch() {
        CommandResult result = CommandRouter.route("internette Ankara hava durumu ara");
        assertEquals(CommandResult.Type.WEB_SEARCH, result.type());
        assertEquals("ankara hava durumu", result.text());
    }

    @Test
    public void routesAlarm() {
        CommandResult result = CommandRouter.route("07:30 alarm kur");
        assertEquals(CommandResult.Type.SET_ALARM, result.type());
        assertEquals(7, result.hour());
        assertEquals(30, result.minute());
    }

    @Test
    public void unknownCommandFallsBackToSearch() {
        CommandResult result = CommandRouter.route("Ankara'nın nüfusu");
        assertEquals(CommandResult.Type.WEB_SEARCH, result.type());
        assertEquals("ankara'nın nüfusu", result.text());
    }
}
