package com.senademirci.futbolyoklama.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.senademirci.futbolyoklama.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val coachName: String = "",
    val email: String = "",
)

class SettingsViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val state: StateFlow<SettingsUiState> = authRepository.currentCoach
        .map { SettingsUiState(coachName = it?.name.orEmpty(), email = it?.email.orEmpty()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
