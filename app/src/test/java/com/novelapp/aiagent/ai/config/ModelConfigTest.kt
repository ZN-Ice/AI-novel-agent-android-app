package com.novelapp.aiagent.ai.config

import org.junit.Assert.*
import org.junit.Test

/**
 * ModelConfig单元测试
 */
class ModelConfigTest {

    // ==================== ModelConfig测试 ====================

    @Test
    fun `ModelConfig isValid returns true for valid config`() {
        val config = ModelConfig(
            modelType = ModelType.GLM_CODEPLAN,
            apiKey = "test-api-key-12345",
            baseUrl = "https://api.example.com",
            modelName = "glm-4-plus"
        )

        assertTrue(config.isValid())
    }

    @Test
    fun `ModelConfig isValid returns false for empty apiKey`() {
        val config = ModelConfig(
            modelType = ModelType.GLM_CODEPLAN,
            apiKey = "",
            baseUrl = "https://api.example.com",
            modelName = "glm-4-plus"
        )

        assertFalse(config.isValid())
    }

    @Test
    fun `ModelConfig isValid returns false for empty baseUrl`() {
        val config = ModelConfig(
            modelType = ModelType.GLM_CODEPLAN,
            apiKey = "test-key",
            baseUrl = "",
            modelName = "glm-4-plus"
        )

        assertFalse(config.isValid())
    }

    @Test
    fun `ModelConfig isValid returns false for zero maxTokens`() {
        val config = ModelConfig(
            modelType = ModelType.GLM_CODEPLAN,
            apiKey = "test-key",
            baseUrl = "https://api.example.com",
            modelName = "glm-4-plus",
            maxTokens = 0
        )

        assertFalse(config.isValid())
    }

    @Test
    fun `ModelConfig buildApiUrl constructs correct URL`() {
        val config = ModelConfig(
            modelType = ModelType.GLM_CODEPLAN,
            apiKey = "test-key",
            baseUrl = "https://api.example.com/v1",
            modelName = "glm-4-plus"
        )

        val url = config.buildApiUrl("chat/completions")

        assertEquals("https://api.example.com/v1/chat/completions", url)
    }

    @Test
    fun `ModelConfig buildApiUrl handles trailing slash`() {
        val config = ModelConfig(
            modelType = ModelType.GLM_CODEPLAN,
            apiKey = "test-key",
            baseUrl = "https://api.example.com/v1/",
            modelName = "glm-4-plus"
        )

        val url = config.buildApiUrl("/chat/completions")

        assertEquals("https://api.example.com/v1/chat/completions", url)
    }

    @Test
    fun `ModelConfig getDisplayName returns model type display name`() {
        val config = ModelConfig(
            modelType = ModelType.GLM_CODEPLAN,
            apiKey = "test-key",
            baseUrl = "https://api.example.com",
            modelName = "glm-4-plus"
        )

        assertEquals("GLM CodePlan", config.getDisplayName())
    }

    // ==================== ModelType测试 ====================

    @Test
    fun `ModelType fromId returns correct type`() {
        assertEquals(ModelType.GLM_CODEPLAN, ModelType.fromId("glm_codeplan"))
    }

    @Test
    fun `ModelType fromId returns null for unknown id`() {
        assertNull(ModelType.fromId("unknown_model"))
    }

    @Test
    fun `ModelType getSupportedModels returns list`() {
        val models = ModelType.getSupportedModels()

        assertTrue(models.isNotEmpty())
        assertTrue(models.any { it.id == "glm_codeplan" })
    }

    @Test
    fun `GLM_CODEPLAN has correct default values`() {
        assertEquals("glm_codeplan", ModelType.GLM_CODEPLAN.id)
        assertEquals("GLM CodePlan", ModelType.GLM_CODEPLAN.displayName)
        assertEquals("https://open.bigmodel.cn/api/paas/v4", ModelType.GLM_CODEPLAN.defaultBaseUrl)
        assertEquals("glm-4-plus", ModelType.GLM_CODEPLAN.defaultModelName)
    }

