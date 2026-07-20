package com.aistudio.sepatify.domain.repository

import com.aistudio.sepatify.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import java.io.File

interface AuthRepository {
    val isInitializing: Flow<Boolean>

    fun currentUserId(): String?
    suspend fun hasValidSession(): Boolean
    suspend fun currentProfile(): UserProfile?

    suspend fun signUp(email: String, password: String, displayName: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut(): Result<Unit>

    suspend fun updateDisplayName(name: String): Result<Unit>
    suspend fun updateAvatarUrl(url: String): Result<Unit>
    suspend fun setPremium(isPremium: Boolean): Result<Unit>
    suspend fun uploadAvatar(file: File): Result<String>
}
