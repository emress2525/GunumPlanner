package com.unifiedai.app.data.provider.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderFactoryUrlTest {
    @Test
    fun blankOrMalformedOllamaUrlIsIgnored() {
        assertNull(ollamaBaseUrlOrNull(""))
        assertNull(ollamaBaseUrlOrNull("   "))
        assertNull(ollamaBaseUrlOrNull("192.168.1.50:11434"))
    }

    @Test
    fun validHttpOllamaUrlIsAccepted() {
        assertEquals(
            "http://192.168.1.50:11434/",
            ollamaBaseUrlOrNull("http://192.168.1.50:11434/")?.toString(),
        )
    }
}
