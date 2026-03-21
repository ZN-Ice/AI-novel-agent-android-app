package com.novelapp.aiagent.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Chapter模型单元测试
 */
class ChapterTest {

    @Test
    fun `when create Chapter then properties are set`() {
        val chapter = Chapter(
            id = "chapter-id",
            novelId = "novel-id",
            title = "第一章 开始",
            content = "这是章节内容",
            chapterNumber = 1
        )

        assertEquals("chapter-id", chapter.id)
        assertEquals("novel-id", chapter.novelId)
        assertEquals("第一章 开始", chapter.title)
        assertEquals(1, chapter.chapterNumber)
    }

    @Test
    fun `calculateWordCount returns correct count for Chinese`() {
        val chapter = Chapter(
            id = "chapter-id",
            novelId = "novel-id",
            title = "测试章节",
            content = "这是一段中文内容，共十二个字。"
        )

        val wordCount = chapter.calculateWordCount()
        assertEquals(13, wordCount) // 13个中文字符
    }

    @Test
    fun `getFormattedWordCount shows wan for large count`() {
        val chapter = Chapter(
            id = "chapter-id",
            novelId = "novel-id",
            title = "测试章节",
            wordCount = 15000
        )

        val formatted = chapter.getFormattedWordCount()
        assertEquals("1.5万字", formatted)
    }

    @Test
    fun `getFormattedWordCount shows zi for small count`() {
        val chapter = Chapter(
            id = "chapter-id",
            novelId = "novel-id",
            title = "测试章节",
            wordCount = 500
        )

        val formatted = chapter.getFormattedWordCount()
        assertEquals("500字", formatted)
    }

    @Test
    fun `getSummary truncates long content`() {
        val longContent = "这是一段很长的内容。".repeat(20)
        val chapter = Chapter(
            id = "chapter-id",
            novelId = "novel-id",
            title = "测试章节",
            content = longContent
        )

        val summary = chapter.getSummary(50)
        assertTrue(summary.length <= 53) // 50 + "..."
        assertTrue(summary.endsWith("..."))
    }

    @Test
    fun `getSummary returns full content when short`() {
        val shortContent = "这是短内容"
        val chapter = Chapter(
            id = "chapter-id",
            novelId = "novel-id",
            title = "测试章节",
            content = shortContent
        )

        val summary = chapter.getSummary(100)
        assertEquals(shortContent, summary)
    }
}
