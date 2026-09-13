package app.namaz.tr.v8.prayer

import app.namaz.tr.v8.model.PrayerName
import app.namaz.tr.v8.model.QazaState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PrayerTrackingRulesTest {
    @Test
    fun sunriseCannotBeTrackedAsObligatoryPrayer() {
        assertFalse(PrayerName.SUNRISE.isTrackable)
    }

    @Test
    fun appNeverDerivesHistoricQazaDebt() {
        val state = QazaState(userEntered = 120)
        assertEquals(120, state.total)
    }
}
