package com.sesliasistan.jarvis.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CommandPlanner {
    private static final int MAX_ACTIONS = 8;
    private static final String ROUTINE_SAVE_PREFIX = "rutin kaydet ";

    private CommandPlanner() { }

    public static List<CommandResult> plan(String rawCommand) {
        String normalized = CommandRouter.normalize(rawCommand);
        if (normalized.isEmpty()) {
            return Collections.singletonList(CommandResult.simple(CommandResult.Type.HELP));
        }

        if (normalized.startsWith(ROUTINE_SAVE_PREFIX)) {
            return Collections.singletonList(CommandRouter.route(normalized));
        }

        String[] parts = normalized.split("\\s+(?:daha sonra|ardından|sonra)\\s+");
        List<CommandResult> actions = new ArrayList<>();
        for (String part : parts) {
            String command = part.trim();
            if (command.isEmpty()) continue;
            actions.add(CommandRouter.route(command));
            if (actions.size() >= MAX_ACTIONS) break;
        }

        if (actions.isEmpty()) {
            actions.add(CommandResult.simple(CommandResult.Type.HELP));
        }
        return Collections.unmodifiableList(actions);
    }
}
