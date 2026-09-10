package com.senademirci.futbolyoklama.util

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Uygulama boyunca tarihler "yyyy-MM-dd" metni olarak taşınır — Firestore'da sıralanabilir.
 *
 * Saat kaynağı olarak `kotlin.time.Clock` kullanılır; `kotlinx.datetime.Clock` artık
 * buna taşındı ve iOS/Native tarafında çözülmüyor.
 */
@OptIn(ExperimentalTime::class)
object DateUtil {

    private fun todayLocal(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    fun today(): String = todayLocal().toString()

    fun daysAgo(days: Int): String = todayLocal().minus(DatePeriod(days = days)).toString()

    fun monthsAgo(months: Int): String = todayLocal().minus(DatePeriod(months = months)).toString()

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
        "${d.day} ${monthNames[d.month.ordinal]} ${d.year} ${dayNames[d.dayOfWeek.ordinal]}"
    }.getOrDefault(isoDate)

    /** "2026-09-10" -> "10 Eylül" */
    fun formatShort(isoDate: String): String = runCatching {
        val d = LocalDate.parse(isoDate)
        "${d.day} ${monthNames[d.month.ordinal]}"
    }.getOrDefault(isoDate)

    /** Doğum tarihinden bugünkü yaş. */
    fun ageFrom(isoBirthDate: String?): Int? = runCatching {
        val b = LocalDate.parse(isoBirthDate ?: return null)
        val today = todayLocal()
        var age = today.year - b.year
        if (today.month.ordinal < b.month.ordinal ||
            (today.month.ordinal == b.month.ordinal && today.day < b.day)
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
