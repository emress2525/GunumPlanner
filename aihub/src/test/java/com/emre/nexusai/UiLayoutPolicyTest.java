package com.emre.nexusai;

import org.junit.Test;
import static org.junit.Assert.*;

public class UiLayoutPolicyTest {
    @Test
    public void addsStatusBarInsetToBaseTopPadding() {
        assertEquals(56, UiLayoutPolicy.safeTopPadding(24, 32));
    }

    @Test
    public void ignoresNegativeInset() {
        assertEquals(24, UiLayoutPolicy.safeTopPadding(24, -5));
    }
}
