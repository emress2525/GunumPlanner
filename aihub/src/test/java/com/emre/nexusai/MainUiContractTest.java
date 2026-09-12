package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MainUiContractTest {
    @Test public void allSixModesExist() {
        assertEquals(6, RequestMode.values().length);
    }
}
