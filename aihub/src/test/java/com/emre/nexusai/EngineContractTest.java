package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;

public class EngineContractTest {
    @Test public void callbackTypeExists() {
        NexusAiEngine.Callback callback = new NexusAiEngine.Callback() {
            @Override public void onResult(NexusResult result) { }
        };
        assertNotNull(callback);
    }
}
