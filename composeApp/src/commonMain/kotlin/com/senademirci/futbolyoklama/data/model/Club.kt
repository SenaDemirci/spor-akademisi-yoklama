package com.senademirci.futbolyoklama.data.model

/** Bir kulüp; altında yaş gruplarına göre takımlar bulunur. */
data class Club(
    val id: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val createdAt: Long = 0L,
)
