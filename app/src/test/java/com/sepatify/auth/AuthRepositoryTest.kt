package com.sepatify.auth

import com.aistudio.sepatify.data.repository.AuthRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class AuthRepositoryTest {

    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        authRepository = mock(AuthRepository::class.java)
    }

    @Test
    fun `successful signin should return success outcome`() = runBlocking {
        val expected = Result.success(Unit)
        `when`(authRepository.signIn("user@test.com", "password")).thenReturn(expected)

        val result = authRepository.signIn("user@test.com", "password")
        assertEquals(expected, result)
    }
}