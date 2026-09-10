package com.senademirci.futbolyoklama.ui.club

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.senademirci.futbolyoklama.data.model.Club
import com.senademirci.futbolyoklama.data.model.Team
import com.senademirci.futbolyoklama.data.repository.ClubRepository
import com.senademirci.futbolyoklama.data.repository.PlayerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class TeamSummary(
    val team: Team,
    val playerCount: Int,
)

data class ClubDetailUiState(
    val club: Club? = null,
    val teams: List<TeamSummary> = emptyList(),
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ClubDetailViewModel(
    private val clubRepository: ClubRepository,
    private val playerRepository: PlayerRepository,
) : ViewModel() {

    private val clubId = MutableStateFlow<String?>(null)

    val state: StateFlow<ClubDetailUiState> = clubId
        .flatMapLatest { id ->
            if (id == null) flowOf(ClubDetailUiState())
            else combine(
                clubRepository.observeClub(id),
                clubRepository.observeTeams(id),
                playerRepository.observeAllForOwner(),
            ) { club, teams, players ->
                ClubDetailUiState(
                    club = club,
                    teams = teams.map { team ->
                        TeamSummary(
                            team = team,
                            playerCount = players.count { it.teamId == team.id && it.isActive },
                        )
                    },
                    isLoading = false,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClubDetailUiState())

    fun load(id: String) {
        clubId.value = id
    }
}
