package com.senademirci.futbolyoklama.ui.roster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.senademirci.futbolyoklama.data.model.Player
import com.senademirci.futbolyoklama.data.repository.ClubRepository
import com.senademirci.futbolyoklama.data.repository.PlayerRepository
import com.senademirci.futbolyoklama.util.DateUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerEditUiState(
    val playerId: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val jerseyNumber: String = "",
    val parentPhone: String = "",
    val isSubmitting: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
) {
    val isEditing: Boolean get() = playerId != null

    val birthDateInvalid: Boolean
        get() = birthDate.isNotBlank() && !DateUtil.isValidIsoDate(birthDate)

    val jerseyInvalid: Boolean
        get() = jerseyNumber.isNotBlank() && jerseyNumber.toIntOrNull() == null

    val canSubmit: Boolean
        get() = firstName.isNotBlank() && lastName.isNotBlank() &&
            !birthDateInvalid && !jerseyInvalid && !isSubmitting
}

class PlayerEditViewModel(
    private val clubRepository: ClubRepository,
    private val playerRepository: PlayerRepository,
) : ViewModel() {

    private var teamId: String = ""


    private val _state = MutableStateFlow(PlayerEditUiState())
    val state: StateFlow<PlayerEditUiState> = _state.asStateFlow()

    private var loaded = false

    fun load(teamId: String, playerId: String?) {
        if (loaded) return
        loaded = true
        this.teamId = teamId
        if (playerId == null) return
        viewModelScope.launch {
            val player = playerRepository.observePlayer(playerId).first() ?: return@launch
            _state.value = PlayerEditUiState(
                playerId = player.id,
                firstName = player.firstName,
                lastName = player.lastName,
                birthDate = player.birthDate.orEmpty(),
                jerseyNumber = player.jerseyNumber?.toString().orEmpty(),
                parentPhone = player.parentPhone.orEmpty(),
            )
        }
    }

    fun onFirstNameChange(v: String) = _state.update { it.copy(firstName = v, error = null) }
    fun onLastNameChange(v: String) = _state.update { it.copy(lastName = v, error = null) }
    fun onBirthDateChange(v: String) = _state.update { it.copy(birthDate = v, error = null) }
    fun onParentPhoneChange(v: String) = _state.update { it.copy(parentPhone = v, error = null) }

    fun onJerseyChange(v: String) {
        if (v.length <= 3 && v.all { it.isDigit() }) {
            _state.update { it.copy(jerseyNumber = v, error = null) }
        }
    }

    fun save() {
        if (!_state.value.canSubmit) return
        _state.update { it.copy(isSubmitting = true, error = null) }

        viewModelScope.launch {
            val team = clubRepository.observeTeam(teamId).first()
            if (team == null) {
                _state.update { it.copy(isSubmitting = false, error = "Takım bulunamadı.") }
                return@launch
            }
            val s = _state.value
            val player = Player(
                id = s.playerId.orEmpty(),
                ownerUid = team.ownerUid,
                clubId = team.clubId,
                teamId = team.id,
                firstName = s.firstName.trim(),
                lastName = s.lastName.trim(),
                birthDate = s.birthDate.trim().takeIf { it.isNotBlank() },
                jerseyNumber = s.jerseyNumber.toIntOrNull(),
                parentPhone = s.parentPhone.trim().takeIf { it.isNotBlank() },
            )
            val result = playerRepository.upsert(player)
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
