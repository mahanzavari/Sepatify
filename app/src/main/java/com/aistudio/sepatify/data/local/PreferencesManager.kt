package com.aistudio.sepatify.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        private val THEME_KEY = stringPreferencesKey("app_theme")
        private val LANGUAGE_KEY = stringPreferencesKey("app_language")
        private val FONT_SIZE_KEY = floatPreferencesKey("app_font_size")
        private val PREMIUM_KEY = booleanPreferencesKey("is_premium")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val DISPLAY_NAME_KEY = stringPreferencesKey("display_name")
        private val AVATAR_KEY = stringPreferencesKey("avatar_url")
    }

    val themeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "System"
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: "fa"
    }

    val fontSizeFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[FONT_SIZE_KEY] ?: 1.0f
    }

    val premiumFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PREMIUM_KEY] ?: false
    }

    val emailFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL_KEY] ?: ""
    }

    val displayNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DISPLAY_NAME_KEY] ?: "Guest User"
    }

    val avatarFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[AVATAR_KEY] ?: ""
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences -> preferences[THEME_KEY] = theme }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences -> preferences[LANGUAGE_KEY] = language }
    }

    suspend fun setFontSize(size: Float) {
        context.dataStore.edit { preferences -> preferences[FONT_SIZE_KEY] = size }
    }

    suspend fun setPremiumStatus(isPremium: Boolean) {
        context.dataStore.edit { preferences -> preferences[PREMIUM_KEY] = isPremium }
    }

    suspend fun setUserSession(email: String, displayName: String, avatarUrl: String? = null) {
        context.dataStore.edit { preferences ->
            preferences[USER_EMAIL_KEY] = email
            preferences[DISPLAY_NAME_KEY] = displayName
            avatarUrl?.let { preferences[AVATAR_KEY] = it }
        }
    }

    suspend fun updateDisplayName(displayName: String) {
        context.dataStore.edit { preferences ->
            preferences[DISPLAY_NAME_KEY] = displayName
        }
    }

    suspend fun updateProfileAvatar(avatarUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[AVATAR_KEY] = avatarUrl
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_EMAIL_KEY)
            preferences.remove(DISPLAY_NAME_KEY)
            preferences.remove(AVATAR_KEY)
            preferences[PREMIUM_KEY] = false
        }
    }
}