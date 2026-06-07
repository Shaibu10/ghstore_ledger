package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.UserEntity
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val user: UserEntity) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

sealed class ForgotPasswordUiState {
    object Idle : ForgotPasswordUiState()
    object Loading : ForgotPasswordUiState()
    object Success : ForgotPasswordUiState()
    data class Error(val message: String) : ForgotPasswordUiState()
}

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _rememberMe = MutableStateFlow(false)
    val rememberMe = _rememberMe.asStateFlow()

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState = _loginState.asStateFlow()

    private val _forgotPasswordState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val forgotPasswordState = _forgotPasswordState.asStateFlow()

    fun onUsernameChanged(value: String) {
        _username.value = value
    }

    fun onPasswordChanged(value: String) {
        _password.value = value
    }

    fun onRememberMeChanged(value: Boolean) {
        _rememberMe.value = value
    }

    fun login() {
        if (_username.value.isBlank() || _password.value.isBlank()) {
            _loginState.value = LoginUiState.Error("Username and password must not be empty.")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            val result = authRepository.login(_username.value, _password.value, _rememberMe.value)
            result.onSuccess { user ->
                _loginState.value = LoginUiState.Success(user)
            }.onFailure { exception ->
                _loginState.value = LoginUiState.Error(exception.message ?: "Authentication failed")
            }
        }
    }

    fun resetPassword() {
        if (_username.value.isBlank()) {
            _forgotPasswordState.value = ForgotPasswordUiState.Error("Please enter your username first")
            return
        }

        viewModelScope.launch {
            _forgotPasswordState.value = ForgotPasswordUiState.Loading
            val result = authRepository.triggerPasswordReset(_username.value)
            result.onSuccess {
                _forgotPasswordState.value = ForgotPasswordUiState.Success
            }.onFailure { exception ->
                _forgotPasswordState.value = ForgotPasswordUiState.Error(exception.message ?: "Reset request failed")
            }
        }
    }

    fun resetStates() {
        _loginState.value = LoginUiState.Idle
        _forgotPasswordState.value = ForgotPasswordUiState.Idle
    }
}

/**
 * Factory class to instantiate models with appropriate dependency injection.
 */
class LoginViewModelFactory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
