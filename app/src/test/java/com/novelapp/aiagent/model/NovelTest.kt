package com.novelapp.aiagent.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Novel模型单元测试
 */
class NovelTest {

    @Test
    fun `when create Novel then id is set`() {
        val novel = Novel(
            id = "test-id",
            title = "测试小说"
        )

        assertEquals("test-id", novel.id)
        assertEquals("测试小说", novel.title)
    }

    @Test
    fun `when wordCount >= 10000 then format as wan`() {
        val novel = Novel(
            id = "test-id",
            title = "测试小说",
            wordCount = 15000L
        )

        val formatted = novel.getFormattedWordCount()
        assertEquals("1.5万", formatted)
    }

    @Test
    fun `when wordCount < 10000 then format as number`() {
        val novel = Novel(
            id = "test-id",
            title = "测试小说",
            wordCount = 5000L
        )

        val formatted = novel.getFormattedWordCount()
        assertEquals("5000", formatted)
    }

    @Test
    fun `when deleted more than 7 days then isExpired returns true`() {
        val sevenDaysAgo = System.currentTimeMillis() - 8 * 24 * 60 * 60 * 1000L
        val novel = Novel(
            id = "test-id",
            title = "测试小说",
            isDeleted = true,
            deletedAt = sevenDaysAgo
        )

        assertTrue(novel.isExpired())
    }

    @Test
    fun `when deleted less than 7 days then isExpired returns false`() {
        val threeDaysAgo = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L
        val novel = Novel(
            id = "test-id",
            title = "测试小说",
            isDeleted = true,
            deletedAt = threeDaysAgo
        )

        assertFalse(novel.isExpired())
    }

    @Test
    fun `when not deleted then isExpired returns false`() {
        val novel = Novel(
            id = "test-id",
            title = "测试小说",
            isDeleted = false
        )

        assertFalse(novel.isExpired())
    }

    @Test
    fun `when update time is recent then show relative time`() {
        val oneHourAgo = System.currentTimeMillis() - 60 * 60 * 1000L
        val novel = Novel(
            id = "test-id",
            title = "测试小说",
            updatedAt = oneHourAgo
        )

        val formatted = novel.getFormattedUpdateTime()
        assertTrue(formatted.contains("小时前"))
    }

    @Test
    fun `NovelGenre fromLabel returns correct genre`() {
        assertEquals(NovelGenre.XUANHUAN, NovelGenre.fromLabel("玄幻"))
        assertEquals(NovelGenre.DUSHI, NovelGenre.fromLabel("都市"))
        assertEquals(NovelGenre.OTHER, NovelGenre.fromLabel("未知类型"))
    }
}
