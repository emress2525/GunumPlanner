package app.namaz.tr.v8.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaMathTest {
    @Test fun turkey_bearings_point_southeast() {
        val ankara = QiblaMath.bearing(39.9334, 32.8597)
        val istanbul = QiblaMath.bearing(41.0082, 28.9784)
        assertTrue("Ankara bearing=$ankara", ankara in 145.0..175.0)
        assertTrue("Istanbul bearing=$istanbul", istanbul in 140.0..170.0)
    }
}
