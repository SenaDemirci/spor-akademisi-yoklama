package com.senademirci.futbolyoklama.util

/** "1500" -> "1.500 ₺". Binlik ayracı Türkçe biçimde nokta. */
fun formatMoney(amount: Int): String {
    val digits = amount.toString()
    val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
    return "$grouped ₺"
}
