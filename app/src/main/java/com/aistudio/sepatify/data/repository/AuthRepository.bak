package com.aistudio.sepatify.data.repository

import com.aistudio.sepatify.data.remote.dto.ProfileDto
import kotlinx.coroutines.flow.Flow
import java.io.File

interface AuthRepository {
    val isInitializing: Flow<Boolean>
    
    fun currentUserId(): String?
    suspend fun hasValidSession(): Boolean
    suspend fun currentProfile(): ProfileDto?

    suspend fun signUp(email: String, password: String, displayName: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
    
    suspend fun updateDisplayName(name: String): Result<Unit>
    suspend fun updateAvatarUrl(url: String): Result<Unit>
    suspend fun setPremium(isPremium: Boolean): Result<Unit>
    suspend fun uploadAvatar(file: File): Result<String>
}

// === COMPATIBILITY COUPLING ===
val AuthRepository.currentUserId: String? get() = currentUserId()

operator fun String?.invoke(): String? = this
operator fun Boolean.invoke(): Boolean = this
operator fun ProfileDto?.invoke(): ProfileDto? = this

suspend fun AuthRepository.updateProfileAvatar(url: String) {
    updateAvatarUrl(url)
}