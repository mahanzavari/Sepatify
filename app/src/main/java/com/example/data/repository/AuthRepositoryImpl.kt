package com.example.data.repository

import com.example.data.remote.Supa
import com.example.data.remote.dto.ProfileDto
import com.example.data.remote.dto.ProfileUpdateDto
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class AuthRepositoryImpl : AuthRepository {

    private val auth get() = Supa.client.auth

    override val isInitializing: Flow<Boolean> =
        auth.sessionStatus.map { it is SessionStatus.Initializing }

    override fun currentUserId(): String? = auth.currentUserOrNull()?.id

    override suspend fun hasValidSession(): Boolean {
        return try {
            auth.awaitInitialization()
            auth.currentUserOrNull() != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Result<ProfileDto> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("display_name", JsonPrimitive(displayName))
                    put("username", JsonPrimitive(displayName.lowercase().replace(" ", "_")))
                }
            }
            // Some Supabase projects require e-mail confirmation before a session exists.
            if (auth.currentUserOrNull() == null) {
                return Result.failure(Exception("Account created! Please confirm your e-mail, then sign in."))
            }
            ensureProfileExists(displayName)
            val profile = fetchProfile() ?: return Result.failure(Exception("Could not load profile after sign up."))
            Result.success(profile)
        } catch (e: RestException) {
            Result.failure(Exception(e.error.ifBlank { e.message ?: "Sign up failed" }))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<ProfileDto> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val profile = fetchProfile() ?: run {
                ensureProfileExists(email.substringBefore("@"))
                fetchProfile()
            }
            if (profile == null) {
                Result.failure(Exception("Signed in, but no profile was found."))
            } else {
                Result.success(profile)
            }
        } catch (e: RestException) {
            Result.failure(Exception(e.error.ifBlank { e.message ?: "Sign in failed" }))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Invalid email or password"))
        }
    }

    override suspend fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            // Already signed out / offline - ignore.
        }
    }

    override suspend fun currentProfile(): ProfileDto? = fetchProfile()

    override suspend fun updateDisplayName(name: String) {
        val uid = currentUserId() ?: return
        Supa.client.from("profiles").update(ProfileUpdateDto(displayName = name)) {
            filter { eq("id", uid) }
        }
    }

    override suspend fun updateAvatarUrl(url: String) {
        val uid = currentUserId() ?: return
        Supa.client.from("profiles").update(ProfileUpdateDto(avatarUrl = url)) {
            filter { eq("id", uid) }
        }
    }

    override suspend fun setPremium(isPremium: Boolean) {
        val uid = currentUserId() ?: return
        Supa.client.from("profiles").update(ProfileUpdateDto(isPremium = isPremium)) {
            filter { eq("id", uid) }
        }
    }

    override suspend fun uploadAvatar(bytes: ByteArray, fileExtension: String): Result<String> {
        val uid = currentUserId() ?: return Result.failure(Exception("Not signed in"))
        return try {
            val path = "$uid/avatar.$fileExtension"
            Supa.client.storage.from("avatars").upload(path, bytes) {
                upsert = true
            }
            val publicUrl = Supa.client.storage.from("avatars").publicUrl(path)
            updateAvatarUrl(publicUrl)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchProfile(): ProfileDto? {
        val uid = currentUserId() ?: return null
        return try {
            Supa.client.from("profiles")
                .select(columns = Columns.ALL) { filter { eq("id", uid) } }
                .decodeSingleOrNull<ProfileDto>()
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun ensureProfileExists(fallbackName: String) {
        if (fetchProfile() != null) return
        val uid = currentUserId() ?: return
        val email = auth.currentUserOrNull()?.email ?: "user"
        try {
            Supa.client.from("profiles").insert(
                mapOf(
                    "id" to uid,
                    "username" to (fallbackName.lowercase().replace(" ", "_") + "_" + uid.take(4)),
                    "display_name" to fallbackName.ifBlank { email.substringBefore("@") }
                )
            )
        } catch (e: Exception) {
            // Row may already exist thanks to the DB trigger - safe to ignore.
        }
    }
}
