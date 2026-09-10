package com.senademirci.futbolyoklama.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.senademirci.futbolyoklama.data.model.Club
import com.senademirci.futbolyoklama.data.repository.AuthRepository
import com.senademirci.futbolyoklama.data.repository.ClubRepository
import com.senademirci.futbolyoklama.data.repository.PlayerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Anasayfadaki bir kulüp kartı: kulüp + kaç takımı ve kaç öğrencisi var. */
data class ClubSummary(
    val club: Club,
    val teamCount: Int,
    val playerCount: Int,
)

data class HomeUiState(
    val coachName: String = "",
    val clubs: List<ClubSummary> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = !isLoading && clubs.isEmpty()
}

class HomeViewModel(
    authRepository: AuthRepository,
    clubRepository: ClubRepository,
    playerRepository: PlayerRepository,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        clubRepository.observeClubs(),
        clubRepository.observeAllTeams(),
        playerRepository.observeAllForOwner(),
        authRepository.currentCoach,
    ) { clubs, teams, players, coach ->
        HomeUiState(
            coachName = coach?.name.orEmpty(),
            clubs = clubs.map { club ->
                ClubSummary(
                    club = club,
                    teamCount = teams.count { it.clubId == club.id },
                    playerCount = players.count { it.clubId == club.id && it.isActive },
                )
            },
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
