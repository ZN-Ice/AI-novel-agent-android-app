package com.novelapp.aiagent.ai

import com.novelapp.aiagent.model.NovelContext
import com.novelapp.aiagent.model.ChapterSummary
import com.novelapp.aiagent.model.CharacterInfo
import com.novelapp.aiagent.model.WorldSetting
import com.novelapp.aiagent.model.ChapterContent
import com.novelapp.aiagent.data.repository.ChapterRepository
import com.novelapp.aiagent.data.repository.NovelRepository
import com.novelapp.aiagent.data.local.CharacterDao
import com.novelapp.aiagent.data.local.WorldSettingDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 上下文管理器
 *
 * 职责：
 * - 构建AI生成所需的上下文
 * - 管理上下文长度限制
 * - 按优先级拼接内容
 *
 * @see docs/design/api.md 4.2节
 * @see AGENTS.md 4.2节
 */
@Singleton
class ContextManager @Inject constructor(
    private val novelRepository: NovelRepository,
    private val chapterRepository: ChapterRepository,
    private val characterDao: CharacterDao,
    private val worldSettingDao: WorldSettingDao
) {
    companion object {
        private const val TAG = "ContextManager"

        // 上下文长度限制
        const val MAX_CONTEXT_TOKENS = 4000

        // 预估：1 token ≈ 1.5个中文字符
        const val CHARS_PER_TOKEN = 1.5

        // 各部分最大长度（字符数）
        const val MAX_OUTLINE_LENGTH = 500
        const val MAX_CHARACTER_LENGTH = 200
        const val MAX_CHAPTER_SUMMARY_LENGTH = 300
        const val MAX_CURRENT_CHAPTER_LENGTH = 1000
    }

    /**
     * 构建上下文
     *
     * @param novelId 小说ID
     * @param chapterId 章节ID
     * @return 格式化的上下文字符串
     */
    suspend fun buildContext(novelId: String, chapterId: String): String = withContext(Dispatchers.IO) {
        Timber.d("Building context for novel=$novelId, chapter=$chapterId")

        val novelContext = loadNovelContext(novelId, chapterId)
        val contextString = formatContext(novelContext)

        // 检查长度并截断
        val truncated = truncateIfNeeded(contextString)

        Timber.d("Context built, length=${truncated.length} chars")
        truncated
    }

    /**
     * 加载小说上下文数据
     */
    private suspend fun loadNovelContext(novelId: String, chapterId: String): NovelContext {
        // 获取小说基本信息
        val novelResult = novelRepository.getNovelById(novelId)
        val novel = novelResult.getOrNull()

        if (novel == null) {
            Timber.w("Novel not found: $novelId")
            return NovelContext(novelId, "未知小说", "")
        }

        // 获取最近章节摘要
        val recentChapters = loadRecentChapterSummaries(novelId, 3)

        // 获取当前章节
        val currentChapterResult = chapterRepository.getChapterById(chapterId)
        val currentChapter = currentChapterResult.getOrNull()

        val chapterContent = currentChapter?.let {
            ChapterContent(
                chapterId = it.id,
                title = it.title,
                content = it.content,
                chapterNumber = it.chapterNumber
            )
        }

        // 从数据库加载角色和世界观设定
        val characterEntities = characterDao.getCharactersByNovelId(novelId)
        val characters = characterEntities.map { entity ->
            CharacterInfo(
                name = entity.name,
                role = entity.role,
                description = entity.description,
                personality = entity.personality,
                abilities = entity.abilities
            )
        }

        val worldSettingEntities = worldSettingDao.getWorldSettingsByNovelId(novelId)
        val worldSettings = worldSettingEntities.map { entity ->
            WorldSetting(
                key = entity.key,
                value = entity.value,
                description = entity.description
            )
        }

        return NovelContext(
            novelId = novelId,
            title = novel.title,
            genre = novel.genre,
            outline = novel.description,
            characters = characters,
            worldSettings = worldSettings,
            recentChapters = recentChapters,
            currentChapter = chapterContent
        )
    }

    /**
     * 加载最近章节摘要
     */
    private suspend fun loadRecentChapterSummaries(novelId: String, limit: Int): List<ChapterSummary> {
        val chapters = chapterRepository.getRecentChapters(novelId, limit)

        return chapters.map { chapter ->
            ChapterSummary(
                chapterId = chapter.id,
                title = chapter.title,
                summary = extractSummary(chapter.content),
                keyEvents = extractKeyEvents(chapter.content)
            )
        }
    }

    /**
     * 格式化上下文
     */
    private fun formatContext(context: NovelContext): String {
        val sb = StringBuilder()

        // 1. 小说基本信息
        sb.appendLine("【小说信息】")
        sb.appendLine("标题：${context.title}")
        if (context.genre.isNotBlank()) {
            sb.appendLine("类型：${context.genre}")
        }
        sb.appendLine()

        // 2. 大纲（如果有）
        if (!context.outline.isNullOrBlank()) {
            sb.appendLine("【故事大纲】")
            sb.appendLine(context.outline.take(MAX_OUTLINE_LENGTH))
            sb.appendLine()
        }

        // 3. 角色设定
        if (context.characters.isNotEmpty()) {
            sb.appendLine("【主要角色】")
            context.characters.forEach { character ->
                sb.appendLine(character.toContextString())
            }
            sb.appendLine()
        }

        // 4. 世界观设定
        if (context.worldSettings.isNotEmpty()) {
            sb.appendLine("【世界观】")
            context.worldSettings.forEach { setting ->
                sb.appendLine("- ${setting.key}: ${setting.value}")
            }
            sb.appendLine()
        }

        // 5. 最近章节摘要
        if (context.recentChapters.isNotEmpty()) {
            sb.appendLine("【前文回顾】")
            context.recentChapters.reversed().forEach { summary ->
                sb.appendLine("- ${summary.title}: ${summary.summary}")
            }
            sb.appendLine()
        }

        // 6. 当前章节内容
        context.currentChapter?.let { chapter ->
            sb.appendLine("【当前章节】${chapter.title}")
            sb.appendLine(chapter.content.take(MAX_CURRENT_CHAPTER_LENGTH))
        }

        return sb.toString()
    }

    /**
     * 按需截断上下文
     */
    private fun truncateIfNeeded(context: String): String {
        val maxChars = (MAX_CONTEXT_TOKENS * CHARS_PER_TOKEN).toInt()

        if (context.length <= maxChars) {
            return context
        }

        Timber.w("Context too long (${context.length}), truncating to $maxChars chars")

        // 保留前面的内容，截断当前章节部分
        val lastChapterIndex = context.lastIndexOf("【当前章节】")
        if (lastChapterIndex > 0) {
            val prefix = context.substring(0, lastChapterIndex)
            val chapterContent = context.substring(lastChapterIndex)
            val remainingLength = maxChars - prefix.length

            if (remainingLength > 100) {
                return prefix + chapterContent.take(remainingLength) + "…"
            }
        }

        // 简单截断
        return context.take(maxChars) + "…"
    }

    /**
     * 提取章节摘要
     */
    private fun extractSummary(content: String): String {
        if (content.isBlank()) return ""

        // 简化实现：取前300字
        return content.take(MAX_CHAPTER_SUMMARY_LENGTH).let {
            if (it.length == MAX_CHAPTER_SUMMARY_LENGTH) "$it…" else it
        }
    }

    /**
     * 提取关键事件
     */
    private fun extractKeyEvents(content: String): List<String> {
        // TODO: 使用NLP或AI提取关键事件
        return emptyList()
    }

    /**
     * 估算token数量
     */
    fun estimateTokens(text: String): Int {
        return (text.length / CHARS_PER_TOKEN).toInt()
    }

    /**
     * 检查上下文是否超长
     */
    fun isContextTooLong(text: String): Boolean {
        return estimateTokens(text) > MAX_CONTEXT_TOKENS
    }
}
