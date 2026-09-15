package app.namaz.tr.v8.quran

object HatimPlan {
    fun pagesPerDay(totalPages: Int = 604, days: Int): Int {
        require(totalPages > 0) { "Toplam sayfa sayısı sıfırdan büyük olmalı" }
        require(days > 0) { "Gün sayısı sıfırdan büyük olmalı" }
        return (totalPages + days - 1) / days
    }
}
