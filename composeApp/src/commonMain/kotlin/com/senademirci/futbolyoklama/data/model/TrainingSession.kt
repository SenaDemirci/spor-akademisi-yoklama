package com.senademirci.futbolyoklama.data.model

/** Yoklama alınmış tek bir antrenman. */
data class TrainingSession(
    val id: String = "",
    val ownerUid: String = "",
    val teamId: String = "",
    /** yyyy-MM-dd */
    val date: String = "",
    val note: String = "",
    val playerCount: Int = 0,
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val excusedCount: Int = 0,
    val lateCount: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val attendedCount: Int get() = presentCount + lateCount
}
