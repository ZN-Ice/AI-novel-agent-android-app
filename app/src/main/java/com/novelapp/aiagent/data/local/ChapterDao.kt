package com.novelapp.aiagent.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 章节数据访问对象
 */
@Dao
interface ChapterDao {

    // ==================== 查询操作 ====================

    /**
     * 获取小说的所有章节（按章节号排序）
     */
    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY chapterNumber ASC")
    fun getChaptersByNovelId(novelId: String): Flow<List<ChapterEntity>>

    /**
     * 获取小说的章节列表（不含内容，用于列表显示）
     */
    @Query("SELECT id, novelId, title, wordCount, chapterNumber, status, updatedAt, createdAt, isPublished FROM chapters WHERE novelId = :novelId ORDER BY chapterNumber ASC")
    fun getChapterListByNovelId(novelId: String): Flow<List<ChapterEntity>>

    /**
     * 根据ID获取章节
     */
    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    suspend fun getChapterById(chapterId: String): ChapterEntity?

    /**
     * 根据ID获取章节（Flow）
     */
    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    fun getChapterByIdFlow(chapterId: String): Flow<ChapterEntity?>

    /**
     * 获取小说的最近一章
     */
    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestChapter(novelId: String): ChapterEntity?

    /**
     * 获取小说的第一章
     */
    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY chapterNumber ASC LIMIT 1")
    suspend fun getFirstChapter(novelId: String): ChapterEntity?

    /**
     * 获取指定章节的前一章
     */
    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND chapterNumber < :currentNumber ORDER BY chapterNumber DESC LIMIT 1")
    suspend fun getPreviousChapter(novelId: String, currentNumber: Int): ChapterEntity?

    /**
     * 获取指定章节的后一章
     */
    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND chapterNumber > :currentNumber ORDER BY chapterNumber ASC LIMIT 1")
    suspend fun getNextChapter(novelId: String, currentNumber: Int): ChapterEntity?

    /**
     * 获取最近N章（用于AI上下文）
     */
    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY chapterNumber DESC LIMIT :limit")
    suspend fun getRecentChapters(novelId: String, limit: Int = 3): List<ChapterEntity>

    /**
     * 获取小说的章节数量
     */
    @Query("SELECT COUNT(*) FROM chapters WHERE novelId = :novelId")
    suspend fun getChapterCount(novelId: String): Int

    /**
     * 获取小说的总字数
     */
    @Query("SELECT SUM(wordCount) FROM chapters WHERE novelId = :novelId")
    suspend fun getTotalWordCount(novelId: String): Long?

    /**
     * 获取小说的最大章节号
     */
    @Query("SELECT MAX(chapterNumber) FROM chapters WHERE novelId = :novelId")
    suspend fun getMaxChapterNumber(novelId: String): Int?

    // ==================== 插入操作 ====================

    /**
     * 插入章节
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    /**
     * 批量插入章节
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    // ==================== 更新操作 ====================

    /**
     * 更新章节
     */
    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    /**
     * 更新章节内容
     */
    @Query("UPDATE chapters SET content = :content, wordCount = :wordCount, updatedAt = :updatedAt WHERE id = :chapterId")
    suspend fun updateContent(
        chapterId: String,
        content: String,
        wordCount: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    /**
     * 更新章节标题
     */
    @Query("UPDATE chapters SET title = :title, updatedAt = :updatedAt WHERE id = :chapterId")
    suspend fun updateTitle(chapterId: String, title: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * 更新章节状态
     */
    @Query("UPDATE chapters SET status = :status, updatedAt = :updatedAt WHERE id = :chapterId")
    suspend fun updateStatus(chapterId: String, status: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * 更新章节序号
     */
    @Query("UPDATE chapters SET chapterNumber = :chapterNumber WHERE id = :chapterId")
    suspend fun updateChapterNumber(chapterId: String, chapterNumber: Int)

    // ==================== 删除操作 ====================

    /**
     * 删除章节
     */
    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)

    /**
     * 根据ID删除章节
     */
    @Query("DELETE FROM chapters WHERE id = :chapterId")
    suspend fun deleteById(chapterId: String)

    /**
     * 删除小说的所有章节
     */
    @Query("DELETE FROM chapters WHERE novelId = :novelId")
    suspend fun deleteByNovelId(novelId: String)

    /**
     * 清空所有章节（仅用于测试）
     */
    @Query("DELETE FROM chapters")
    suspend fun deleteAll()

    // ==================== 事务操作 ====================

    /**
     * 在指定位置插入新章节（自动调整后续章节序号）
     */
    @Transaction
    suspend fun insertChapterAtPosition(chapter: ChapterEntity) {
        // 先将后续章节序号+1
        shiftChapterNumbers(chapter.novelId, chapter.chapterNumber, 1)
        // 插入新章节
        insertChapter(chapter)
    }

    /**
     * 移动章节序号
     */
    @Query("UPDATE chapters SET chapterNumber = chapterNumber + :offset WHERE novelId = :novelId AND chapterNumber >= :fromNumber")
    suspend fun shiftChapterNumbers(novelId: String, fromNumber: Int, offset: Int)
}
