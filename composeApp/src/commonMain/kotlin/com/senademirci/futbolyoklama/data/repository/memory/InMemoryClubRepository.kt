package com.senademirci.futbolyoklama.data.repository.memory

import com.senademirci.futbolyoklama.data.model.Club
import com.senademirci.futbolyoklama.data.model.Team
import com.senademirci.futbolyoklama.data.repository.AuthRepository
import com.senademirci.futbolyoklama.data.repository.ClubRepository
import com.senademirci.futbolyoklama.util.turkishSortKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryClubRepository(
    private val store: InMemoryStore,
    private val authRepository: AuthRepository,
) : ClubRepository {

    override fun observeClubs(): Flow<List<Club>> =
        combine(store.clubs, authRepository.currentCoach) { clubs, coach ->
            if (coach == null) emptyList()
            else clubs.filter { it.ownerUid == coach.uid }.sortedBy { turkishSortKey(it.name) }
        }

    override fun observeClub(clubId: String): Flow<Club?> =
        store.clubs.map { list -> list.firstOrNull { it.id == clubId } }

    override fun observeTeams(clubId: String): Flow<List<Team>> =
        store.teams.map { list ->
            list.filter { it.clubId == clubId }.sortedBy { it.ageGroupOrder() }
        }

    override fun observeTeam(teamId: String): Flow<Team?> =
        store.teams.map { list -> list.firstOrNull { it.id == teamId } }

    override fun observeAllTeams(): Flow<List<Team>> =
        combine(store.teams, authRepository.currentCoach) { teams, coach ->
            if (coach == null) emptyList() else teams.filter { it.ownerUid == coach.uid }
        }

    override suspend fun upsertClub(club: Club): Result<String> {
        val id = club.id.ifBlank { store.newId("club") }
        store.clubs.update { list ->
            if (list.any { it.id == id }) list.map { if (it.id == id) club.copy(id = id) else it }
            else list + club.copy(id = id)
        }
        return Result.success(id)
    }

    override suspend fun upsertTeam(team: Team): Result<String> {
        val id = team.id.ifBlank { store.newId("team") }
        store.teams.update { list ->
            if (list.any { it.id == id }) list.map { if (it.id == id) team.copy(id = id) else it }
            else list + team.copy(id = id)
        }
        return Result.success(id)
    }

    override suspend fun updateMonthlyDues(teamId: String, amount: Int): Result<Unit> {
        store.teams.update { list ->
            list.map { if (it.id == teamId) it.copy(monthlyDuesAmount = amount) else it }
        }
        return Result.success(Unit)
    }
}

/** "U8" < "U9" < "U10" olacak şekilde sayısal sıralama; metin sıralaması U10'u U8'in önüne atardı. */
private fun Team.ageGroupOrder(): Int =
    name.removePrefix("U").toIntOrNull() ?: Int.MAX_VALUE
