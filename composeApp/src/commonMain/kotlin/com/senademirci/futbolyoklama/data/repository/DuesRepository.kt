package com.senademirci.futbolyoklama.data.repository

import com.senademirci.futbolyoklama.data.model.DuesRecord
import kotlinx.coroutines.flow.Flow

interface DuesRepository {
    /** Bir takımın tüm aidat kayıtları — borç hesabı için tamamı gerekir. */
    fun observeDuesForTeam(teamId: String): Flow<List<DuesRecord>>

    fun observeDuesForPlayer(playerId: String): Flow<List<DuesRecord>>

    /** Ödendi/ödenmedi işaretler. Ödenmedi'ye çekilirse kayıt silinir. */
    suspend fun setPaid(
        playerId: String,
        period: String,
        isPaid: Boolean,
        amount: Int,
    ): Result<Unit>

    suspend fun setNote(playerId: String, period: String, note: String): Result<Unit>
}
