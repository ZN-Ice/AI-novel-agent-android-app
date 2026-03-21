package com.novelapp.aiagent.ai.config

import org.junit.Assert.*

/**
 * ModelConfig单元测试
 */
class ModelConfigTest {

    // ==================== ModelConfig测试 ====================

    @org.junit.Test
    fun `ModelConfig isValid returns true for valid config`() {
        val config = ModelConfig(
            providerType = ModelProviderType.GLM_CODEPLAN,
            glmModel = GLMModel.GLM_4_6,
            apiKey = "test-api-key-12345",
            baseUrl = "https://open.bigmodel.cn/api/coding/paas/v4",
            modelName = "GLM-4.6"
        )

        assertTrue(config.isValid())
    }

    @org.junit.Test
    fun `ModelConfig isValid returns false for empty apiKey`() {
        val config = ModelConfig(
            providerType = ModelProviderType.GLM_CODEPLAN,
            glmModel = GLMModel.GLM_4_6,
            apiKey = "",
            baseUrl = "https://open.bigmodel.cn/api/coding/paas/v4",
            modelName = "GLM-4.6"
        )

        assertFalse(config.isValid())
    }

    @org.junit.Test
    fun `ModelConfig isValid returns false for empty baseUrl`() {
        val config = ModelConfig(
            providerType = ModelProviderType.GLM_CODEPLAN,
            glmModel = GLMModel.GLM_4_6,
            apiKey = "test-key",
            baseUrl = "",
            modelName = "GLM-4.6"
        )

        assertFalse(config.isValid())
    }

    @org.junit.Test
    fun `ModelConfig isValid returns false for zero maxTokens`() {
        val config = ModelConfig(
            providerType = ModelProviderType.GLM_CODEPLAN,
            glmModel = GLMModel.GLM_4_6,
            apiKey = "test-key",
            baseUrl = "https://open.bigmodel.cn/api/coding/paas/v4",
            modelName = "GLM-4.6",
            maxTokens = 0
        )

        assertFalse(config.isValid())
    }

    @org.junit.Test
    fun `ModelConfig buildApiUrl constructs correct URL`() {
        val config = ModelConfig(
            providerType = ModelProviderType.GLM_CODEPLAN,
            glmModel = GLMModel.GLM_4_6,
            apiKey = "test-key",
            baseUrl = "https://open.bigmodel.cn/api/coding/paas/v4",
            modelName = "GLM-4.6"
        )

        val url = config.buildApiUrl("chat/completions")

        assertEquals("https://open.bigmodel.cn/api/coding/paas/v4/chat/completions", url)
    }

    @org.junit.Test
    fun `ModelConfig buildApiUrl handles trailing slash`() {
        val config = ModelConfig(
            providerType = ModelProviderType.GLM_CODEPLAN,
            glmModel = GLMModel.GLM_4_6,
            apiKey = "test-key",
            baseUrl = "https://open.bigmodel.cn/api/coding/paas/v4/",
            modelName = "GLM-4.6"
        )

        val url = config.buildApiUrl("/chat/completions")

        assertEquals("https://open.bigmodel.cn/api/coding/paas/v4/chat/completions", url)
    }

    @org.junit.Test
    fun `ModelConfig getDisplayName returns GLM model display name when set`() {
        val config = ModelConfig(
            providerType = ModelProviderType.GLM_CODEPLAN,
            glmModel = GLMModel.GLM_5,
            apiKey = "test-key",
            baseUrl = "https://open.bigmodel.cn/api/coding/paas/v4",
            modelName = "GLM-5"
        )

        assertEquals("GLM 5", config.getDisplayName())
    }

    @org.junit.Test
    fun `ModelConfig getDisplayName returns provider display name when model not set`() {
        val config = ModelConfig(
            providerType = ModelProviderType.GLM_CODEPLAN,
            glmModel = null,
            apiKey = "test-key",
            baseUrl = "https://open.bigmodel.cn/api/coding/paas/v4",
            modelName = "GLM-4.6"
        )

        assertEquals("GLM CodePlan", config.getDisplayName())
    }

    // ==================== ModelProviderType测试 ====================

    @org.junit.Test
    fun `ModelProviderType fromId returns correct type`() {
        assertEquals(ModelProviderType.GLM_CODEPLAN, ModelProviderType.fromId("glm_codeplan"))
    }

