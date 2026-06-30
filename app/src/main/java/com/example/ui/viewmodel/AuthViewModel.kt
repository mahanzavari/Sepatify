package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val email: String, val displayName: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

sealed interface AuthEffect {
    data class ShowSnackbar(val message: String) : AuthEffect
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val mainViewModel: MainViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        // If a Supabase session already exists on disk (previous app launch), restore it
        // silently so the user isn't sent back to the login screen unnecessarily.
        viewModelScope.launch {
            if (authRepository.hasValidSession()) {
                val profile = authRepository.currentProfile()
                if (profile != null) {
                    mainViewModel.setUserSession(profile.username, profile.displayName)
                    mainViewModel.setPremium(profile.isPremium, syncRemote = false)
                } else {
                    mainViewModel.logout()
                }
            } else {
                mainViewModel.logout()
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and Password cannot be empty")
            viewModelScope.launch { _effects.send(AuthEffect.ShowSnackbar("Email and Password cannot be empty")) }
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signIn(email.trim(), password)
                .onSuccess { profile ->
                    mainViewModel.setUserSession(email.trim(), profile.displayName)
                    mainViewModel.setPremium(profile.isPremium, syncRemote = false)
                    _uiState.value = AuthUiState.Success(email.trim(), profile.displayName)
                }
                .onFailure { error ->
                    val message = error.message ?: "Sign in failed"
                    _uiState.value = AuthUiState.Error(message)
                    viewModelScope.launch { _effects.send(AuthEffect.ShowSnackbar(message)) }
                }
        }
    }

    fun register(email: String, name: String, password: String) {
        if (email.isBlank() || name.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("All fields are required")
            viewModelScope.launch { _effects.send(AuthEffect.ShowSnackbar("All fields are required")) }
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
            viewModelScope.launch { _effects.send(AuthEffect.ShowSnackbar("Password must be at least 6 characters")) }
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signUp(email.trim(), password, name.trim())
                .onSuccess { profile ->
                    mainViewModel.setUserSession(email.trim(), profile.displayName)
                    mainViewModel.setPremium(profile.isPremium, syncRemote = false)
                    _uiState.value = AuthUiState.Success(email.trim(), profile.displayName)
                }
                .onFailure { error ->
                    val message = error.message ?: "Sign up failed"
                    _uiState.value = AuthUiState.Error(message)
                    viewModelScope.launch { _effects.send(AuthEffect.ShowSnackbar(message)) }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            mainViewModel.logout()
            _uiState.value = AuthUiState.Idle
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
