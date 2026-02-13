package br.com.devcapu.remy.presentation.auth.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.devcapu.remy.infra.auth.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoginSuccess: Boolean = false
)

class LoginViewModel : ViewModel() {
    private val authService = AuthService()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        val emailError = validateEmail(email)
        _uiState.value = _uiState.value.copy(email = email, emailError = emailError)
    }

    fun onPasswordChange(password: String) {
        val passwordError = validatePassword(password)
        _uiState.value = _uiState.value.copy(password = password, passwordError = passwordError)
    }

    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> null
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email inválido"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> null
            password.length < 6 -> "Senha deve ter pelo menos 6 caracteres"
            else -> null
        }
    }

    private fun isFormValid(): Boolean {
        val currentState = _uiState.value
        return currentState.emailError == null &&
               currentState.passwordError == null &&
               currentState.email.isNotBlank() &&
               currentState.password.isNotBlank()
    }

    fun login() {
        if (!isFormValid()) {
            _uiState.value = _uiState.value.copy(
                emailError = validateEmail(_uiState.value.email)
                    ?: if (_uiState.value.email.isBlank()) "Email é obrigatório" else null,
                passwordError = validatePassword(_uiState.value.password)
                    ?: if (_uiState.value.password.isBlank()) "Senha é obrigatória" else null
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = authService.signIn(
                email = _uiState.value.email,
                password = _uiState.value.password
            )

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoginSuccess = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Erro ao fazer login"
                )
            }
        }
    }
}
