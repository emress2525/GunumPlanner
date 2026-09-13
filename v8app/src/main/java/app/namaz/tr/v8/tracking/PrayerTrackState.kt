package app.namaz.tr.v8.tracking

enum class PrayerTrackState(val label: String) {
    UNSET("İşaretlenmedi"),
    PRAYED("Kıldım"),
    ON_TIME("Vaktinde"),
    CONGREGATION("Cemaatle"),
    QADA("Kazaya kaldı")
}
