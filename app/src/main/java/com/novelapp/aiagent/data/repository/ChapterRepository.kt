package com.novelapp.aiagent.data.repository

import com.novelapp.aiagent.data.local.ChapterDao
import com.novelapp.aiagent.data.local.ChapterEntity
import com.novelapp.aiagent.data.local.NovelDao
import com.novelapp.aiagent.model.Chapter
import com.novelapp.aiagent.model.ChapterListItem
import com.novelapp.aiagent.model.ChapterStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 章节数据仓库
 *
 * 职责：
 * - 章节CRUD操作
 * - 章节内容管理
 * - 统计信息更新
 */
@Singleton
class ChapterRepository @Inject constructor(
    private val chapterDao: ChapterDao,
    private val novelDao: NovelDao
) {
    /**
     * 获取小说的所有章节
     */
    fun getChaptersByNovelId(novelId: String): Flow<List<Chapter>> {
        return chapterDao.getChaptersByNovelId(novelId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * 获取章节列表（用于UI显示）
     */
    fun getChapterList(novelId: String): Flow<List<ChapterListItem>> {
        return chapterDao.getChapterListByNovelId(novelId).map { entities ->
            entities.map { entity ->
                ChapterListItem(
                    id = entity.id,
                    title = entity.title,
                    wordCount = entity.wordCount,
                    chapterNumber = entity.chapterNumber,
                    status = ChapterStatus.valueOf(entity.status),
                    updatedAt = entity.updatedAt
                )
            }
        }
    }

    /**
     * 根据ID获取章节
     */
    suspend fun getChapterById(chapterId: String): Result<Chapter> {
        return try {
            val entity = chapterDao.getChapterById(chapterId)
            if (entity != null) {
                Result.success(entity.toModel())
            } else {
                Result.failure(Exception("章节不存在"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取最近编辑的章节
     */
    suspend fun getLatestChapter(novelId: String): Result<Chapter> {
        return try {
            val entity = chapterDao.getLatestChapter(novelId)
            if (entity != null) {
                Result.success(entity.toModel())
            } else {
                Result.failure(Exception("暂无章节"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取上一章
     */
    suspend fun getPreviousChapter(novelId: String, currentChapterNumber: Int): Result<Chapter> {
        return try {
            val entity = chapterDao.getPreviousChapter(novelId, currentChapterNumber)
            if (entity != null) {
                Result.success(entity.toModel())
            } else {
                Result.failure(Exception("已是第一章"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取下一章
     */
    suspend fun getNextChapter(novelId: String, currentChapterNumber: Int): Result<Chapter> {
        return try {
            val entity = chapterDao.getNextChapter(novelId, currentChapterNumber)
            if (entity != null) {
                Result.success(entity.toModel())
            } else {
                Result.failure(Exception("已是最后一章"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 创建新章节
     */
    suspend fun createChapter(novelId: String, title: String): Result<Chapter> {
        return try {
            val maxNumber = chapterDao.getMaxChapterNumber(novelId) ?: 0
            val newNumber = maxNumber + 1

            val now = System.currentTimeMillis()
            val chapter = ChapterEntity(
                id = UUID.randomUUID().toString(),
                novelId = novelId,
                title = title.ifBlank { "第${newNumber.toChineseNumber()}章" },
                chapterNumber = newNumber,
                createdAt = now,
                updatedAt = now
            )

            chapterDao.insertChapter(chapter)

            // 更新小说的章节数
            novelDao.updateChapterCount(novelId, newNumber)

            Result.success(chapter.toModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新章节内容
     */
    suspend fun updateChapterContent(
        chapterId: String,
        content: String,
        novelId: String
    ): Result<Unit> {
        return try {
            val wordCount = calculateWordCount(content)
            chapterDao.updateContent(chapterId, content, wordCount)

            // 更新小说的总字数
            val totalWordCount = chapterDao.getTotalWordCount(novelId) ?: 0L
            novelDao.updateWordCount(novelId, totalWordCount)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新章节标题
     */
    suspend fun updateChapterTitle(chapterId: String, title: String): Result<Unit> {
        return try {
            if (title.isBlank()) {
                return Result.failure(Exception("章节标题不能为空"))
            }
            chapterDao.updateTitle(chapterId, title)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除章节
     */
    suspend fun deleteChapter(novelId: String, chapterId: String): Result<Unit> {
        return try {
            chapterDao.deleteById(chapterId)

            // 更新小说统计
            val chapterCount = chapterDao.getChapterCount(novelId)
            val wordCount = chapterDao.getTotalWordCount(novelId) ?: 0L

            novelDao.updateChapterCount(novelId, chapterCount)
            novelDao.updateWordCount(novelId, wordCount)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取最近N章（用于AI上下文）
     */
    suspend fun getRecentChapters(novelId: String, limit: Int = 3): List<Chapter> {
        return chapterDao.getRecentChapters(novelId, limit).map { it.toModel() }
    }

    /**
     * 计算字数
     */
    private fun calculateWordCount(content: String): Int {
        // 中文按字符数，英文按单词数
        val chineseChars = content.count { it.code in 0x4E00..0x9FFF }
        val englishWords = content.split(Regex("\\s+"))
            .count { it.isNotEmpty() && it.all { it.code !in 0x4E00..0x9FFF } }
        return chineseChars + englishWords
    }

    /**
     * 数字转中文
     */
    private fun Int.toChineseNumber(): String {
        val chars = arrayOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
        val units = arrayOf("", "十", "百", "千", "万")

        if (this == 0) return "零"
        if (this < 10) return chars[this]

        val result = StringBuilder()
        var num = this
        var unitIndex = 0

        while (num > 0) {
            val digit = num % 10
            if (digit != 0) {
                result.insert(0, chars[digit] + units[unitIndex])
            } else if (result.isNotEmpty() && !result.startsWith("零")) {
                result.insert(0, "零")
            }
            num /= 10
            unitIndex++
        }

        return result.toString().replace(Regex("^一十"), "十")
    }
}
