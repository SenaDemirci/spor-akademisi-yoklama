package com.senademirci.futbolyoklama.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.senademirci.futbolyoklama.data.model.TrainingSession
import com.senademirci.futbolyoklama.data.repository.AttendanceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionHistoryUiState(
    val sessions: List<TrainingSession> = emptyList(),
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SessionHistoryViewModel(
    private val attendanceRepository: AttendanceRepository,
) : ViewModel() {

    private val teamId = MutableStateFlow<String?>(null)

    val state: StateFlow<SessionHistoryUiState> = teamId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else attendanceRepository.observeSessions(id)
        }
        .map { SessionHistoryUiState(sessions = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionHistoryUiState())

    fun load(id: String) {
        teamId.value = id
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch { attendanceRepository.deleteSession(sessionId) }
    }
}
