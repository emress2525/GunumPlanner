package app.namaz.tr.v8.settings

import app.namaz.tr.v8.model.AppDestination
import app.namaz.tr.v8.model.AppMode
import app.namaz.tr.v8.model.UserProfile
import app.namaz.tr.v8.model.availableDestinations
import app.namaz.tr.v8.model.defaultModeFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileRulesTest {
    @Test
    fun beginnerProfileStartsInSimpleMode() {
        assertEquals(AppMode.SIMPLE, defaultModeFor(UserProfile.RELIGION_FROM_ZERO))
    }

    @Test
    fun everyProfileKeepsAllDestinationsAvailable() {
        UserProfile.entries.forEach { profile ->
            assertTrue(availableDestinations(profile).containsAll(AppDestination.entries))
        }
    }
}
