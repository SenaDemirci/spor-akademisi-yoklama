package com.senademirci.futbolyoklama.ui.roster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.senademirci.futbolyoklama.data.model.Club
import com.senademirci.futbolyoklama.data.model.Player
import com.senademirci.futbolyoklama.data.model.Team
import com.senademirci.futbolyoklama.data.repository.ClubRepository
import com.senademirci.futbolyoklama.data.repository.PlayerRepository
import com.senademirci.futbolyoklama.util.turkishLowercase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RosterUiState(
    val team: Team? = null,
    val club: Club? = null,
    val players: List<Player> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
) {
    val title: String get() = team?.name.orEmpty()
    val subtitle: String get() = club?.name.orEmpty()
    val isEmpty: Boolean get() = !isLoading && players.isEmpty() && query.isBlank()
    val noSearchResults: Boolean get() = !isLoading && players.isEmpty() && query.isNotBlank()
}

@OptIn(ExperimentalCoroutinesApi::class)
class RosterViewModel(
    private val clubRepository: ClubRepository,
    private val playerRepository: PlayerRepository,
) : ViewModel() {

    private val teamId = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow("")

    val state: StateFlow<RosterUiState> = teamId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(RosterUiState())
            } else {
                clubRepository.observeTeam(id).flatMapLatest { team ->
                    combine(
                        playerRepository.observeRoster(id),
                        query,
                        if (team == null) flowOf(null) else clubRepository.observeClub(team.clubId),
                    ) { players, q, club ->
                        RosterUiState(
                            team = team,
                            club = club,
                            players = players.filter { it.matches(q) },
                            query = q,
                            isLoading = false,
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RosterUiState())

    fun load(id: String) {
        teamId.value = id
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun archive(playerId: String) {
        viewModelScope.launch { playerRepository.archive(playerId) }
    }

    fun deletePermanently(playerId: String) {
        viewModelScope.launch { playerRepository.deletePermanently(playerId) }
    }
}

private fun Player.matches(query: String): Boolean {
    if (query.isBlank()) return true
    val q = turkishLowercase(query.trim())
    return turkishLowercase(fullName).contains(q) || jerseyNumber?.toString() == q
}
