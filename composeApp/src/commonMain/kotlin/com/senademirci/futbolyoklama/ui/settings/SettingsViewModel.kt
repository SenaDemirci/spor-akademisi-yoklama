package com.senademirci.futbolyoklama.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.senademirci.futbolyoklama.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val coachName: String = "",
    val email: String = "",
    val teamName: String = "",
)

class SettingsViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        authRepository.currentCoach,
        authRepository.observeTeam(),
    ) { coach, team ->
        SettingsUiState(
            coachName = coach?.name.orEmpty(),
            email = coach?.email.orEmpty(),
            teamName = team?.name.orEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun updateTeamName(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { authRepository.updateTeamName(name) }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
