package com.senademirci.futbolyoklama.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.senademirci.futbolyoklama.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val info: String? = null,
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !isSubmitting
}

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun signIn() {
        if (!_state.value.canSubmit) return
        _state.update { it.copy(isSubmitting = true, error = null, info = null) }
        viewModelScope.launch {
            val result = authRepository.signIn(_state.value.email, _state.value.password)
            _state.update {
                it.copy(
                    isSubmitting = false,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun sendPasswordReset() {
        val email = _state.value.email.trim()
        if (email.isBlank()) {
            _state.update { it.copy(error = "Önce e-posta adresini yaz.") }
            return
        }
        viewModelScope.launch {
            authRepository.sendPasswordReset(email)
            _state.update {
                it.copy(info = "Şifre sıfırlama bağlantısı $email adresine gönderildi.", error = null)
            }
        }
    }

    fun dismissMessages() = _state.update { it.copy(error = null, info = null) }
}
