package com.novelapp.aiagent.ai

import com.novelapp.aiagent.model.AIRequest
import com.novelapp.aiagent.model.AIResponse
import com.novelapp.aiagent.model.AIResult
import com.novelapp.aiagent.model.Checkpoint
import com.novelapp.aiagent.model.NovelContext
import com.novelapp.aiagent.model.ChapterSummary
import com.novelapp.aiagent.model.CharacterInfo
import com.novelapp.aiagent.data.repository.ChapterRepository
import com.novelapp.aiagent.data.repository.NovelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI仓库
 *
 * 职责：
 * - 统一AI接口访问
 * - 超时重试机制
 * - 断点续创管理
 * - 上下文管理
 *
 * @see docs/design/api.md
 */
@Singleton
class AIRepository @Inject constructor(
    private val aiClient: AIClient,
    private val contextManager: ContextManager,
    private val novelRepository: NovelRepository,
    private val chapterRepository: ChapterRepository
) {
    companion object {
        private const val TAG = "AIRepository"

        // 重试配置
        const val MAX_RETRY = 3
        val RETRY_INTERVALS = listOf(1000L, 2000L, 4000L)

        // 检查点配置
        const val CHECKPOINT_TOKEN_INTERVAL = 500  // 每500字保存检查点
        const val MAX_CHECKPOINTS = 3

        // 可重试的错误码
        val RETRYABLE_CODES = listOf(429, 500, 503, 504)
    }

    // 活跃的检查点
    private val activeCheckpoints = mutableMapOf<String, Checkpoint>()

    /**
     * 生成内容
     *
     * @param novelId 小说ID
     * @param chapterId 章节ID
     * @param instruction 用户指令（可选）
     * @return 生成结果
     */
    suspend fun generateContent(
        novelId: String,
        chapterId: String,
        instruction: String? = null
    ): AIResult<AIResponse> {
        return withContext(Dispatchers.IO) {
            // 构建上下文
            val context = contextManager.buildContext(novelId, chapterId)

            // 创建请求
            val request = AIRequest(
                novelId = novelId,
                chapterId = chapterId,
                context = context,
                instruction = instruction,
                maxTokens = AIClient.DEFAULT_MAX_TOKENS,
                temperature = AIClient.DEFAULT_TEMPERATURE
            )

            // 执行请求（带重试）
            executeWithRetry(request)
        }
    }

    /**
     * 取消生成
     *
     * @param requestId 请求ID
     */
    suspend fun cancelGeneration(requestId: String): AIResult<Unit> {
        return withContext(Dispatchers.IO) {
            aiClient.cancel(requestId)
        }
    }

    /**
     * 断点续创
     *
     * @param checkpointId 检查点ID
     */
    suspend fun resumeGeneration(checkpointId: String): AIResult<AIResponse> {
        return withContext(Dispatchers.IO) {
            val checkpoint = activeCheckpoints[checkpointId]
            if (checkpoint == null) {
                AIResult.failure("检查点不存在或已过期")
            } else if (checkpoint.isExpired()) {
                activeCheckpoints.remove(checkpointId)
                AIResult.failure("检查点已过期")
            } else {
                aiClient.resume(checkpointId)
            }
        }
    }

    /**
     * 获取活跃的检查点
     *
     * @param novelId 小说ID
     * @return 检查点列表
     */
    fun getActiveCheckpoints(novelId: String): List<Checkpoint> {
        return activeCheckpoints.values
            .filter { it.novelId == novelId && !it.isExpired() }
            .sortedByDescending { it.createdAt }
    }

    /**
     * 清理过期检查点
     */
    fun cleanExpiredCheckpoints() {
        val expiredKeys = activeCheckpoints.entries
            .filter { it.value.isExpired() }
            .map { it.key }

        expiredKeys.forEach { activeCheckpoints.remove(it) }

        if (expiredKeys.isNotEmpty()) {
            Timber.i("Cleaned ${expiredKeys.size} expired checkpoints")
        }
    }

    /**
     * 带重试的执行
     */
    private suspend fun executeWithRetry(
        request: AIRequest,
        retryCount: Int = 0
    ): AIResult<AIResponse> {
        val result = aiClient.generate(request)

        if (result.isSuccess) {
            return result
        }

        // 检查是否需要重试
        val response = result.data
        if (response != null && response.code in RETRYABLE_CODES && retryCount < MAX_RETRY) {
            val delay = RETRY_INTERVALS.getOrElse(retryCount) { 4000L }
            Timber.w("Retrying AI request (attempt ${retryCount + 1}/$MAX_RETRY) after ${delay}ms")
            kotlinx.coroutines.delay(delay)
            return executeWithRetry(request, retryCount + 1)
        }

        return result
    }

    /**
     * 保存检查点
     */
    private fun saveCheckpoint(
        requestId: String,
        novelId: String,
        chapterId: String,
        generatedText: String,
        tokens: Int
    ) {
        val checkpoint = Checkpoint.create(
            requestId = requestId,
            novelId = novelId,
            chapterId = chapterId,
            generatedText = generatedText,
            tokens = tokens
        )

        activeCheckpoints[checkpoint.checkpointId] = checkpoint

        // 限制检查点数量
        if (activeCheckpoints.size > MAX_CHECKPOINTS * 10) {
            cleanExpiredCheckpoints()
        }

        Timber.d("Checkpoint saved: ${checkpoint.checkpointId}")
    }
}
