package com.senademirci.futbolyoklama.ui.roster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.senademirci.futbolyoklama.data.model.AttendanceRecord
import com.senademirci.futbolyoklama.data.model.Player
import com.senademirci.futbolyoklama.data.repository.AttendanceRepository
import com.senademirci.futbolyoklama.data.repository.PlayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class PlayerDetailUiState(
    val player: Player? = null,
    val recentRecords: List<AttendanceRecord> = emptyList(),
    val isLoading: Boolean = true,
)

class PlayerDetailViewModel(
    private val playerRepository: PlayerRepository,
    private val attendanceRepository: AttendanceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerDetailUiState())
    val state: StateFlow<PlayerDetailUiState> = _state.asStateFlow()

    private var loaded = false

    fun load(playerId: String) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            combine(
                playerRepository.observePlayer(playerId),
                attendanceRepository.observeRecordsForPlayer(playerId, limit = 10),
            ) { player, records ->
                PlayerDetailUiState(player = player, recentRecords = records, isLoading = false)
            }.collect { _state.value = it }
        }
    }

    fun archive() {
        val id = _state.value.player?.id ?: return
        viewModelScope.launch { playerRepository.archive(id) }
    }

    fun restore() {
        val id = _state.value.player?.id ?: return
        viewModelScope.launch { playerRepository.restore(id) }
    }

    fun deletePermanently(onDone: () -> Unit) {
        val id = _state.value.player?.id ?: return
        viewModelScope.launch {
            playerRepository.deletePermanently(id)
            onDone()
        }
    }
}
