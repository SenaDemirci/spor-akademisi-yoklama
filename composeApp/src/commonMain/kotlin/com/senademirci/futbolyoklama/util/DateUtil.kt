package com.senademirci.futbolyoklama.util

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.Instant
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime

/** Uygulama boyunca tarihler "yyyy-MM-dd" metni olarak taşınır — Firestore'da sıralanabilir. */
object DateUtil {

    fun today(): String = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

    fun daysAgo(days: Int): String =
        Clock.System.todayIn(TimeZone.currentSystemDefault())
            .minus(DatePeriod(days = days))
            .toString()

    fun monthsAgo(months: Int): String =
        Clock.System.todayIn(TimeZone.currentSystemDefault())
            .minus(DatePeriod(months = months))
            .toString()

    private val monthNames = listOf(
        "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
        "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık",
    )

    private val dayNames = listOf(
        "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar",
    )

    /** "2026-09-10" -> "10 Eylül 2026 Perşembe" */
    fun formatLong(isoDate: String): String = runCatching {
        val d = LocalDate.parse(isoDate)
        "${d.dayOfMonth} ${monthNames[d.monthNumber - 1]} ${d.year} ${dayNames[d.dayOfWeek.ordinal]}"
    }.getOrDefault(isoDate)

    /** "2026-09-10" -> "10 Eylül" */
    fun formatShort(isoDate: String): String = runCatching {
        val d = LocalDate.parse(isoDate)
        "${d.dayOfMonth} ${monthNames[d.monthNumber - 1]}"
    }.getOrDefault(isoDate)

    /** Doğum tarihinden bugünkü yaş. */
    fun ageFrom(isoBirthDate: String?): Int? = runCatching {
        val b = LocalDate.parse(isoBirthDate ?: return null)
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        var age = today.year - b.year
        if (today.monthNumber < b.monthNumber ||
            (today.monthNumber == b.monthNumber && today.dayOfMonth < b.dayOfMonth)
        ) age--
        age.takeIf { it >= 0 }
    }.getOrNull()

    fun isValidIsoDate(text: String): Boolean = runCatching { LocalDate.parse(text) }.isSuccess

    /**
     * Material3 tarih seçici UTC gece yarısını milisaniye olarak kullanır;
     * dönüşümlerin ikisi de UTC üzerinden yapılır ki gün kaymasın.
     */
    fun isoToEpochMillis(isoDate: String): Long = runCatching {
        LocalDate.parse(isoDate).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    }.getOrElse { 0L }

    fun epochMillisToIso(millis: Long): String =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date.toString()
}
