package app.namaz.tr.v8.onboarding

enum class UserProfile(
    val title: String,
    val description: String
) {
    BEGINNER_RELIGION(
        "Dini sıfırdan öğrenmek istiyorum",
        "İman, abdest, namaz ve günlük dini bilgileri adım adım öğren."
    ),
    NEW_TO_PRAYER(
        "Namaza yeni başladım",
        "Vakitler, namaz rehberi ve takip sistemi öne çıksın."
    ),
    LEARN_QURAN(
        "Kur’an öğrenmek istiyorum",
        "Elif-Bâ, okuma, meal, ses ve ezber araçları öne çıksın."
    ),
    DAILY_TRACKING(
        "Sadece günlük ibadetlerimi takip edeceğim",
        "Sade ana ekran, vakitler ve ibadet takibi öncelikli olsun."
    )
}
