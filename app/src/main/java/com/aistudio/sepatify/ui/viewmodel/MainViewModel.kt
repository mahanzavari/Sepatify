package com.aistudio.sepatify.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.sepatify.data.local.PreferencesManager
import com.aistudio.sepatify.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val preferencesManager: PreferencesManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentTheme: StateFlow<String> = preferencesManager.themeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val currentLanguage: StateFlow<String> = preferencesManager.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val fontSizeScale: StateFlow<Float> = preferencesManager.fontSizeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val isPremium: StateFlow<Boolean> = preferencesManager.premiumFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userEmail: StateFlow<String?> = preferencesManager.emailFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userDisplayName: StateFlow<String> = preferencesManager.displayNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Guest User")

    val userAvatar: StateFlow<String> = preferencesManager.avatarFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            preferencesManager.setTheme(theme)
        }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch {
            preferencesManager.setLanguage(lang)
        }
    }

    fun updateFontSize(scale: Float) {
        viewModelScope.launch {
            preferencesManager.setFontSize(scale)
        }
    }

    fun setPremium(isPremium: Boolean, syncRemote: Boolean = true) {
        viewModelScope.launch {
            preferencesManager.setPremiumStatus(isPremium)
            if (syncRemote) {
                runCatching { authRepository.setPremium(isPremium) }
            }
        }
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            preferencesManager.updateDisplayName(name)
            runCatching { authRepository.updateDisplayName(name) }
        }
    }

    fun updateAvatar(avatar: String) {
        viewModelScope.launch {
            preferencesManager.updateProfileAvatar(avatar)
            runCatching { authRepository.updateAvatarUrl(avatar) }
        }
    }

    fun updateProfileAvatar(avatar: String) = updateAvatar(avatar)

    fun logout() {
        viewModelScope.launch {
            runCatching { authRepository.signOut() }
            preferencesManager.clearSession()
        }
    }

    fun setUserSession(email: String, name: String) {
        viewModelScope.launch {
            preferencesManager.setUserSession(email, name)
        }
    }
}
