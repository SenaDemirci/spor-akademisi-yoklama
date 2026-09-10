package com.senademirci.futbolyoklama.data.model

/**
 * Bir öğrencinin bir aya ait aidat kaydı.
 * Belge kimliği "{playerId}_{period}" — aynı aya iki kayıt yazılamaz.
 *
 * Kayıt yoksa o ay "ödenmedi" sayılır; böylece her ay için önceden kayıt
 * oluşturmaya gerek kalmaz.
 */
data class DuesRecord(
    val id: String = "",
    val ownerUid: String = "",
    val clubId: String = "",
    val teamId: String = "",
    val playerId: String = "",
    val playerName: String = "",
    /** "yyyy-MM" */
    val period: String = "",
    val amount: Int = 0,
    val isPaid: Boolean = false,
    /** Ödendiği tarih, "yyyy-MM-dd". Ödenmediyse null. */
    val paidDate: String? = null,
    val note: String = "",
) {
    companion object {
        fun idFor(playerId: String, period: String) = "${playerId}_$period"
    }
}
