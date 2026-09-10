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

    // ---- Aidat ayları ("yyyy-MM") ----

    /** İçinde bulunulan ay, "yyyy-MM". */
    fun currentPeriod(): String = periodOf(todayLocal())

    fun periodOf(date: LocalDate): String =
        "${date.year}-${date.month.ordinal.plus(1).toString().padStart(2, '0')}"

    fun periodOfIsoDate(isoDate: String): String =
        runCatching { periodOf(LocalDate.parse(isoDate)) }.getOrDefault(currentPeriod())

    /** "2026-09" -> "Eylül 2026" */
    fun periodLabel(period: String): String = runCatching {
        val (y, m) = period.split("-")
        "${monthNames[m.toInt() - 1]} $y"
    }.getOrDefault(period)

    /** Ayı [delta] kadar kaydırır: shiftPeriod("2026-09", -1) == "2026-08" */
    fun shiftPeriod(period: String, delta: Int): String = runCatching {
        val (y, m) = period.split("-").map { it.toInt() }
        val total = y * 12 + (m - 1) + delta
        val ny = total / 12
        val nm = total % 12 + 1
        "$ny-${nm.toString().padStart(2, '0')}"
    }.getOrDefault(period)

    /** [start]'tan [end]'e kadar (ikisi dahil) tüm aylar. Sıra bozuksa boş liste. */
    fun periodsBetween(start: String, end: String): List<String> {
        if (start.isBlank() || end.isBlank() || start > end) return emptyList()
        val result = mutableListOf<String>()
        var p = start
        // Makul bir üst sınır: 10 sezon
        while (p <= end && result.size < 120) {
            result += p
            p = shiftPeriod(p, 1)
        }
        return result
    }
}
