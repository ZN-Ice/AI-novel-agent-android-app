package com.novelapp.aiagent.ai

import com.novelapp.aiagent.model.AIRequest
import com.novelapp.aiagent.model.AIResponse
import com.novelapp.aiagent.model.AIContent
import com.novelapp.aiagent.model.FinishReason
import com.novelapp.aiagent.model.AIResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI客户端
 *
 * 职责：
 * - HTTP请求封装
 * - 请求/响应处理
 * - 超时重试机制
 * - 流式响应处理
 *
 * @see docs/design/api.md
 */
@Singleton
class AIClient @Inject constructor() {

    companion object {
        private const val TAG = "AIClient"

        // 超时配置
        const val CONNECT_TIMEOUT = 10L
        const val READ_TIMEOUT = 30L
        const val WRITE_TIMEOUT = 30L
        const val STREAM_TIMEOUT = 300L  // 5分钟

        // 默认配置
        const val DEFAULT_MAX_TOKENS = 2000
        const val DEFAULT_TEMPERATURE = 0.8f

        // JSON媒体类型
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    // OkHttp客户端
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // API基础URL（从BuildConfig获取）
    private var baseUrl: String = "https://api.novelapp.ai/v1"

    // API密钥
    private var apiKey: String? = null

    /**
     * 设置API配置
     *
     * @param baseUrl API基础URL
     * @param apiKey API密钥
     */
    fun configure(baseUrl: String, apiKey: String) {
        this.baseUrl = baseUrl
        this.apiKey = apiKey
        Timber.i("AI client configured with base URL: $baseUrl")
    }

    /**
     * 生成内容（同步请求）
     *
     * @param request AI请求
     * @return AI结果
     */
    suspend fun generate(request: AIRequest): AIResult<AIResponse> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = buildRequestBody(request)
            val httpRequest = buildRequest("$baseUrl/generate", jsonBody)

            Timber.d("Sending AI request: novelId=${request.novelId}, chapterId=${request.chapterId}")

            val response = httpClient.newCall(httpRequest).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Timber.e("AI request failed: ${response.code}, $errorBody")
                return@withContext AIResult.failure("请求失败: ${response.code}")
            }

            val responseBody = response.body?.string()
            if (responseBody.isNullOrBlank()) {
                return@withContext AIResult.failure("响应为空")
            }

            val aiResponse = parseResponse(responseBody)
            Timber.d("AI request successful: tokens=${aiResponse.data?.tokens}")

            AIResult.success(aiResponse)
        } catch (e: Exception) {
            Timber.e(e, "AI request error")
            AIResult.failure("请求异常: ${e.message}")
        }
    }

    /**
     * 流式生成内容
     *
     * @param request AI请求
     * @return 流式响应
     */
    fun generateStream(request: AIRequest): Flow<StreamEvent> = flow {
        emit(StreamEvent.Start(request.novelId))

        try {
            // TODO: 实现SSE流式请求
            // 这里是简化实现，实际需要处理Server-Sent Events

            val result = generate(request)

            if (result.isSuccess) {
                val response = result.data!!
                emit(StreamEvent.Token(response.data?.text ?: ""))
                emit(StreamEvent.Done(response))
            } else {
                emit(StreamEvent.Error(result.error ?: "生成失败"))
            }
        } catch (e: Exception) {
            emit(StreamEvent.Error(e.message ?: "未知错误"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 取消生成
     *
     * @param requestId 请求ID
     */
    suspend fun cancel(requestId: String): AIResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val httpRequest = buildRequest("$baseUrl/generate/cancel",
                """{"request_id":"$requestId"}""")

            val response = httpClient.newCall(httpRequest).execute()

            if (response.isSuccessful) {
                Timber.i("AI generation cancelled: $requestId")
                AIResult.success(Unit)
            } else {
                AIResult.failure("取消失败")
            }
        } catch (e: Exception) {
            Timber.e(e, "Cancel request error")
            AIResult.failure("取消异常: ${e.message}")
        }
    }

    /**
     * 断点续创
     *
     * @param checkpointId 检查点ID
     * @param maxTokens 最大token数
     */
    suspend fun resume(
        checkpointId: String,
        maxTokens: Int = DEFAULT_MAX_TOKENS
    ): AIResult<AIResponse> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = """{"checkpoint_id":"$checkpointId","max_tokens":$maxTokens}"""
            val httpRequest = buildRequest("$baseUrl/generate/resume", jsonBody)

            val response = httpClient.newCall(httpRequest).execute()

            if (!response.isSuccessful) {
                return@withContext AIResult.failure("续创失败: ${response.code}")
            }

            val responseBody = response.body?.string()
            val aiResponse = parseResponse(responseBody ?: "{}")

            Timber.i("AI generation resumed: $checkpointId")
            AIResult.success(aiResponse)
        } catch (e: Exception) {
            Timber.e(e, "Resume request error")
            AIResult.failure("续创异常: ${e.message}")
        }
    }

    /**
     * 构建请求体
     */
    private fun buildRequestBody(request: AIRequest): String {
        return buildString {
            append("{")
            append("\"novel_id\":\"${request.novelId}\",")
            append("\"chapter_id\":\"${request.chapterId}\",")
            append("\"context\":\"${escapeJson(request.context)}\",")
            if (request.instruction != null) {
                append("\"instruction\":\"${escapeJson(request.instruction)}\",")
            }
            append("\"max_tokens\":${request.maxTokens},")
            append("\"temperature\":${request.temperature}")
            append("}")
        }
    }

    /**
     * 构建HTTP请求
     */
    private fun buildRequest(url: String, jsonBody: String): Request {
        val body = jsonBody.toRequestBody(JSON_MEDIA_TYPE)
        val builder = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")

        apiKey?.let {
            builder.addHeader("Authorization", "Bearer $it")
        }

        return builder.build()
    }

    /**
     * 解析响应
     */
    private fun parseResponse(json: String): AIResponse {
        // 简化实现，实际应使用Moshi/Gson
        // TODO: 使用kotlinx.serialization解析

        return try {
            // 提取code
            val codeMatch = Regex("\"code\"\\s*:\\s*(\\d+)").find(json)
            val code = codeMatch?.groupValues?.get(1)?.toInt() ?: -1

            // 提取message
            val messageMatch = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(json)
            val message = messageMatch?.groupValues?.get(1) ?: ""

            // 提取data - 检查是否有data对象
            val data = if (json.contains("\"data\":")) {
                val requestIdMatch = Regex("\"request_id\"\\s*:\\s*\"([^\"]+)\"").find(json)
                val textMatch = Regex("\"text\"\\s*:\\s*\"([^\"]+)\"").find(json)
                val tokensMatch = Regex("\"tokens\"\\s*:\\s*(\\d+)").find(json)
                val finishReasonMatch = Regex("\"finish_reason\"\\s*:\\s*\"([^\"]+)\"").find(json)

                AIContent(
                    requestId = requestIdMatch?.groupValues?.get(1) ?: "",
                    text = textMatch?.groupValues?.get(1) ?: "",
                    tokens = tokensMatch?.groupValues?.get(1)?.toInt() ?: 0,
                    finishReason = FinishReason.fromValue(finishReasonMatch?.groupValues?.get(1) ?: "error")
                )
            } else null

            AIResponse(code, message, data)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse response")
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
 * 流式事件
 */
sealed class StreamEvent {
    data class Start(val novelId: String) : StreamEvent()
    data class Token(val text: String) : StreamEvent()
    data class Done(val response: AIResponse) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
}
