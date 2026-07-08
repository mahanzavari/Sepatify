package com.sepatify.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aistudio.sepatify.ui.screens.LoginScreen
import org.junit.Rule
import org.junit.Test

class LoginFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clickToggle_ChangesLoginToRegisterState() {
        composeTestRule.setContent {
            LoginScreen(
                onLoginClick = { _, _ -> },
                onRegisterClick = { _, _, _ -> }
            )
        }

        // Standard test asserting navigation toggle buttons switch state
        composeTestRule.onNodeWithText("Don't have an account? Sign up").performClick()
        composeTestRule.onNodeWithText("Register").assertExists()
    }
}