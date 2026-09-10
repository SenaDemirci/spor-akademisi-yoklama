package com.senademirci.futbolyoklama.data.repository.memory

import com.senademirci.futbolyoklama.data.model.AttendanceRecord
import com.senademirci.futbolyoklama.data.model.AttendanceStatus
import com.senademirci.futbolyoklama.data.model.PlayerStats
import com.senademirci.futbolyoklama.data.model.TrainingSession
import com.senademirci.futbolyoklama.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryAttendanceRepository(private val store: InMemoryStore) : AttendanceRepository {

    override fun observeSessions(teamId: String): Flow<List<TrainingSession>> =
        store.sessions.map { list ->
            list.filter { it.teamId == teamId }.sortedByDescending { it.date }
        }

    override fun observeSession(sessionId: String): Flow<TrainingSession?> =
        store.sessions.map { list -> list.firstOrNull { it.id == sessionId } }

    override fun observeRecordsForSession(sessionId: String): Flow<List<AttendanceRecord>> =
        store.records.map { list -> list.filter { it.sessionId == sessionId } }

    override fun observeRecordsInRange(
        teamId: String,
        fromDate: String,
        toDate: String,
    ): Flow<List<AttendanceRecord>> = store.records.map { list ->
        list.filter { it.teamId == teamId && it.date >= fromDate && it.date <= toDate }
    }

    override fun observeRecordsForPlayer(playerId: String, limit: Int): Flow<List<AttendanceRecord>> =
        store.records.map { list ->
            list.filter { it.playerId == playerId }.sortedByDescending { it.date }.take(limit)
        }

    override suspend fun findSessionByDate(teamId: String, date: String): TrainingSession? =
        store.sessions.value.firstOrNull { it.teamId == teamId && it.date == date }

    override suspend fun saveAttendance(
        sessionId: String?,
        teamId: String,
        date: String,
        note: String,
        statuses: Map<String, AttendanceStatus>,
        playerNames: Map<String, String>,
    ): Result<String> {
        val ownerUid = store.teams.value.firstOrNull { it.id == teamId }?.ownerUid
            ?: return Result.failure(IllegalStateException("Takım bulunamadı."))

        val id = sessionId ?: store.newId("session")
        val counts = statuses.values.groupingBy { it }.eachCount()

        val session = TrainingSession(
            id = id,
            ownerUid = ownerUid,
            teamId = teamId,
            date = date,
            note = note,
            playerCount = statuses.size,
            presentCount = counts[AttendanceStatus.PRESENT] ?: 0,
            absentCount = counts[AttendanceStatus.ABSENT] ?: 0,
            excusedCount = counts[AttendanceStatus.EXCUSED] ?: 0,
            lateCount = counts[AttendanceStatus.LATE] ?: 0,
        )

        store.sessions.update { list ->
            if (list.any { it.id == id }) list.map { if (it.id == id) session else it }
            else list + session
        }

        val newRecords = statuses.map { (playerId, status) ->
            AttendanceRecord(
                id = AttendanceRecord.idFor(id, playerId),
                ownerUid = ownerUid,
                teamId = teamId,
                sessionId = id,
                playerId = playerId,
                playerName = playerNames[playerId].orEmpty(),
                date = date,
                status = status,
            )
        }
        // Bu antrenmanın eski kayıtlarını at, yenilerini yaz — düzenleme de böylece doğru çalışır
        store.records.update { list -> list.filterNot { it.sessionId == id } + newRecords }

        recomputeStats(teamId)
        return Result.success(id)
    }

    override suspend fun deleteSession(sessionId: String): Result<Unit> {
        val teamId = store.sessions.value.firstOrNull { it.id == sessionId }?.teamId
        store.sessions.update { list -> list.filterNot { it.id == sessionId } }
        store.records.update { list -> list.filterNot { it.sessionId == sessionId } }
        teamId?.let { recomputeStats(it) }
        return Result.success(Unit)
    }

    /**
     * Firestore sürümünde sayaçlar artımlı güncellenir. Bellek içi sürümde tüm
     * kayıtlardan baştan hesaplamak hem daha kısa hem her zaman tutarlı.
     */
    private fun recomputeStats(teamId: String) {
        val byPlayer = store.records.value.filter { it.teamId == teamId }.groupBy { it.playerId }
        store.players.update { list ->
            list.map { player ->
                if (player.teamId != teamId) return@map player
                val recs = byPlayer[player.id].orEmpty()
                player.copy(
                    stats = PlayerStats(
                        totalSessions = recs.size,
                        present = recs.count { it.status == AttendanceStatus.PRESENT },
                        absent = recs.count { it.status == AttendanceStatus.ABSENT },
                        excused = recs.count { it.status == AttendanceStatus.EXCUSED },
                        late = recs.count { it.status == AttendanceStatus.LATE },
                    ),
                )
            }
        }
    }
}
