package app.namaz.tr.v8

import org.junit.Assert.assertEquals
import org.junit.Test

class AppIdentityTest {
    @Test
    fun expectedIdentityIsStable() {
        assertEquals("app.namaz.tr.v8", BuildConfig.APPLICATION_ID)
        assertEquals(8, BuildConfig.VERSION_CODE)
        assertEquals("8.0.0", BuildConfig.VERSION_NAME)
    }
}
