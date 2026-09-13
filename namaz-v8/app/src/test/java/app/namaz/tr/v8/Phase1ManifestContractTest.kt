package app.namaz.tr.v8

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class Phase1ManifestContractTest {
    @Test
    fun phase1IdentityAndRuntimeComponentsExist() {
        assertEquals("app.namaz.tr.v8", BuildConfig.APPLICATION_ID)
        assertEquals(8, BuildConfig.VERSION_CODE)
        assertEquals("8.0.0", BuildConfig.VERSION_NAME)
        assertNotNull(Class.forName("app.namaz.tr.v8.alarm.PrayerAlarmReceiver"))
        assertNotNull(Class.forName("app.namaz.tr.v8.alarm.BootReceiver"))
        assertNotNull(Class.forName("app.namaz.tr.v8.adhan.AdhanService"))
    }
}
