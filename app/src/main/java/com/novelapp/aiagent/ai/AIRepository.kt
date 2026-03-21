package com.novelapp.aiagent.ai

import com.novelapp.aiagent.ai.config.ModelConfigManager
import com.novelapp.aiagent.ai.config.ModelFeature
import com.novelapp.aiagent.ai.config.ModelProviderType
import com.novelapp.aiagent.ai.providers.AIProviderFactory
import com.novelapp.aiagent.model.AIRequest
import com.novelapp.aiagent.model.AIResponse
import com.novelapp.aiagent.model.AIResult
import com.novelapp.aiagent.model.Checkpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI仓库（重构版）
 *
 * 职责：
 * - 统一AI接口访问
 * - 使用ModelConfigManager获取配置
 * - 支持多模型切换
 *
 * @see docs/design/api.md
 * @see AGENTS.md AI接口模块规范
 */
@Singleton
class AIRepository @Inject constructor(
    private val modelConfigManager: ModelConfigManager,
    private val contextManager: ContextManager
) {
    companion object {
        private const val TAG = "AIRepository"

        // 重试配置
        const val MAX_RETRY = 3
        val RETRY_INTERVALS = listOf(1000L, 2000L, 4000L)

        // 检查点配置
        const val CHECKPOINT_TOKEN_INTERVAL = 500
        const val MAX_CHECKPOINTS = 3
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
        // 检查是否已登录
        if (!modelConfigManager.isLoggedIn()) {
            return AIResult.failure("请先登录并配置模型")
        }

        val config = modelConfigManager.getConfig()!!

        return withContext(Dispatchers.IO) {
            // 构建上下文
            val context = contextManager.buildContext(novelId, chapterId)

            // 创建请求
            val request = AIRequest(
                novelId = novelId,
                chapterId = chapterId,
                context = context,
                instruction = instruction,
                maxTokens = config.maxTokens,
                temperature = config.temperature
            )

            // 获取对应的Provider
            val provider = AIProviderFactory.getProvider(config.providerType)
            if (provider == null) {
                Timber.e("No provider found for: ${config.providerType}")
                return@withContext AIResult.failure("不支持的模型提供商")
            }

            // 执行请求（带重试）
            executeWithRetry(provider, config, request, 0)
        }
    }

    /**
     * 取消生成
     *
     * @param requestId 请求ID
     */
    suspend fun cancelGeneration(requestId: String): AIResult<Unit> {
        val config = modelConfigManager.getConfig() ?: return AIResult.failure("未登录")

        return withContext(Dispatchers.IO) {
            val provider = AIProviderFactory.getProvider(config.providerType)
            provider?.cancel(requestId) ?: AIResult.failure("Provider不存在")
        }
    }

    /**
     * 断点续创
     *
     * @param checkpointId 检查点ID
     */
    suspend fun resumeGeneration(checkpointId: String): AIResult<AIResponse> {
        val checkpoint = activeCheckpoints[checkpointId]
            ?: return AIResult.failure("检查点不存在或已过期")

        if (checkpoint.isExpired()) {
            activeCheckpoints.remove(checkpointId)
            return AIResult.failure("检查点已过期")
        }

        // 使用检查点信息重新生成
        return generateContent(
            novelId = checkpoint.novelId,
            chapterId = checkpoint.chapterId,
            instruction = "继续从上次中断的地方续写"
        )
    }

    /**
     * 获取活跃的检查点
     *
     * @param novelId 小说ID
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
     * 检查是否支持某特性
     */
    fun supportsFeature(feature: ModelFeature): Boolean {
        return modelConfigManager.supportsFeature(feature)
    }

    /**
     * 检查是否已登录
     */
    fun isLoggedIn(): Boolean {
        return modelConfigManager.isLoggedIn()
    }

    /**
     * 带重试的执行
     */
    private suspend fun executeWithRetry(
        provider: com.novelapp.aiagent.ai.providers.AIProvider,
        config: com.novelapp.aiagent.ai.config.ModelConfig,
        request: AIRequest,
        retryCount: Int
    ): AIResult<AIResponse> {
        val result = provider.generate(config, request)

        if (result.isSuccess) {
            // 保存检查点
            result.data?.data?.let { content ->
                if (content.tokens >= CHECKPOINT_TOKEN_INTERVAL) {
                    saveCheckpoint(
                        requestId = content.requestId,
                        novelId = request.novelId,
                        chapterId = request.chapterId,
                        generatedText = content.text,
                        tokens = content.tokens
                    )
                }
            }
            return result
        }

        // 检查是否需要重试
        if (retryCount < MAX_RETRY) {
            val delay = RETRY_INTERVALS.getOrElse(retryCount) { 4000L }
            Timber.w("Retrying AI request (attempt ${retryCount + 1}/$MAX_RETRY) after ${delay}ms")
            kotlinx.coroutines.delay(delay)
            return executeWithRetry(provider, config, request, retryCount + 1)
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
