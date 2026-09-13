package app.namaz.tr.v8

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildContractTest {
    @Test
    fun packageAndVersionAreStable() {
        assertEquals("app.namaz.tr.v8", BuildConfig.APPLICATION_ID)
        assertEquals(8, BuildConfig.VERSION_CODE)
        assertEquals("8.0.0-dev1", BuildConfig.VERSION_NAME)
    }
}
