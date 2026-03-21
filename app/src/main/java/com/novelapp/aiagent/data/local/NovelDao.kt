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
 * 小说数据访问对象
 */
@Dao
interface NovelDao {

    // ==================== 查询操作 ====================

    /**
     * 获取所有未删除的小说（按更新时间降序）
     */
    @Query("SELECT * FROM novels WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllNovels(): Flow<List<NovelEntity>>

    /**
     * 根据ID获取小说
     */
    @Query("SELECT * FROM novels WHERE id = :novelId")
    suspend fun getNovelById(novelId: String): NovelEntity?

    /**
     * 根据ID获取小说（Flow）
     */
    @Query("SELECT * FROM novels WHERE id = :novelId")
    fun getNovelByIdFlow(novelId: String): Flow<NovelEntity?>

    /**
     * 检查标题是否已存在
     */
    @Query("SELECT COUNT(*) FROM novels WHERE title = :title AND isDeleted = 0")
    suspend fun existsByTitle(title: String): Int

    /**
     * 检查标题是否已存在（排除指定ID）
     */
    @Query("SELECT COUNT(*) FROM novels WHERE title = :title AND id != :excludeId AND isDeleted = 0")
    suspend fun existsByTitleExclude(title: String, excludeId: String): Int

    /**
     * 搜索小说
     */
    @Query("SELECT * FROM novels WHERE title LIKE '%' || :keyword || '%' AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun searchNovels(keyword: String): Flow<List<NovelEntity>>

    /**
     * 获取已删除的小说（回收站）
     */
    @Query("SELECT * FROM novels WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedNovels(): Flow<List<NovelEntity>>

    /**
     * 获取过期的已删除小说（超过7天）
     */
    @Query("SELECT * FROM novels WHERE isDeleted = 1 AND deletedAt < :expireTime")
    suspend fun getExpiredNovels(expireTime: Long): List<NovelEntity>

    /**
     * 获取小说总数
     */
    @Query("SELECT COUNT(*) FROM novels WHERE isDeleted = 0")
    suspend fun getNovelCount(): Int

    // ==================== 插入操作 ====================

    /**
     * 插入小说
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNovel(novel: NovelEntity): Long

    /**
     * 批量插入小说
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNovels(novels: List<NovelEntity>)

    // ==================== 更新操作 ====================

    /**
     * 更新小说
     */
    @Update
    suspend fun updateNovel(novel: NovelEntity)

    /**
     * 更新小说标题
     */
    @Query("UPDATE novels SET title = :title, updatedAt = :updatedAt WHERE id = :novelId")
    suspend fun updateTitle(novelId: String, title: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * 更新章节统计
     */
    @Query("UPDATE novels SET chapterCount = :count, updatedAt = :updatedAt WHERE id = :novelId")
    suspend fun updateChapterCount(novelId: String, count: Int, updatedAt: Long = System.currentTimeMillis())

    /**
     * 更新字数统计
     */
    @Query("UPDATE novels SET wordCount = :wordCount, updatedAt = :updatedAt WHERE id = :novelId")
    suspend fun updateWordCount(novelId: String, wordCount: Long, updatedAt: Long = System.currentTimeMillis())

    /**
     * 软删除小说
     */
    @Query("UPDATE novels SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :novelId")
    suspend fun softDelete(novelId: String, deletedAt: Long = System.currentTimeMillis())

    /**
     * 恢复已删除的小说
     */
    @Query("UPDATE novels SET isDeleted = 0, deletedAt = NULL WHERE id = :novelId")
    suspend fun restore(novelId: String)

    // ==================== 删除操作 ====================

    /**
     * 删除小说
     */
    @Delete
    suspend fun deleteNovel(novel: NovelEntity)

    /**
     * 根据ID删除小说
     */
    @Query("DELETE FROM novels WHERE id = :novelId")
    suspend fun deleteById(novelId: String)

    /**
     * 删除所有已过期的小说
     */
    @Query("DELETE FROM novels WHERE isDeleted = 1 AND deletedAt < :expireTime")
    suspend fun deleteExpiredNovels(expireTime: Long)

    /**
     * 清空所有小说（仅用于测试）
     */
    @Query("DELETE FROM novels")
    suspend fun deleteAll()

    // ==================== 事务操作 ====================

    /**
     * 创建小说并初始化第一章
     */
    @Transaction
    suspend fun createNovelWithFirstChapter(
        novel: NovelEntity,
        firstChapter: ChapterEntity
    ) {
        insertNovel(novel)
        // ChapterDao需要单独注入，这里仅作示例
    }
}
