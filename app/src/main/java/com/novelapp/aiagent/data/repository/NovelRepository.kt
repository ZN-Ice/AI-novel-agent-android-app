package com.novelapp.aiagent.data.repository

import com.novelapp.aiagent.data.local.ChapterDao
import com.novelapp.aiagent.data.local.ChapterEntity
import com.novelapp.aiagent.data.local.NovelDao
import com.novelapp.aiagent.data.local.NovelEntity
import com.novelapp.aiagent.model.Chapter
import com.novelapp.aiagent.model.Novel
import com.novelapp.aiagent.model.NovelStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 小说数据仓库
 *
 * 职责：
 * - 统一数据访问接口
 * - 协调本地/远程数据源
 * - 缓存策略
 */
@Singleton
class NovelRepository @Inject constructor(
    private val novelDao: NovelDao,
    private val chapterDao: ChapterDao
) {
    /**
     * 获取所有小说列表
     */
    fun getAllNovels(): Flow<List<Novel>> {
        return novelDao.getAllNovels().map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * 根据ID获取小说
     */
    suspend fun getNovelById(novelId: String): Result<Novel> {
        return try {
            val entity = novelDao.getNovelById(novelId)
            if (entity != null) {
                Result.success(entity.toModel())
            } else {
                Result.failure(Exception("小说不存在"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 创建新小说
     * @param title 小说标题
     * @param genre 小说类型
     */
    suspend fun createNovel(title: String, genre: String): Result<Novel> {
        return try {
            // 校验标题
            if (title.isBlank()) {
                return Result.failure(Exception("小说标题不能为空"))
            }
            if (title.length > 50) {
                return Result.failure(Exception("小说标题不能超过50个字符"))
            }

            // 检查重名
            if (novelDao.existsByTitle(title) > 0) {
                return Result.failure(Exception("已存在同名小说"))
            }

            // 创建小说实体
            val novelId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val novel = NovelEntity(
                id = novelId,
                title = title,
                genre = genre,
                createdAt = now,
                updatedAt = now
            )

            // 创建第一章
            val chapterId = UUID.randomUUID().toString()
            val firstChapter = ChapterEntity(
                id = chapterId,
                novelId = novelId,
                title = "第一章",
                chapterNumber = 1,
                createdAt = now,
                updatedAt = now
            )

            // 保存到数据库
            novelDao.insertNovel(novel)
            chapterDao.insertChapter(firstChapter)

            // 更新章节计数
            novelDao.updateChapterCount(novelId, 1)

            Result.success(novel.toModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新小说信息
     */
    suspend fun updateNovel(novel: Novel): Result<Unit> {
        return try {
            val entity = NovelEntity.fromModel(novel)
            novelDao.updateNovel(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新小说标题
     */
    suspend fun updateTitle(novelId: String, newTitle: String): Result<Unit> {
        return try {
            // 校验标题
            if (newTitle.isBlank()) {
                return Result.failure(Exception("小说标题不能为空"))
            }
            if (newTitle.length > 50) {
                return Result.failure(Exception("小说标题不能超过50个字符"))
            }

            // 检查重名（排除当前小说）
            if (novelDao.existsByTitleExclude(newTitle, novelId) > 0) {
                return Result.failure(Exception("已存在同名小说"))
            }

            novelDao.updateTitle(novelId, newTitle)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 软删除小说
     */
    suspend fun deleteNovel(novelId: String): Result<Unit> {
        return try {
            novelDao.softDelete(novelId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 恢复已删除的小说
     */
    suspend fun restoreNovel(novelId: String): Result<Unit> {
        return try {
            novelDao.restore(novelId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 永久删除小说
     */
    suspend fun permanentDeleteNovel(novelId: String): Result<Unit> {
        return try {
            // 删除所有章节
            chapterDao.deleteByNovelId(novelId)
            // 删除小说
            novelDao.deleteById(novelId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 搜索小说
     */
    fun searchNovels(keyword: String): Flow<List<Novel>> {
        return novelDao.searchNovels(keyword).map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * 获取回收站小说
     */
    fun getDeletedNovels(): Flow<List<Novel>> {
        return novelDao.getDeletedNovels().map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * 清理过期小说（7天前删除的）
     */
    suspend fun cleanExpiredNovels(): Result<Int> {
        return try {
            val expireTime = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            val expiredNovels = novelDao.getExpiredNovels(expireTime)

            expiredNovels.forEach { novel ->
                chapterDao.deleteByNovelId(novel.id)
            }

            novelDao.deleteExpiredNovels(expireTime)

            Result.success(expiredNovels.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新小说统计信息
     */
    suspend fun updateNovelStats(novelId: String): Result<Unit> {
        return try {
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
     * 获取小说数量
     */
    suspend fun getNovelCount(): Int {
        return novelDao.getNovelCount()
    }
}
