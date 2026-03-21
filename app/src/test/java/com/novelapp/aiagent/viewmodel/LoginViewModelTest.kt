package com.novelapp.aiagent.viewmodel

import org.junit.Assert.*
import org.junit.Test

/**
 * LoginViewModel单元测试
 */
class LoginViewModelTest {

    // ==================== LoginUiState测试 ====================

    @Test
    fun `LoginUiState Idle isLoading is false`() {
        val state = LoginUiState.Idle

        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isError)
        assertFalse(state.isAlreadyLoggedIn)
    }

    @Test
    fun `LoginUiState Loading isLoading is true`() {
        val state = LoginUiState.Loading

        assertTrue(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isError)
    }

    @Test
    fun `LoginUiState Success isSuccess is true`() {
        val state = LoginUiState.Success(
            com.novelapp.aiagent.ai.config.ModelConfig(
                modelType = com.novelapp.aiagent.ai.config.ModelType.GLM_CODEPLAN,
                apiKey = "test-key",
                baseUrl = "https://api.example.com",
                modelName = "glm-4-plus"
            )
        )

        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
        assertFalse(state.isError)
    }

    @Test
    fun `LoginUiState Error isError is true`() {
        val state = LoginUiState.Error("登录失败")

        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertTrue(state.isError)
    }

    @Test
    fun `LoginUiState AlreadyLoggedIn isAlreadyLoggedIn is true`() {
        val state = LoginUiState.AlreadyLoggedIn(
            com.novelapp.aiagent.ai.config.ModelConfig(
                modelType = com.novelapp.aiagent.ai.config.ModelType.GLM_CODEPLAN,
                apiKey = "test-key",
                baseUrl = "https://api.example.com",
                modelName = "glm-4-plus"
            )
        )

        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isError)
        assertTrue(state.isAlreadyLoggedIn)
    }

    // ==================== FormValidation测试 ====================

    @Test
    fun `FormValidation Valid isValid is true`() {
        val validation = FormValidation.Valid

        assertTrue(validation.isValid)
        assertFalse(validation.isInvalid)
    }

    @Test
    fun `FormValidation Invalid isInvalid is true`() {
        val validation = FormValidation.Invalid(listOf("错误1", "错误2"))

        assertFalse(validation.isValid)
        assertTrue(validation.isInvalid)
        assertEquals("错误1", validation.firstError)
    }

    @Test
    fun `FormValidation Invalid firstError returns first message`() {
        val validation = FormValidation.Invalid(listOf("错误A", "错误B"))

        assertEquals("错误A", validation.firstError)
    }

    @Test
    fun `FormValidation Invalid empty list returns default message`() {
        val validation = FormValidation.Invalid(emptyList())

        assertEquals("验证失败", validation.firstError)
    }
}