    @Test
    fun `GLM_CODEPLAN has expected features`() {
        val features = ModelType.GLM_CODEPLAN.features

        assertTrue(features.contains(ModelFeature.LONG_CONTEXT))
        assertTrue(features.contains(ModelFeature.STREAMING))
        assertTrue(features.contains(ModelFeature.FUNCTION_CALLING))
    }

    // ==================== LoginConfig测试 ====================

    @Test
    fun `LoginConfig validate returns success for valid config`() {
        val loginConfig = LoginConfig(
            modelType = ModelType.GLM_CODEPLAN,
            apiKey = "valid-api-key-12345"
        )

        val result = loginConfig.validate()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `LoginConfig validate fails for empty apiKey`() {
        val loginConfig = LoginConfig(
            modelType = ModelType.GLM_CODEPLAN,
            apiKey = ""
        )

        val result = loginConfig.validate()

        assertTrue(result.isError)
        assertTrue(result.firstError?.contains("API密钥") == true)
    }

    @Test
    fun `LoginConfig validate fails for short apiKey`() {
        val loginConfig = LoginConfig(
            modelType = ModelType.GLM_CODEPLAN,
            apiKey = "short"
        )

        val result = loginConfig.validate()

        assertTrue(result.isError)
    }

    @Test
    fun `LoginConfig validate fails for non-https custom URL`() {
        val loginConfig = LoginConfig(
            modelType = ModelType.GLM_CODEPLAN,
            apiKey = "valid-api-key",
            customBaseUrl = "http://insecure.com"
        )

        val result = loginConfig.validate()

        assertTrue(result.isError)
        assertTrue(result.firstError?.contains("HTTPS") == true)
    }

    @Test
    fun `LoginConfig validate accepts https custom URL`() {
        val loginConfig = LoginConfig(
            modelType = ModelType.GLM_CODEPLAN,
            apiKey = "valid-api-key",
            customBaseUrl = "https://secure.example.com"
        )

        val result = loginConfig.validate()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `LoginConfig toModelConfig uses default values when custom not provided`() {
        val loginConfig = LoginConfig(
            modelType = ModelType.GLM_CODEPLAN,
            apiKey = "test-key"
        )

        val modelConfig = loginConfig.toModelConfig()

        assertEquals(ModelType.GLM_CODEPLAN, modelConfig.modelType)
        assertEquals("test-key", modelConfig.apiKey)
        assertEquals(ModelType.GLM_CODEPLAN.defaultBaseUrl, modelConfig.baseUrl)
        assertEquals(ModelType.GLM_CODEPLAN.defaultModelName, modelConfig.modelName)
    }

    @Test
    fun `LoginConfig toModelConfig uses custom values when provided`() {
        val loginConfig = LoginConfig(
            modelType = ModelType.GLM_CODEPLAN,
            apiKey = "test-key",
            customBaseUrl = "https://custom.api.com",
            customModelName = "custom-model"
        )

        val modelConfig = loginConfig.toModelConfig()

        assertEquals("https://custom.api.com", modelConfig.baseUrl)
        assertEquals("custom-model", modelConfig.modelName)
    }

    // ==================== ModelFeature测试 ====================

    @Test
    fun `ModelFeature display names are correct`() {
        assertEquals("长文本支持", ModelFeature.LONG_CONTEXT.displayName)
        assertEquals("流式输出", ModelFeature.STREAMING.displayName)
        assertEquals("函数调用", ModelFeature.FUNCTION_CALLING.displayName)
        assertEquals("图像理解", ModelFeature.VISION.displayName)
        assertEquals("代码生成", ModelFeature.CODE_GENERATION.displayName)
    }

    // ==================== ValidationResult测试 ====================

    @Test
    fun `ValidationResult Success isSuccess is true`() {
        val result = ValidationResult.Success

        assertTrue(result.isSuccess)
        assertFalse(result.isError)
    }

    @Test
    fun `ValidationResult Error isError is true`() {
        val result = ValidationResult.Error(listOf("error1", "error2"))

        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertEquals("error1", result.firstError)
    }
}
