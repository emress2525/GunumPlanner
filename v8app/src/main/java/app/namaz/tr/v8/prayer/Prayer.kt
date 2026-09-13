package app.namaz.tr.v8.prayer

enum class Prayer(
    val displayName: String,
    val isTrackable: Boolean
) {
    FAJR("Sabah", true),
    SUNRISE("Güneş", false),
    DHUHR("Öğle", true),
    ASR("İkindi", true),
    MAGHRIB("Akşam", true),
    ISHA("Yatsı", true)
}
