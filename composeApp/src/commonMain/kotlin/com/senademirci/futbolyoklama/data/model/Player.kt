package com.senademirci.futbolyoklama.data.model

data class Player(
    val id: String = "",
    val ownerUid: String = "",
    val teamId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    /** yyyy-MM-dd biçiminde, opsiyonel. */
    val birthDate: String? = null,
    val jerseyNumber: Int? = null,
    val parentPhone: String? = null,
    val photoUrl: String? = null,
    /** false ise kadrodan çıkarılmış; geçmiş yoklamaları korunur. */
    val isActive: Boolean = true,
    val stats: PlayerStats = PlayerStats(),
    val createdAt: Long = 0L,
) {
    val fullName: String get() = "$firstName $lastName".trim()

    val initials: String
        get() = buildString {
            firstName.firstOrNull()?.let { append(it.uppercaseChar()) }
            lastName.firstOrNull()?.let { append(it.uppercaseChar()) }
        }.ifEmpty { "?" }
}
