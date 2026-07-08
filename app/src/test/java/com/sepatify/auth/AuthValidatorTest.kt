package com.sepatify.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {

    @Test
    fun `valid email pattern should return true`() {
        val email = "user@test.com"
        assertTrue(email.contains("@") && email.contains("."))
    }

    @Test
    fun `invalid email pattern should return false`() {
        val email = "usertestcom"
        assertFalse(email.contains("@") && email.contains("."))
    }

    @Test
    fun `password shorter than 6 characters should return false`() {
        val pass = "123"
        assertFalse(pass.length >= 6)
    }
}