package com.senademirci.futbolyoklama.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.senademirci.futbolyoklama.data.model.AttendanceStatus
import com.senademirci.futbolyoklama.data.model.Player
import com.senademirci.futbolyoklama.data.repository.AttendanceRepository
import com.senademirci.futbolyoklama.data.repository.PlayerRepository
import com.senademirci.futbolyoklama.util.DateUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TakeAttendanceUiState(
    val sessionId: String? = null,
    val date: String = DateUtil.today(),
    val note: String = "",
    val players: List<Player> = emptyList(),
    val statuses: Map<String, AttendanceStatus> = emptyMap(),
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    /** Aynı tarihte zaten bir antrenman var — kullanıcıya sorulur. */
    val existingSessionIdForDate: String? = null,
) {
    val presentCount: Int get() = statuses.values.count { it == AttendanceStatus.PRESENT }
    val absentCount: Int get() = statuses.values.count { it == AttendanceStatus.ABSENT }
    val excusedCount: Int get() = statuses.values.count { it == AttendanceStatus.EXCUSED }
    val lateCount: Int get() = statuses.values.count { it == AttendanceStatus.LATE }

    val isEditing: Boolean get() = sessionId != null
    val canSubmit: Boolean get() = players.isNotEmpty() && !isSubmitting
}

class TakeAttendanceViewModel(
    private val playerRepository: PlayerRepository,
    private val attendanceRepository: AttendanceRepository,
) : ViewModel() {

    private var teamId: String = ""


    private val _state = MutableStateFlow(TakeAttendanceUiState())
    val state: StateFlow<TakeAttendanceUiState> = _state.asStateFlow()

    private var loaded = false

    fun load(teamId: String, sessionId: String?) {
        if (loaded) return
        loaded = true
        this.teamId = teamId

        viewModelScope.launch {
            val players = playerRepository.observeRoster(teamId).first()

            if (sessionId == null) {
                // Yeni yoklama: herkes varsayılan olarak "Var". Koç yalnızca gelmeyenlere dokunur.
                _state.update {
                    it.copy(
                        players = players,
                        statuses = players.associate { p -> p.id to AttendanceStatus.PRESENT },
                        isLoading = false,
                    )
                }
                checkExistingSession(_state.value.date)
            } else {
                val session = attendanceRepository.observeSession(sessionId).first()
                val records = attendanceRepository.observeRecordsForSession(sessionId).first()
                // Kayıttan sonra kadroya eklenen öğrenciler "Var" olarak gelir
                val saved = records.associate { r -> r.playerId to r.status }
                _state.update {
                    it.copy(
                        sessionId = sessionId,
                        date = session?.date ?: it.date,
                        note = session?.note.orEmpty(),
                        players = players,
                        statuses = players.associate { p ->
                            p.id to (saved[p.id] ?: AttendanceStatus.PRESENT)
                        },
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun setStatus(playerId: String, status: AttendanceStatus) {
        _state.update { it.copy(statuses = it.statuses + (playerId to status)) }
    }

    fun markAllPresent() {
        _state.update { s ->
            s.copy(statuses = s.players.associate { it.id to AttendanceStatus.PRESENT })
        }
    }

    fun onNoteChange(value: String) = _state.update { it.copy(note = value) }

    fun onDateChange(value: String) {
        if (!DateUtil.isValidIsoDate(value)) return
        _state.update { it.copy(date = value) }
        if (!_state.value.isEditing) checkExistingSession(value)
    }

    private fun checkExistingSession(date: String) {
        viewModelScope.launch {
            val existing = attendanceRepository.findSessionByDate(teamId, date)
            _state.update { it.copy(existingSessionIdForDate = existing?.id) }
        }
    }

    /** Aynı güne ait mevcut kaydın üzerine yazmayı seçtiğinde çağrılır. */
    fun switchToExistingSession() {
        val existingId = _state.value.existingSessionIdForDate ?: return
        loaded = false
        _state.value = TakeAttendanceUiState(date = _state.value.date)
        load(teamId, existingId)
    }

    fun dismissExistingSessionWarning() =
        _state.update { it.copy(existingSessionIdForDate = null) }

    fun save() {
        val s = _state.value
        if (!s.canSubmit) return

        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = attendanceRepository.saveAttendance(
                sessionId = s.sessionId,
                teamId = teamId,
                date = s.date,
                note = s.note.trim(),
                statuses = s.statuses,
                playerNames = s.players.associate { it.id to it.fullName },
            )
            _state.update {
                it.copy(
                    isSubmitting = false,
                    isSaved = result.isSuccess,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }
}
