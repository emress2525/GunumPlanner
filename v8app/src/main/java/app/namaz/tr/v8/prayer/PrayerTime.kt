package app.namaz.tr.v8.prayer

import java.time.LocalDateTime

data class PrayerTime(
    val prayer: Prayer,
    val at: LocalDateTime
)
