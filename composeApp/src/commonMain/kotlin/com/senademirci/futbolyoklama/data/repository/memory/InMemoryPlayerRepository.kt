package com.senademirci.futbolyoklama.data.repository.memory

import com.senademirci.futbolyoklama.data.model.Player
import com.senademirci.futbolyoklama.data.repository.AuthRepository
import com.senademirci.futbolyoklama.util.turkishSortKey
import com.senademirci.futbolyoklama.data.repository.PlayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryPlayerRepository(
    private val store: InMemoryStore,
    private val authRepository: AuthRepository,
) : PlayerRepository {

    private val byName =
        compareBy<Player>({ turkishSortKey(it.lastName) }, { turkishSortKey(it.firstName) })

    override fun observeRoster(teamId: String): Flow<List<Player>> =
        store.players.map { list ->
            list.filter { it.teamId == teamId && it.isActive }.sortedWith(byName)
        }

    override fun observeAll(teamId: String): Flow<List<Player>> =
        store.players.map { list -> list.filter { it.teamId == teamId }.sortedWith(byName) }

    override fun observeAllForOwner(): Flow<List<Player>> =
        combine(store.players, authRepository.currentCoach) { players, coach ->
            if (coach == null) emptyList() else players.filter { it.ownerUid == coach.uid }
        }

    override fun observePlayer(playerId: String): Flow<Player?> =
        store.players.map { list -> list.firstOrNull { it.id == playerId } }

    override suspend fun upsert(player: Player): Result<String> {
        val id = player.id.ifBlank { store.newId("player") }
        store.players.update { list ->
            val existing = list.firstOrNull { it.id == id }
            if (existing == null) {
                list + player.copy(id = id)
            } else {
                // stats yoklama tarafından yönetilir; düzenleme ekranı onu ezmemeli
                list.map { if (it.id == id) player.copy(id = id, stats = existing.stats) else it }
            }
        }
        return Result.success(id)
    }

    override suspend fun archive(playerId: String): Result<Unit> = setActive(playerId, false)

    override suspend fun restore(playerId: String): Result<Unit> = setActive(playerId, true)

    private fun setActive(playerId: String, active: Boolean): Result<Unit> {
        store.players.update { list ->
            list.map { if (it.id == playerId) it.copy(isActive = active) else it }
        }
        return Result.success(Unit)
    }

    override suspend fun deletePermanently(playerId: String): Result<Unit> {
        store.players.update { list -> list.filterNot { it.id == playerId } }
        store.records.update { list -> list.filterNot { it.playerId == playerId } }
        store.dues.update { list -> list.filterNot { it.playerId == playerId } }
        return Result.success(Unit)
    }
}
