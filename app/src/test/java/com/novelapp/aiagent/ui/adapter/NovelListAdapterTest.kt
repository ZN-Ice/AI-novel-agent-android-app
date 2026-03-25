package com.novelapp.aiagent.ui.adapter

import com.novelapp.aiagent.model.Novel
import com.novelapp.aiagent.model.NovelStatus
import com.novelapp.aiagent.ui.home.NovelListAdapter
import org.junit.Assert.*
import org.junit.Test

/**
 * NovelListAdapter 单元测试
 *
 * 测试覆盖：
 * - DiffUtil 比较逻辑
 * - 数据类比较
 * - 字数格式化
 * - 时间格式化
 *
 * @see AGENTS.md 10.2节 单元测试规范
 */
class NovelListAdapterTest {

    // ==================== NovelDiffCallback测试 ====================

    @Test
    fun `when items have same id then areItemsTheSame returns true`() {
        val diffCallback = NovelListAdapter.NovelDiffCallback()
        val novel1 = createTestNovel(id = "1", title = "小说A")
        val novel2 = createTestNovel(id = "1", title = "小说B")

        assertTrue(diffCallback.areItemsTheSame(novel1, novel2))
    }

    @Test
    fun `when items have different id then areItemsTheSame returns false`() {
        val diffCallback = NovelListAdapter.NovelDiffCallback()
        val novel1 = createTestNovel(id = "1", title = "小说A")
        val novel2 = createTestNovel(id = "2", title = "小说A")

        assertFalse(diffCallback.areItemsTheSame(novel1, novel2))
    }

    @Test
    fun `when items are identical then areContentsTheSame returns true`() {
        val diffCallback = NovelListAdapter.NovelDiffCallback()
        val novel1 = createTestNovel(id = "1", title = "小说A")
        val novel2 = createTestNovel(id = "1", title = "小说A")

        assertTrue(diffCallback.areContentsTheSame(novel1, novel2))
    }

    @Test
    fun `when items have different content then areContentsTheSame returns false`() {
        val diffCallback = NovelListAdapter.NovelDiffCallback()
        val novel1 = createTestNovel(id = "1", title = "小说A", wordCount = 1000)
        val novel2 = createTestNovel(id = "1", title = "小说A", wordCount = 2000)

        assertFalse(diffCallback.areContentsTheSame(novel1, novel2))
    }

    @Test
    fun `when title differs then areContentsTheSame returns false`() {
        val diffCallback = NovelListAdapter.NovelDiffCallback()
        val novel1 = createTestNovel(id = "1", title = "小说A")
        val novel2 = createTestNovel(id = "1", title = "小说B")

        assertFalse(diffCallback.areContentsTheSame(novel1, novel2))
    }

    @Test
    fun `when genre differs then areContentsTheSame returns false`() {
        val diffCallback = NovelListAdapter.NovelDiffCallback()
        val novel1 = createTestNovel(id = "1", genre = "玄幻")
        val novel2 = createTestNovel(id = "1", genre = "都市")

        assertFalse(diffCallback.areContentsTheSame(novel1, novel2))
    }

    @Test
    fun `when chapterCount differs then areContentsTheSame returns false`() {
        val diffCallback = NovelListAdapter.NovelDiffCallback()
        val novel1 = createTestNovel(id = "1", chapterCount = 10)
        val novel2 = createTestNovel(id = "1", chapterCount = 20)

        assertFalse(diffCallback.areContentsTheSame(novel1, novel2))
    }

    @Test
    fun `when status differs then areContentsTheSame returns false`() {
        val diffCallback = NovelListAdapter.NovelDiffCallback()
        val novel1 = createTestNovel(id = "1", status = NovelStatus.ONGOING)
        val novel2 = createTestNovel(id = "1", status = NovelStatus.COMPLETED)

        assertFalse(diffCallback.areContentsTheSame(novel1, novel2))
    }

    // ==================== Novel数据类测试 ====================

    @Test
    fun `Novel data class equals works correctly`() {
        val fixedTime = System.currentTimeMillis()
        val novel1 = createTestNovelWithFixedTime(id = "1", title = "测试小说", fixedTime = fixedTime)
        val novel2 = createTestNovelWithFixedTime(id = "1", title = "测试小说", fixedTime = fixedTime)

        assertEquals(novel1, novel2)
    }

