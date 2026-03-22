package com.novelapp.aiagent.model

/**
 * AI生成请求
 *
 * @property novelId 小说ID
 * @property chapterId 章节ID
 * @property context 上下文内容
 * @property instruction 用户指令
 * @param maxTokens 最大生成token数
 * @param temperature 创造性参数
 */
data class AIRequest(
    val novelId: String,
    val chapterId: String,
    val context: String,
    val instruction: String? = null,
    val maxTokens: Int = 2000,
    val temperature: Float = 0.8f
)

/**
 * AI生成响应
 *
 * @property code 状态码
 * @property message 状态信息
 * @property data 生成内容
 */
data class AIResponse(
    val code: Int,
    val message: String,
    val data: AIContent?
) {
    val isSuccess: Boolean
        get() = code == 0 && data != null
}

/**
 * AI生成内容
 *
 * @property requestId 请求ID（用于取消/续创）
 * @property text 生成的文本
 * @property tokens 实际token数
 * @property finishReason 结束原因
 */
data class AIContent(
    val requestId: String,
    val text: String,
    val tokens: Int,
    val finishReason: FinishReason
)

/**
 * 生成结束原因
 */
enum class FinishReason(val label: String) {
    STOP("正常结束"),
    LENGTH("达到最大长度"),
    ERROR("发生错误"),
    CANCELLED("用户取消"),
    TIMEOUT("超时");

    companion object {
        fun fromValue(value: String): FinishReason {
            return entries.find { it.name.lowercase() == value.lowercase() } ?: ERROR
        }
    }
}

/**
 * AI结果封装类
 *
 * @param data 成功时的数据
 * @param error 失败时的错误信息
 */
data class AIResult<T>(
    val data: T? = null,
    val error: String? = null
) {
    val isSuccess: Boolean
        get() = data != null && error == null

    companion object {
        fun <T> success(data: T): AIResult<T> = AIResult(data = data)
        fun <T> failure(error: String): AIResult<T> = AIResult(error = error)
    }
}

/**
 * AI生成状态
 */
sealed class AIGenerateState {
    object Idle : AIGenerateState()
    object Connecting : AIGenerateState()
    data class Generating(val progress: Int = 0) : AIGenerateState()
    data class Success(val content: AIContent) : AIGenerateState()
    data class Error(val message: String) : AIGenerateState()
    object Cancelled : AIGenerateState()

    val isGenerating: Boolean
        get() = this is Connecting || this is Generating

    val isSuccess: Boolean
        get() = this is Success

    val isError: Boolean
        get() = this is Error
}

/**
 * 上下文信息（用于AI生成）
 */
data class NovelContext(
    val novelId: String,
    val title: String,
    val genre: String,
    val outline: String = "",
    val characters: List<CharacterInfo> = emptyList(),
    val worldSettings: List<WorldSetting> = emptyList(),
    val recentChapters: List<ChapterSummary> = emptyList(),
    val currentChapter: ChapterContent? = null
)

/**
 * 章节内容
 */
data class ChapterContent(
    val chapterId: String,
    val title: String,
    val content: String,
    val chapterNumber: Int
)

/**
 * 世界观设定
 */
data class WorldSetting(
    val key: String,
    val value: String,
    val description: String = ""
)

/**
 * 检查点（用于断点续创）
 */
data class Checkpoint(
    val checkpointId: String,
    val requestId: String,
    val novelId: String,
    val chapterId: String,
    val generatedText: String,
    val tokens: Int,
    val createdAt: Long,
    val expiresAt: Long
) {
    /**
     * 是否已过期
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiresAt
    }

    companion object {
        const val EXPIRY_DURATION = 24 * 60 * 60 * 1000L // 24小时

        fun create(
            requestId: String,
            novelId: String,
            chapterId: String,
            generatedText: String,
            tokens: Int
        ): Checkpoint {
            val now = System.currentTimeMillis()
            return Checkpoint(
                checkpointId = "cp_${requestId}_${now}",
                requestId = requestId,
                novelId = novelId,
                chapterId = chapterId,
                generatedText = generatedText,
                tokens = tokens,
                createdAt = now,
                expiresAt = now + EXPIRY_DURATION
            )
        }
    }
}
