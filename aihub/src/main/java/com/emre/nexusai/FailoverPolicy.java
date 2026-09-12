package com.emre.nexusai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FailoverPolicy {
    private FailoverPolicy() { }

    public static List<NexusConfig.TextEndpoint> ordered(List<NexusConfig.TextEndpoint> endpoints) {
        if (endpoints == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(endpoints));
    }
}