    @org.junit.Test
    fun `ModelProviderType fromId returns null for unknown id`() {
        assertNull(ModelProviderType.fromId("unknown_provider"))
    }

    @org.junit.Test
    fun `ModelProviderType getSupportedProviders returns list`() {
        val providers = ModelProviderType.getSupportedProviders()

        assertTrue(providers.isNotEmpty())
        assertTrue(providers.any { it.id == "glm_codeplan" })
    }

    @org.junit.Test
    fun `GLM_CODEPLAN has correct default values`() {
        assertEquals("glm_codeplan", ModelProviderType.GLM_CODEPLAN.id)
        assertEquals("GLM CodePlan", ModelProviderType.GLM_CODEPLAN.displayName)
        assertEquals("https://open.bigmodel.cn/api/coding/paas/v4", ModelProviderType.GLM_CODEPLAN.defaultBaseUrl)
    }

    @org.junit.Test
    fun `GLM_CODEPLAN has supported models`() {
        val models = ModelProviderType.GLM_CODEPLAN.supportedModels

        assertTrue(models.isNotEmpty())
        assertTrue(models.any { it.id == "GLM-5" })
        assertTrue(models.any { it.id == "GLM-4.7" })
        assertTrue(models.any { it.id == "GLM-4.6" })
        assertTrue(models.any { it.id == "GLM-4.5-air" })
    }

    // ==================== GLMModel测试 ====================

    @org.junit.Test
    fun `GLMModel fromModelId returns correct model`() {
        assertEquals(GLMModel.GLM_5, GLMModel.fromModelId("GLM-5"))
        assertEquals(GLMModel.GLM_4_7, GLMModel.fromModelId("GLM-4.7"))
        assertEquals(GLMModel.GLM_4_6, GLMModel.fromModelId("GLM-4.6"))
        assertEquals(GLMModel.GLM_4_5_AIR, GLMModel.fromModelId("GLM-4.5-air"))
    }

    @org.junit.Test
    fun `GLMModel fromModelId returns null for unknown id`() {
        assertNull(GLMModel.fromModelId("unknown-model"))
    }

    @org.junit.Test
    fun `GLMModel getAllModels returns all models`() {
        val models = GLMModel.getAllModels()

        assertEquals(4, models.size)
        assertTrue(models.any { it.id == "GLM-5" })
        assertTrue(models.any { it.id == "GLM-4.7" })
        assertTrue(models.any { it.id == "GLM-4.6" })
        assertTrue(models.any { it.id == "GLM-4.5-air" })
    }

    @org.junit.Test
    fun `GLM_5 has correct values`() {
        assertEquals("GLM-5", GLMModel.GLM_5.modelId)
        assertEquals("GLM 5", GLMModel.GLM_5.displayName)
        assertEquals(8192, GLMModel.GLM_5.maxTokens)
        assertEquals(0.7f, GLMModel.GLM_5.recommendedTemperature)
    }

    @org.junit.Test
    fun `GLM_4_6 has correct values`() {
        assertEquals("GLM-4.6", GLMModel.GLM_4_6.modelId)
        assertEquals("GLM 4.6", GLMModel.GLM_4_6.displayName)
        assertEquals(4096, GLMModel.GLM_4_6.maxTokens)
        assertEquals(0.8f, GLMModel.GLM_4_6.recommendedTemperature)
    }

    // ==================== LoginConfig测试 ====================

    @org.junit.Test
    fun `LoginConfig validate returns success for valid config`() {
        val loginConfig = LoginConfig(
            providerType = ModelProviderType.GLM_CODEPLAN,
            glmModel = GLMModel.GLM_4_6,
            apiKey = "valid-api-key-12345"
        )

        val result = loginConfig.validate()

        assertTrue(result.isSuccess)
    }

    @org.junit.Test
    fun `LoginConfig validate fails for empty apiKey`() {
        val loginConfig = LoginConfig(
            providerType = ModelProviderType.GLM_CODEPLAN,
            glmModel = GLMModel.GLM_4_6,
            apiKey = ""
        )

        val result = loginConfig.validate()

        assertTrue(result.isError)
        assertTrue(result.firstError?.contains("API密钥") == true)
    }

