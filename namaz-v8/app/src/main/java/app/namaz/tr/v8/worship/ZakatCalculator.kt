package app.namaz.tr.v8.worship

object ZakatCalculator {
    fun amount(eligibleWealth: Double, rate: Double = 0.025): Double {
        require(eligibleWealth >= 0.0) { "Tutar negatif olamaz" }
        require(rate >= 0.0) { "Oran negatif olamaz" }
        return eligibleWealth * rate
    }
}