    @Test
    fun `Novel data class copy works correctly`() {
        val original = createTestNovel(id = "1", title = "原标题")
        val copied = original.copy(title = "新标题")

        assertEquals("新标题", copied.title)
        assertEquals(original.id, copied.id)
    }

    @Test
    fun `Novel default values are correct`() {
        val novel = Novel(id = "1", title = "测试")

        assertEquals("", novel.genre)
        assertEquals("", novel.description)
        assertEquals(0, novel.chapterCount)
        assertEquals(0L, novel.wordCount)
        assertEquals(NovelStatus.DRAFT, novel.status)
        assertFalse(novel.isDeleted)
    }

    // ==================== 字数格式化测试 ====================

    @Test
    fun `Novel getFormattedWordCount for less than 10000`() {
        val novel = createTestNovel(wordCount = 5000)

        assertEquals("5000", novel.getFormattedWordCount())
    }

    @Test
    fun `Novel getFormattedWordCount for exactly 10000`() {
        val novel = createTestNovel(wordCount = 10000)

        assertEquals("1.0万", novel.getFormattedWordCount())
    }

    @Test
    fun `Novel getFormattedWordCount for 15000`() {
        val novel = createTestNovel(wordCount = 15000)

        assertEquals("1.5万", novel.getFormattedWordCount())
    }

    @Test
    fun `Novel getFormattedWordCount for 123456`() {
        val novel = createTestNovel(wordCount = 123456)

        assertEquals("12.3万", novel.getFormattedWordCount())
    }

    // ==================== 软删除测试 ====================

    @Test
    fun `Novel isExpired returns false when not deleted`() {
        val novel = createTestNovel(isDeleted = false, deletedAt = null)

        assertFalse(novel.isExpired())
    }

    @Test
    fun `Novel isExpired returns false when recently deleted`() {
        val recentDeletedAt = System.currentTimeMillis() - (6 * 24 * 60 * 60 * 1000L) // 6天前
        val novel = createTestNovel(isDeleted = true, deletedAt = recentDeletedAt)

        assertFalse(novel.isExpired())
    }

    @Test
    fun `Novel isExpired returns true when deleted more than 7 days`() {
        val oldDeletedAt = System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000L) // 8天前
        val novel = createTestNovel(isDeleted = true, deletedAt = oldDeletedAt)

        assertTrue(novel.isExpired())
    }

    @Test
    fun `Novel isExpired returns true when deleted exactly 7 days`() {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L + 1000) // 7天+1秒
        val novel = createTestNovel(isDeleted = true, deletedAt = sevenDaysAgo)

        assertTrue(novel.isExpired())
    }

    // ==================== 辅助方法 ====================

    private fun createTestNovel(
        id: String = "test-id",
        title: String = "测试小说",
        genre: String = "玄幻",
        chapterCount: Int = 10,
        wordCount: Long = 10000,
        status: NovelStatus = NovelStatus.ONGOING,
        isDeleted: Boolean = false,
        deletedAt: Long? = null
    ): Novel {
        return Novel(
            id = id,
            title = title,
            genre = genre,
            chapterCount = chapterCount,
            wordCount = wordCount,
            status = status,
            createdAt = System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000L),
            updatedAt = System.currentTimeMillis(),
            isDeleted = isDeleted,
            deletedAt = deletedAt
        )
    }

    private fun createTestNovelWithFixedTime(
        id: String = "test-id",
        title: String = "测试小说",
        genre: String = "玄幻",
        chapterCount: Int = 10,
        wordCount: Long = 10000,
        status: NovelStatus = NovelStatus.ONGOING,
        isDeleted: Boolean = false,
        deletedAt: Long? = null,
        fixedTime: Long = System.currentTimeMillis()
    ): Novel {
        return Novel(
            id = id,
            title = title,
            genre = genre,
            chapterCount = chapterCount,
            wordCount = wordCount,
            status = status,
            createdAt = fixedTime - (10 * 24 * 60 * 60 * 1000L),
            updatedAt = fixedTime,
            isDeleted = isDeleted,
            deletedAt = deletedAt
        )
    }
}
