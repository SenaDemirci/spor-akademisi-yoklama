package com.senademirci.futbolyoklama.data.model

import kotlin.math.roundToInt

/**
 * Öğrenci belgesinde tutulan denormalize sayaçlar. Yoklama kaydedilirken atomik
 * olarak güncellenir; böylece rapor ekranı hiç sorgu çalıştırmadan açılır.
 */
data class PlayerStats(
    val totalSessions: Int = 0,
    val present: Int = 0,
    val absent: Int = 0,
    val excused: Int = 0,
    val late: Int = 0,
) {
    /** İzinli olunan antrenmanlar paydadan düşülür — mazeretli yokluk cezalandırılmaz. */
    private val accountableSessions: Int get() = totalSessions - excused

    val absenceRate: Float
        get() = if (accountableSessions <= 0) 0f else absent.toFloat() / accountableSessions

    val absencePercent: Int get() = (absenceRate * 100).roundToInt()

    val attendedCount: Int get() = present + late
}
