package com.novelapp.aiagent.ai.providers

import com.novelapp.aiagent.ai.config.ModelConfig
import com.novelapp.aiagent.ai.config.ModelProviderType
import com.novelapp.aiagent.ai.config.GLMModel
import com.novelapp.aiagent.model.AIRequest
import com.novelapp.aiagent.model.AIResponse
import com.novelapp.aiagent.model.AIResult
import com.novelapp.aiagent.model.AIContent
import com.novelapp.aiagent.model.FinishReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * AI Provider接口
 *
 * 定义统一的AI调用接口，便于扩展不同的模型提供商
 */
interface AIProvider {
    /**
     * 提供商标识
     */
    val providerId: String

    /**
     * 支持的提供商类型
     */
    val supportedProviderTypes: Set<ModelProviderType>

    /**
     * 检查是否支持指定提供商
     */
    fun supports(providerType: ModelProviderType): Boolean

    /**
     * 生成内容
     */
    suspend fun generate(config: ModelConfig, request: AIRequest): AIResult<AIResponse>

    /**
     * 取消生成
     */
    suspend fun cancel(requestId: String): AIResult<Unit>
}

/**
 * GLM Provider实现
 *
 * 支持GLM系列模型（GLM-5, GLM-4.7, GLM-4.6, GLM-4.5-air）
 */
class GLMProvider : AIProvider {

    companion object {
        private const val TAG = "GLMProvider"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        // GLM API端点
        private const val ENDPOINT_CHAT = "chat/completions"

        // 超时配置
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 120L  // GLM生成可能较慢
    }

    override val providerId: String = "glm"

    override val supportedProviderTypes: Set<ModelProviderType> = setOf(ModelProviderType.GLM_CODEPLAN)

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    override fun supports(providerType: ModelProviderType): Boolean {
        return providerType in supportedProviderTypes
    }

    override suspend fun generate(config: ModelConfig, request: AIRequest): AIResult<AIResponse> =
        withContext(Dispatchers.IO) {
            try {
                val url = config.buildApiUrl(ENDPOINT_CHAT)
                val jsonBody = buildGLMRequestBody(config, request)
                val httpRequest = buildRequest(url, config.apiKey, jsonBody)

                Timber.d("GLM request: model=${config.modelName}, tokens=${request.maxTokens}")

                val response = httpClient.newCall(httpRequest).execute()

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Timber.e("GLM request failed: ${response.code}, $errorBody")
                    return@withContext AIResult.failure("请求失败: ${response.code}")
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrBlank()) {
                    return@withContext AIResult.failure("响应为空")
                }

                val aiResponse = parseGLMResponse(responseBody)
                Timber.d("GLM response: tokens=${aiResponse.data?.tokens}")

                AIResult.success(aiResponse)
            } catch (e: CancellationException) {
                Timber.w("GLM request cancelled")
                AIResult.failure("请求已取消")
            } catch (e: Exception) {
                Timber.e(e, "GLM request error")
                AIResult.failure("请求异常: ${e.message}")
            }
        }

    override suspend fun cancel(requestId: String): AIResult<Unit> = withContext(Dispatchers.IO) {
        // GLM API不支持主动取消，这里只是标记
        Timber.i("GLM cancel requested: $requestId")
        AIResult.success(Unit)
    }

    /**
     * 构建GLM请求体
     */
    private fun buildGLMRequestBody(config: ModelConfig, request: AIRequest): String {
        // 根据模型配置获取温度参数
        val temperature = config.glmModel?.recommendedTemperature ?: config.temperature

        return buildString {
            append("{")
            append("\"model\":\"${config.modelName}\",")
            append("\"messages\":[")
            append("{\"role\":\"system\",\"content\":\"你是一个专业的小说创作助手。\"},")
            append("{\"role\":\"user\",\"content\":\"${escapeJson(request.context)}\"}")
            append("],")
            if (request.instruction != null) {
                append("\"prompt\":\"${escapeJson(request.instruction)}\",")
            }
            append("\"max_tokens\":${request.maxTokens},")
            append("\"temperature\":$temperature,")
            append("\"stream\":false")
            append("}")
        }
    }

    /**
     * 构建HTTP请求
     */
    private fun buildRequest(url: String, apiKey: String, jsonBody: String): Request {
        return Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .build()
    }

    /**
     * 解析GLM响应
     */
    private fun parseGLMResponse(json: String): AIResponse {
        return try {
            // 提取choices
            val contentMatch = Regex("\"content\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(json)
            val content = contentMatch?.groupValues?.get(1)
                ?.replace("\\n", "\n")
                ?.replace("\\\"", "\"")
                ?.replace("\\\\", "\\") ?: ""

            // 提取usage
            val promptTokensMatch = Regex("\"prompt_tokens\"\\s*:\\s*(\\d+)").find(json)
            val completionTokensMatch = Regex("\"completion_tokens\"\\s*:\\s*(\\d+)").find(json)
            val promptTokens = promptTokensMatch?.groupValues?.get(1)?.toInt() ?: 0
            val completionTokens = completionTokensMatch?.groupValues?.get(1)?.toInt() ?: 0

            // 提取finish_reason
            val finishReasonMatch = Regex("\"finish_reason\"\\s*:\\s*\"([^\"]+)\"").find(json)
            val finishReason = when (finishReasonMatch?.groupValues?.get(1)) {
                "stop" -> FinishReason.STOP
                "length" -> FinishReason.LENGTH
                else -> FinishReason.ERROR
            }

            // 生成request_id
            val requestId = "glm_${System.currentTimeMillis()}"

            AIResponse(
                code = 0,
                message = "success",
                data = AIContent(
                    requestId = requestId,
                    text = content,
                    tokens = completionTokens,
                    finishReason = finishReason
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse GLM response")
            AIResponse(-1, "解析响应失败", null)
        }
    }

    /**
     * 转义JSON字符串
     */
    private fun escapeJson(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

/**
 * AI Provider工厂
 *
 * 根据提供商类型创建对应的Provider
 */
object AIProviderFactory {

    private val providers = mutableMapOf<String, AIProvider>()

    init {
        // 注册默认Provider
        registerProvider(GLMProvider())
    }

    /**
     * 注册Provider
     */
    fun registerProvider(provider: AIProvider) {
        providers[provider.providerId] = provider
        Timber.i("AI Provider registered: ${provider.providerId}")
    }

    /**
     * 获取Provider
     *
     * @param providerType 提供商类型
     * @return 对应的Provider，找不到返回null
     */
    fun getProvider(providerType: ModelProviderType): AIProvider? {
        return providers.values.find { it.supports(providerType) }
    }

    /**
     * 获取所有支持的提供商类型
     */
    fun getSupportedProviderTypes(): List<ModelProviderType> {
        return providers.values.flatMap { it.supportedProviderTypes }.distinct()
    }
}
