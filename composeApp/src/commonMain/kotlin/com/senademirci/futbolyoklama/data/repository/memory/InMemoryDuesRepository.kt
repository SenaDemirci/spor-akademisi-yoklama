package com.senademirci.futbolyoklama.data.repository.memory

import com.senademirci.futbolyoklama.data.model.DuesRecord
import com.senademirci.futbolyoklama.data.repository.DuesRepository
import com.senademirci.futbolyoklama.util.DateUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryDuesRepository(private val store: InMemoryStore) : DuesRepository {

    override fun observeDuesForTeam(teamId: String): Flow<List<DuesRecord>> =
        store.dues.map { list -> list.filter { it.teamId == teamId } }

    override fun observeDuesForPlayer(playerId: String): Flow<List<DuesRecord>> =
        store.dues.map { list ->
            list.filter { it.playerId == playerId }.sortedByDescending { it.period }
        }

    override suspend fun setPaid(
        playerId: String,
        period: String,
        isPaid: Boolean,
        amount: Int,
    ): Result<Unit> {
        val id = DuesRecord.idFor(playerId, period)

        if (!isPaid) {
            // "Ödenmedi" ayrı bir durum değil, kaydın yokluğu — geri alınca kaydı siliyoruz
            store.dues.update { list -> list.filterNot { it.id == id } }
            return Result.success(Unit)
        }

        val player = store.players.value.firstOrNull { it.id == playerId }
            ?: return Result.failure(IllegalStateException("Öğrenci bulunamadı."))

        val existingNote = store.dues.value.firstOrNull { it.id == id }?.note.orEmpty()
        val record = DuesRecord(
            id = id,
            ownerUid = player.ownerUid,
            clubId = player.clubId,
            teamId = player.teamId,
            playerId = playerId,
            playerName = player.fullName,
            period = period,
            amount = amount,
            isPaid = true,
            paidDate = DateUtil.today(),
            note = existingNote,
        )
        store.dues.update { list -> list.filterNot { it.id == id } + record }
        return Result.success(Unit)
    }

    override suspend fun setNote(playerId: String, period: String, note: String): Result<Unit> {
        val id = DuesRecord.idFor(playerId, period)
        store.dues.update { list ->
            list.map { if (it.id == id) it.copy(note = note) else it }
        }
        return Result.success(Unit)
    }
}