    @org.junit.Test
    fun `LoginConfig validate fails for short apiKey`() {
        val loginConfig = LoginConfig(
            providerType = ModelProviderType.GLM_CODEPLAN,
            glmModel = GLMModel.GLM_4_6,
            apiKey = "short"
        )

        val result = loginConfig.validate()

        assertTrue(result.isError)
    }

    @org.junit.Test
    fun `LoginConfig validate fails for non-https custom URL`() {
        val loginConfig = LoginConfig(
            providerType = ModelProviderType.GLM_CODEPLAN,
            glmModel = GLMModel.GLM_4_6,
            apiKey = "valid-api-key",
            customBaseUrl = "http://insecure.com"
        )

        val result = loginConfig.validate()

        assertTrue(result.isError)
        assertTrue(result.firstError?.contains("HTTPS") == true)
    }

    @org.junit.Test
    fun `LoginConfig validate accepts https custom URL`() {
        val loginConfig = LoginConfig(
            providerType = ModelProviderType.GLM_CODEPLAN,
            glmModel = GLMModel.GLM_4_6,
            apiKey = "valid-api-key",
            customBaseUrl = "https://secure.example.com"
        )

        val result = loginConfig.validate()

        assertTrue(result.isSuccess)
    }

    @org.junit.Test
    fun `LoginConfig validate fails when GLM model not selected for GLM provider`() {
        val loginConfig = LoginConfig(
            providerType = ModelProviderType.GLM_CODEPLAN,
            glmModel = null,
            apiKey = "valid-api-key"
        )

        val result = loginConfig.validate()

        assertTrue(result.isError)
        assertTrue(result.firstError?.contains("模型") == true)
    }

    @org.junit.Test
    fun `LoginConfig toModelConfig uses default values when custom not provided`() {
        val loginConfig = LoginConfig(
            providerType = ModelProviderType.GLM_CODEPLAN,
            glmModel = GLMModel.GLM_4_6,
            apiKey = "test-key"
        )

        val modelConfig = loginConfig.toModelConfig()

        assertEquals(ModelProviderType.GLM_CODEPLAN, modelConfig.providerType)
        assertEquals(GLMModel.GLM_4_6, modelConfig.glmModel)
        assertEquals("test-key", modelConfig.apiKey)
        assertEquals(ModelProviderType.GLM_CODEPLAN.defaultBaseUrl, modelConfig.baseUrl)
        assertEquals(GLMModel.GLM_4_6.modelId, modelConfig.modelName)
    }

    @org.junit.Test
    fun `LoginConfig toModelConfig uses custom values when provided`() {
        val loginConfig = LoginConfig(
            providerType = ModelProviderType.GLM_CODEPLAN,
            glmModel = GLMModel.GLM_5,
            apiKey = "test-key",
            customBaseUrl = "https://custom.api.com"
        )

        val modelConfig = loginConfig.toModelConfig()

        assertEquals("https://custom.api.com", modelConfig.baseUrl)
        assertEquals("GLM-5", modelConfig.modelName)
    }

    // ==================== ModelInfo和ProviderInfo测试 ====================

    @org.junit.Test
    fun `ModelInfo has correct values`() {
        val modelInfo = ModelInfo(
            id = "test-model",
            displayName = "Test Model",
            description = "A test model"
        )

        assertEquals("test-model", modelInfo.id)
        assertEquals("Test Model", modelInfo.displayName)
        assertEquals("A test model", modelInfo.description)
    }

    @org.junit.Test
    fun `ProviderInfo has correct values`() {
        val providerInfo = ProviderInfo(
            id = "test-provider",
            displayName = "Test Provider",
            description = "A test provider",
            models = listOf(
                ModelInfo("model-1", "Model 1", "First model")
            )
        )

        assertEquals("test-provider", providerInfo.id)
        assertEquals("Test Provider", providerInfo.displayName)
        assertEquals(1, providerInfo.models.size)
    }

    // ==================== ValidationResult测试 ====================

    @org.junit.Test
    fun `ValidationResult Success isSuccess is true`() {
        val result = ValidationResult.Success

        assertTrue(result.isSuccess)
        assertFalse(result.isError)
    }

    @org.junit.Test
    fun `ValidationResult Error isError is true`() {
        val result = ValidationResult.Error(listOf("error1", "error2"))

        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertEquals("error1", result.firstError)
    }
}
