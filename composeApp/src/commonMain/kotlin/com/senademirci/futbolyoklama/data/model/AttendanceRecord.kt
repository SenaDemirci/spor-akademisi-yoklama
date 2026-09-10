package com.senademirci.futbolyoklama.data.model

/**
 * Tek öğrencinin tek antrenmandaki kaydı.
 * Belge kimliği "{sessionId}_{playerId}" — aynı kayıt iki kez yazılamaz.
 */
data class AttendanceRecord(
    val id: String = "",
    val ownerUid: String = "",
    val teamId: String = "",
    val sessionId: String = "",
    val playerId: String = "",
    /** Rapor tek sorguda çıksın diye öğrenci adı burada da tutulur. */
    val playerName: String = "",
    /** yyyy-MM-dd */
    val date: String = "",
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val note: String = "",
) {
    companion object {
        fun idFor(sessionId: String, playerId: String) = "${sessionId}_$playerId"
    }
}
