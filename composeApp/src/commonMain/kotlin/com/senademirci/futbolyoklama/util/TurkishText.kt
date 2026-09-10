package com.senademirci.futbolyoklama.util

/**
 * Türkçe alfabe sırası. Varsayılan metin karşılaştırması Unicode kod noktalarına
 * bakar; orada "ç" harfi "z"den sonra gelir ve Çetin listenin sonuna düşer.
 */
private const val TURKISH_ALPHABET = "abcçdefgğhıijklmnoöprsştuüvyz"

/** Büyük harfe çevirirken Türkçe'ye özgü i/ı ayrımını korur. */
private val lowercaseMap = mapOf(
    'I' to 'ı', 'İ' to 'i', 'Ç' to 'ç', 'Ğ' to 'ğ',
    'Ö' to 'ö', 'Ş' to 'ş', 'Ü' to 'ü',
)

private fun turkishLowercase(c: Char): Char = lowercaseMap[c] ?: c.lowercaseChar()

/**
 * Sıralama anahtarı: her harf Türkçe alfabedeki sırasına göre iki haneli bir
 * sayıya çevrilir. Alfabede olmayan karakterler (boşluk, tire, rakam) en sona atılır.
 */
fun turkishSortKey(text: String): String = buildString {
    text.forEach { ch ->
        val lower = turkishLowercase(ch)
        val index = TURKISH_ALPHABET.indexOf(lower)
        append(if (index >= 0) index.toString().padStart(2, '0') else "99")
    }
}

/** Türkçe harf duyarlı arama karşılaştırması için normalleştirme. */
fun turkishLowercase(text: String): String = text.map(::turkishLowercase).joinToString("")
