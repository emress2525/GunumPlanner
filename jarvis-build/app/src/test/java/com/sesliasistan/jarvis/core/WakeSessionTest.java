package com.sesliasistan.jarvis.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WakeSessionTest {
    @Test
    public void wakeAloneArmsNextUtterance() {
        WakeSession session = new WakeSession(8000L);
        assertEquals(WakeSession.DecisionType.ARM, session.onUtterance("Jarvis", 1000L).type());
        WakeSession.Decision command = session.onUtterance("Spotify aç", 2000L);
        assertEquals(WakeSession.DecisionType.COMMAND, command.type());
        assertEquals("spotify aç", command.command());
        assertFalse(session.isArmed(2001L));
    }

    @Test
    public void wakeAndCommandExecuteImmediately() {
        WakeSession session = new WakeSession(8000L);
        WakeSession.Decision decision = session.onUtterance("Jarvis saat kaç", 1000L);
        assertEquals(WakeSession.DecisionType.COMMAND, decision.type());
        assertEquals("saat kaç", decision.command());
    }

    @Test
    public void armExpires() {
        WakeSession session = new WakeSession(8000L);
        session.onUtterance("Jarvis", 1000L);
        assertTrue(session.isArmed(9000L));
        assertEquals(WakeSession.DecisionType.IGNORE, session.onUtterance("Spotify aç", 9001L).type());
    }
}
