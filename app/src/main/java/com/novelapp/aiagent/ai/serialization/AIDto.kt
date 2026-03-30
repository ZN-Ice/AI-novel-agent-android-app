package com.novelapp.aiagent.ai.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GLM Chat Completion 请求 DTO
 */
@Serializable
data class GLMChatRequest(
    val model: String,
    val messages: List<GLMMessage>,
    @SerialName("max_tokens")
    val maxTokens: Int = 2000,
    val temperature: Float = 0.7f,
    val stream: Boolean = false,
    val prompt: String? = null
)

/**
 * GLM 消息
 */
@Serializable
data class GLMMessage(
    val role: String,
    val content: String
)

/**
 * GLM Chat Completion 响应 DTO
 */
@Serializable
data class GLMChatResponse(
    val id: String = "",
    val created: Long = 0,
    val model: String = "",
    val choices: List<GLMChoice> = emptyList(),
    val usage: GLMUsage = GLMUsage()
)

/**
 * GLM 选择项
 */
@Serializable
data class GLMChoice(
    val index: Int = 0,
    val message: GLMMessage = GLMMessage("", ""),
    @SerialName("finish_reason")
    val finishReason: String? = null
)

/**
 * GLM Token 使用量
 */
@Serializable
data class GLMUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0
)

/**
 * GLM SSE 流式响应中的增量数据
 */
@Serializable
data class GLMStreamDelta(
    val role: String? = null,
    val content: String = ""
)

/**
 * GLM SSE 流式选择项
 */
@Serializable
data class GLMStreamChoice(
    val index: Int = 0,
    val delta: GLMStreamDelta = GLMStreamDelta(),
    @SerialName("finish_reason")
    val finishReason: String? = null
)

/**
 * GLM SSE 流式响应 DTO
 */
@Serializable
data class GLMStreamResponse(
    val id: String = "",
    val created: Long = 0,
    val model: String = "",
    val choices: List<GLMStreamChoice> = emptyList()
)

/**
 * 内部 API 请求 DTO
 */
@Serializable
data class InternalAIRequest(
    @SerialName("novel_id")
    val novelId: String,
    @SerialName("chapter_id")
    val chapterId: String,
    val context: String,
    val instruction: String? = null,
    @SerialName("max_tokens")
    val maxTokens: Int = 2000,
    val temperature: Float = 0.8f
)

/**
 * 内部 API 响应数据
 */
@Serializable
data class InternalAIData(
    @SerialName("request_id")
    val requestId: String = "",
    val text: String = "",
    val tokens: Int = 0,
    @SerialName("finish_reason")
    val finishReason: String = ""
)

/**
 * 内部 API 响应 DTO
 */
@Serializable
data class InternalAIResponse(
    val code: Int = -1,
    val message: String = "",
    val data: InternalAIData? = null
)
