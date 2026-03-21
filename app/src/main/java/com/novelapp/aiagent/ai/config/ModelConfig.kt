package com.novelapp.aiagent.ai.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * AI模型配置
 *
 * 支持动态配置不同的AI模型，便于后期扩展
 *
 * @property providerType 模型提供商类型
 * @property glmModel GLM模型类型（当providerType为GLM_CODEPLAN时使用）
 * @property apiKey API密钥
 * @property baseUrl API基础URL
 * @property modelName 模型名称
 * @property maxTokens 最大token数
 * @property temperature 创造性参数
 */
@Serializable
data class ModelConfig(
    @SerialName("provider_type")
    val providerType: ModelProviderType,

    @SerialName("glm_model")
    val glmModel: GLMModel? = null,

    @SerialName("api_key")
    val apiKey: String,

    @SerialName("base_url")
    val baseUrl: String,

    @SerialName("model_name")
    val modelName: String,

    @SerialName("max_tokens")
    val maxTokens: Int = 4096,

    @SerialName("temperature")
    val temperature: Float = 0.7f,

    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @SerialName("updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 验证配置是否有效
     */
    fun isValid(): Boolean {
        return apiKey.isNotBlank() &&
                baseUrl.isNotBlank() &&
                modelName.isNotBlank() &&
                maxTokens > 0
    }

    /**
     * 构建完整的API URL
     */
    fun buildApiUrl(endpoint: String): String {
        val base = baseUrl.trimEnd('/')
        val path = endpoint.trimStart('/')
        return "$base/$path"
    }

    /**
     * 获取模型显示名称
     */
    fun getDisplayName(): String {
        return glmModel?.displayName ?: providerType.displayName
    }

    companion object {
        /**
         * 创建GLM配置
         */
        fun createGLMConfig(
            glmModel: GLMModel,
            apiKey: String
        ): ModelConfig {
            return ModelConfig(
                providerType = ModelProviderType.GLM_CODEPLAN,
                glmModel = glmModel,
                apiKey = apiKey,
                baseUrl = ModelProviderType.GLM_CODEPLAN.defaultBaseUrl,
                modelName = glmModel.modelId
            )
        }
    }
}

/**
 * 模型提供商类型
 *
 * 后期可扩展添加新的提供商
 */
@Serializable
enum class ModelProviderType(
    val id: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val description: String,
    val supportedModels: List<ModelInfo>
) {
    /**
     * GLM CodePlan
     *
     * 智谱AI代码助手API，支持多种GLM模型
     */
    @SerialName("glm_codeplan")
    GLM_CODEPLAN(
        id = "glm_codeplan",
        displayName = "GLM CodePlan",
        defaultBaseUrl = "https://open.bigmodel.cn/api/coding/paas/v4",
        description = "智谱AI GLM系列模型，支持小说创作",
        supportedModels = GLMModel.entries.map { model ->
            ModelInfo(
                id = model.modelId,
                displayName = model.displayName,
                description = model.description
            )
        }
    );

    /**
     * 根据ID获取提供商类型
     */
    companion object {
        fun fromId(id: String): ModelProviderType? {
            return entries.find { it.id == id }
        }

        /**
         * 获取所有支持的提供商（用于UI展示）
         */
        fun getSupportedProviders(): List<ProviderInfo> {
            return entries.map { provider ->
                ProviderInfo(
                    id = provider.id,
                    displayName = provider.displayName,
                    description = provider.description,
                    models = provider.supportedModels
                )
            }
        }
    }
}

/**
 * GLM模型类型
 *
 * 支持的GLM模型列表
 */
@Serializable
enum class GLMModel(
    val modelId: String,
    val displayName: String,
    val description: String,
    val maxTokens: Int = 4096,
    val recommendedTemperature: Float = 0.7f
) {
    @SerialName("glm_5")
    GLM_5(
        modelId = "GLM-5",
        displayName = "GLM 5",
        description = "最新一代GLM模型，性能最强",
        maxTokens = 8192,
        recommendedTemperature = 0.7f
    ),

    @SerialName("glm_4_7")
    GLM_4_7(
        modelId = "GLM-4.7",
        displayName = "GLM 4.7",
        description = "高性能模型，平衡效果与速度",
        maxTokens = 4096,
        recommendedTemperature = 0.7f
    ),

    @SerialName("glm_4_6")
    GLM_4_6(
        modelId = "GLM-4.6",
        displayName = "GLM 4.6",
        description = "稳定版本，适合长文本创作",
        maxTokens = 4096,
        recommendedTemperature = 0.8f
    ),

    @SerialName("glm_4_5_air")
    GLM_4_5_AIR(
        modelId = "GLM-4.5-air",
        displayName = "GLM 4.5 air",
        description = "轻量级模型，响应速度快",
        maxTokens = 2048,
        recommendedTemperature = 0.7f
    );

    /**
     * 根据模型ID获取模型
     */
    companion object {
        fun fromModelId(modelId: String): GLMModel? {
            return entries.find { it.modelId == modelId }
        }

        /**
         * 获取所有模型（用于UI展示）
         */
        fun getAllModels(): List<ModelInfo> {
            return entries.map { model ->
                ModelInfo(
                    id = model.modelId,
                    displayName = model.displayName,
                    description = model.description
                )
            }
        }
    }
}

/**
 * 模型信息（用于UI展示）
 */
@Serializable
data class ModelInfo(
    @SerialName("id")
    val id: String,

    @SerialName("display_name")
    val displayName: String,

    @SerialName("description")
    val description: String
)

/**
 * 提供商信息（用于UI展示）
 */
@Serializable
data class ProviderInfo(
    @SerialName("id")
    val id: String,

    @SerialName("display_name")
    val displayName: String,

    @SerialName("description")
    val description: String,

    @SerialName("models")
    val models: List<ModelInfo>
)

/**
 * 登录配置（用于登录时选择模型）
 *
 * 登录流程：
 * 1. 选择模型方案（ModelProviderType）
 * 2. 选择模型类型（GLMModel）
 * 3. 输入API Key
 */
@Serializable
data class LoginConfig(
    @SerialName("provider_type")
    val providerType: ModelProviderType,

    @SerialName("glm_model")
    val glmModel: GLMModel? = null,

    @SerialName("api_key")
    val apiKey: String,

    @SerialName("custom_base_url")
    val customBaseUrl: String? = null
) {
    /**
     * 转换为完整的ModelConfig
     */
    fun toModelConfig(): ModelConfig {
        return when (providerType) {
            ModelProviderType.GLM_CODEPLAN -> {
                ModelConfig.createGLMConfig(
                    glmModel = glmModel ?: GLMModel.GLM_4_6,
                    apiKey = apiKey
                )
            }
        }
    }

    /**
     * 验证登录配置
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        // 验证API Key
        if (apiKey.isBlank()) {
            errors.add("API密钥不能为空")
        }

        if (apiKey.length < 10) {
            errors.add("API密钥格式不正确")
        }

        // 验证GLM模型选择
        if (providerType == ModelProviderType.GLM_CODEPLAN && glmModel == null) {
            errors.add("请选择模型类型")
        }

        // 验证自定义URL
        customBaseUrl?.let { url ->
            if (url.isNotBlank() && !url.startsWith("https://")) {
                errors.add("API地址必须使用HTTPS")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
}

/**
 * 验证结果
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val messages: List<String>) : ValidationResult()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val firstError: String? get() = (this as? Error)?.messages?.firstOrNull()
}
