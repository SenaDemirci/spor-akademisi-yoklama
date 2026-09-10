package com.senademirci.futbolyoklama.data.model

/** Bir kulübün yaş grubu takımı (U8, U9, ... U16). */
data class Team(
    val id: String = "",
    val ownerUid: String = "",
    val clubId: String = "",
    val name: String = "",
    val season: String = "",
    /** Aylık aidat tutarı (TL). 0 ise aidat takibi yapılmıyor demektir. */
    val monthlyDuesAmount: Int = 0,
    /** Aidat takibinin başladığı ay, "yyyy-MM". Borç bu aydan itibaren hesaplanır. */
    val duesStartPeriod: String = "",
    val createdAt: Long = 0L,
)
