package app.namaz.tr.v8.tools

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object QiblaMath {
    private const val KAABA_LATITUDE = 21.4225
    private const val KAABA_LONGITUDE = 39.8262

    fun bearing(latitude: Double, longitude: Double): Double {
        require(latitude in -90.0..90.0) { "Geçersiz enlem" }
        require(longitude in -180.0..180.0) { "Geçersiz boylam" }
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(KAABA_LATITUDE)
        val deltaLon = Math.toRadians(KAABA_LONGITUDE - longitude)
        val y = sin(deltaLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }
}
