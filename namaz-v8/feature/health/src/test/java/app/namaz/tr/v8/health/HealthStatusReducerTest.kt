package app.namaz.tr.v8.health

import org.junit.Assert.assertEquals
import org.junit.Test

class HealthStatusReducerTest {
    @Test
    fun missingNotificationPermissionIsRedOnApi33Plus() {
        val result = HealthStatusReducer().reduce(
            api = 35,
            notificationGranted = false,
            exactAlarm = true,
            batteryRestricted = false,
        )
        assertEquals(HealthStatus.RED, result.first { it.id == "notification" }.status)
    }

    @Test
    fun missingExactAlarmIsRedOnApi31Plus() {
        val result = HealthStatusReducer().reduce(
            api = 35,
            notificationGranted = true,
            exactAlarm = false,
            batteryRestricted = false,
        )
        assertEquals(HealthStatus.RED, result.first { it.id == "exact_alarm" }.status)
    }
}
