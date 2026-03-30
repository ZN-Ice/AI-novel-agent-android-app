package com.novelapp.aiagent.ai.serialization

import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * SSE (Server-Sent Events) 解析器
 *
 * 解析GLM API返回的SSE流式数据
 * 格式: data:{json}\n\n
 */
object SSEParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 解析单行SSE数据
     *
     * @param line SSE数据行
     * @return 解析后的事件，null表示忽略该行
     */
    fun parseSSELine(line: String): SSEEvent? {
        val trimmed = line.trim()

        // 空行忽略
        if (trimmed.isEmpty()) {
            return null
        }

        // 注释行忽略（SSE规范中 : 开头的行是注释）
        if (trimmed.startsWith(":")) {
            return null
        }

        // 非data行忽略
        if (!trimmed.startsWith("data:")) {
            return null
        }

        val dataContent = trimmed.removePrefix("data:").trim()

        // [DONE] 标记流结束
        if (dataContent == "[DONE]") {
            return SSEEvent.Done
        }

        return try {
            val streamResponse = json.decodeFromString<GLMStreamResponse>(dataContent)

            val choice = streamResponse.choices.firstOrNull()
            val content = choice?.delta?.content ?: ""
            val finishReason = choice?.finishReason

            SSEEvent.Data(
                content = content,
                finishReason = finishReason
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse SSE data: $dataContent")
            null
        }
    }

    /**
     * 解析完整SSE流，累积所有内容
     *
     * @param lines SSE数据行列表
     * @return 累积的完整内容
     */
    fun parseSSEStream(lines: List<String>): String {
        val sb = StringBuilder()

        for (line in lines) {
            when (val event = parseSSELine(line)) {
                is SSEEvent.Data -> sb.append(event.content)
                is SSEEvent.Done -> break
                null -> { /* 忽略 */ }
            }
        }

        return sb.toString()
    }
}

/**
 * SSE事件类型
 */
sealed class SSEEvent {
    /**
     * 数据事件，包含生成的文本片段
     */
    data class Data(
        val content: String,
        val finishReason: String? = null
    ) : SSEEvent()

    /**
     * 流结束事件
     */
    object Done : SSEEvent()
}
