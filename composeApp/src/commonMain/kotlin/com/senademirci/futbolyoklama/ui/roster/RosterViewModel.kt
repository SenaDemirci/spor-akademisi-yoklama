package com.senademirci.futbolyoklama.ui.roster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.senademirci.futbolyoklama.data.model.Player
import com.senademirci.futbolyoklama.data.repository.AuthRepository
import com.senademirci.futbolyoklama.data.repository.PlayerRepository
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
    val teamName: String = "",
    val coachName: String = "",
    val players: List<Player> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = !isLoading && players.isEmpty() && query.isBlank()
    val noSearchResults: Boolean get() = !isLoading && players.isEmpty() && query.isNotBlank()
}

@OptIn(ExperimentalCoroutinesApi::class)
class RosterViewModel(
    private val authRepository: AuthRepository,
    private val playerRepository: PlayerRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val roster = authRepository.currentCoach
        .flatMapLatest { coach ->
            if (coach == null) flowOf(emptyList())
            else playerRepository.observeRoster(coach.teamId)
        }

    val state: StateFlow<RosterUiState> = combine(
        roster,
        query,
        authRepository.observeTeam(),
        authRepository.currentCoach,
    ) { players, q, team, coach ->
        RosterUiState(
            teamName = team?.name.orEmpty(),
            coachName = coach?.name.orEmpty(),
            players = players.filter { it.matches(q) },
            query = q,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RosterUiState())

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
    val q = query.trim().lowercase()
    return fullName.lowercase().contains(q) || jerseyNumber?.toString() == q
}
