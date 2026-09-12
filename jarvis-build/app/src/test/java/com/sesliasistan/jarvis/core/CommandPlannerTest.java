package com.sesliasistan.jarvis.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;

public class CommandPlannerTest {
    @Test public void plansTwoSequentialCommands() {
        List<CommandResult> actions = CommandPlanner.plan("Spotify aç sonra sesi yükselt");
        assertEquals(2, actions.size());
        assertEquals(CommandResult.Type.OPEN_APP, actions.get(0).type());
        assertEquals("spotify", actions.get(0).text());
        assertEquals(CommandResult.Type.VOLUME_UP, actions.get(1).type());
    }

    @Test public void supportsArdindanSeparator() {
        List<CommandResult> actions = CommandPlanner.plan("kamerayı aç ardından 5 dakika zamanlayıcı kur");
        assertEquals(2, actions.size());
        assertEquals(CommandResult.Type.OPEN_CAMERA, actions.get(0).type());
        assertEquals(CommandResult.Type.SET_TIMER, actions.get(1).type());
        assertEquals(300, actions.get(1).value());
    }

    @Test public void supportsDahaSonraSeparator() {
        List<CommandResult> actions = CommandPlanner.plan("saat kaç daha sonra pil yüzde kaç");
        assertEquals(2, actions.size());
        assertEquals(CommandResult.Type.TIME, actions.get(0).type());
        assertEquals(CommandResult.Type.BATTERY_STATUS, actions.get(1).type());
    }

    @Test public void doesNotSplitRoutineDefinitionBody() {
        List<CommandResult> actions = CommandPlanner.plan("rutin kaydet eve geldim: spotify aç sonra sesi yükselt");
        assertEquals(1, actions.size());
        assertEquals(CommandResult.Type.SAVE_ROUTINE, actions.get(0).type());
        assertEquals("eve geldim", actions.get(0).text());
        assertEquals("spotify aç sonra sesi yükselt", actions.get(0).secondaryText());
    }

    @Test public void capsPlansAtEightActions() {
        String command = "saat kaç sonra saat kaç sonra saat kaç sonra saat kaç sonra " +
                "saat kaç sonra saat kaç sonra saat kaç sonra saat kaç sonra saat kaç";
        List<CommandResult> actions = CommandPlanner.plan(command);
        assertEquals(8, actions.size());
        for (CommandResult action : actions) assertEquals(CommandResult.Type.TIME, action.type());
    }

    @Test public void blankInputProducesHelp() {
        List<CommandResult> actions = CommandPlanner.plan("   ");
        assertEquals(1, actions.size());
        assertEquals(CommandResult.Type.HELP, actions.get(0).type());
    }
}
