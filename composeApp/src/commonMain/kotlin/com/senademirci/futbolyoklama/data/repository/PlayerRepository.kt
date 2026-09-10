package com.senademirci.futbolyoklama.data.repository

import com.senademirci.futbolyoklama.data.model.Player
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    /** Kadro, soyada göre sıralı ve canlı. Arşivlenenler dahil edilmez. */
    fun observeRoster(teamId: String): Flow<List<Player>>

    /** Arşivlenmiş öğrenciler dahil hepsi — rapor geçmişi için gerekir. */
    fun observeAll(teamId: String): Flow<List<Player>>

    fun observePlayer(playerId: String): Flow<Player?>

    suspend fun upsert(player: Player): Result<String>

    /** Kadrodan çıkarır ama geçmiş yoklamaları korur. */
    suspend fun archive(playerId: String): Result<Unit>

    suspend fun restore(playerId: String): Result<Unit>

    /** Öğrenciyi ve tüm yoklama kayıtlarını kalıcı olarak siler. Geri alınamaz. */
    suspend fun deletePermanently(playerId: String): Result<Unit>
}
