package com.novelapp.aiagent.ai.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * AI模型配置
 *
 * 支持动态配置不同的AI模型，便于后期扩展
 *
 * @property modelType 模型类型
 * @property apiKey API密钥
 * @property baseUrl API基础URL
 * @property modelName 模型名称
 * @property maxTokens 最大token数
 * @property temperature 创造性参数
 */
@Serializable
data class ModelConfig(
    @SerialName("model_type")
    val modelType: ModelType,

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
        return modelType.displayName
    }
}

/**
 * 支持的模型类型
 *
 * 后期可扩展添加新的模型类型
 */
@Serializable
enum class ModelType(
    val id: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModelName: String,
    val description: String,
    val features: Set<ModelFeature>
) {
    /**
     * GLM CodePlan模型
     *
     * 支持长文本生成，适合小说创作
     */
    @SerialName("glm_codeplan")
    GLM_CODEPLAN(
        id = "glm_codeplan",
        displayName = "GLM CodePlan",
        defaultBaseUrl = "https://open.bigmodel.cn/api/paas/v4",
        defaultModelName = "glm-4-plus",
        description = "智谱AI GLM-4模型，支持长文本创作",
        features = setOf(
            ModelFeature.LONG_CONTEXT,
            ModelFeature.STREAMING,
            ModelFeature.FUNCTION_CALLING
        )
    );

    /**
     * 根据ID获取模型类型
     */
    companion object {
        fun fromId(id: String): ModelType? {
            return entries.find { it.id == id }
        }

        /**
         * 获取所有支持的模型类型（用于UI展示）
         */
        fun getSupportedModels(): List<ModelTypeInfo> {
            return entries.map { type ->
                ModelTypeInfo(
                    id = type.id,
                    displayName = type.displayName,
                    description = type.description,
                    features = type.features.map { it.displayName }
                )
            }
        }
    }
}

/**
 * 模型特性
 */
@Serializable
enum class ModelFeature(val displayName: String) {
    @SerialName("long_context")
    LONG_CONTEXT("长文本支持"),

    @SerialName("streaming")
    STREAMING("流式输出"),

    @SerialName("function_calling")
    FUNCTION_CALLING("函数调用"),

    @SerialName("vision")
    VISION("图像理解"),

    @SerialName("code_generation")
    CODE_GENERATION("代码生成");
}

/**
 * 模型类型信息（用于UI展示）
 */
@Serializable
data class ModelTypeInfo(
    @SerialName("id")
    val id: String,

    @SerialName("display_name")
    val displayName: String,

    @SerialName("description")
    val description: String,

    @SerialName("features")
    val features: List<String>
)

/**
 * 登录配置（用于登录时选择模型）
 */
@Serializable
data class LoginConfig(
    @SerialName("model_type")
    val modelType: ModelType,

    @SerialName("api_key")
    val apiKey: String,

    @SerialName("custom_base_url")
    val customBaseUrl: String? = null,  // 可选自定义URL

    @SerialName("custom_model_name")
    val customModelName: String? = null  // 可选自定义模型名
) {
    /**
     * 转换为完整的ModelConfig
     */
    fun toModelConfig(): ModelConfig {
        return ModelConfig(
            modelType = modelType,
            apiKey = apiKey,
            baseUrl = customBaseUrl ?: modelType.defaultBaseUrl,
            modelName = customModelName ?: modelType.defaultModelName
        )
    }

    /**
     * 验证登录配置
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (apiKey.isBlank()) {
            errors.add("API密钥不能为空")
        }

        if (apiKey.length < 10) {
            errors.add("API密钥格式不正确")
        }

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
