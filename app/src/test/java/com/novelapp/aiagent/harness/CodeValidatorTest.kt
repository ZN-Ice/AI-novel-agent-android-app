package com.novelapp.aiagent.harness

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * CodeValidator单元测试
 */
class CodeValidatorTest {

    private lateinit var validator: CodeValidator

    @Before
    fun setup() {
        validator = CodeValidator()
    }

    // ==================== 小说名称校验测试 ====================

    @Test
    fun `validateNovelName with valid name returns Success`() {
        val result = validator.validateNovelName("我的玄幻小说")

        assertTrue(result.isSuccess)
        assertFalse(result.isError)
    }

    @Test
    fun `validateNovelName with empty name returns Error`() {
        val result = validator.validateNovelName("")

        assertTrue(result.isError)
        assertTrue((result as ValidationResult.Error).messages.contains("小说名称不能为空"))
    }

    @Test
    fun `validateNovelName with blank name returns Error`() {
        val result = validator.validateNovelName("   ")

        assertTrue(result.isError)
    }

    @Test
    fun `validateNovelName with too long name returns Error`() {
        val longName = "a".repeat(51)
        val result = validator.validateNovelName(longName)

        assertTrue(result.isError)
        val error = result as ValidationResult.Error
        assertTrue(error.messages.any { it.contains("不能超过") })
    }

    @Test
    fun `validateNovelName with invalid chars returns Error`() {
        val result = validator.validateNovelName("小说<名称>")

        assertTrue(result.isError)
        val error = result as ValidationResult.Error
        assertTrue(error.messages.any { it.contains("特殊字符") })
    }

    @Test
    fun `validateNovelName with max length returns Success`() {
        val maxLengthName = "a".repeat(50)
        val result = validator.validateNovelName(maxLengthName)

        assertTrue(result.isSuccess)
    }

    // ==================== 章节标题校验测试 ====================

    @Test
    fun `validateChapterTitle with valid title returns Success`() {
        val result = validator.validateChapterTitle("第一章 天地初开")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateChapterTitle with empty title returns Error`() {
        val result = validator.validateChapterTitle("")

        assertTrue(result.isError)
    }

    @Test
    fun `validateChapterTitle with too long title returns Error`() {
        val longTitle = "章".repeat(101)
        val result = validator.validateChapterTitle(longTitle)

        assertTrue(result.isError)
    }

    // ==================== 章节内容校验测试 ====================

    @Test
    fun `validateChapterContent with valid content returns Success`() {
        val content = "这是一段正常的小说内容。".repeat(100)
        val result = validator.validateChapterContent(content)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateChapterContent with too long content returns Error`() {
        val longContent = "内".repeat(100001)
        val result = validator.validateChapterContent(longContent)

        assertTrue(result.isError)
    }

    // ==================== API密钥校验测试 ====================

    @Test
    fun `validateApiKey with valid key returns Success`() {
        val result = validator.validateApiKey("sk-abcdefghijklmnopqrstuvwxyz123456")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateApiKey with empty key returns Error`() {
        val result = validator.validateApiKey("")

        assertTrue(result.isError)
    }

    @Test
    fun `validateApiKey with weak key returns Error`() {
        val result = validator.validateApiKey("test")

        assertTrue(result.isError)
    }

    @Test
    fun `validateApiKey with short key returns Error`() {
        val result = validator.validateApiKey("short_key")

        assertTrue(result.isError)
    }

    // ==================== ValidationResult测试 ====================

    @Test
    fun `ValidationResult Error firstMessage returns first error`() {
        val result = ValidationResult.Error(listOf("错误1", "错误2"))

        assertEquals("错误1", result.firstMessage)
    }

    @Test
    fun `ValidationResult Error firstMessage with empty list returns default`() {
        val result = ValidationResult.Error(emptyList())

        assertEquals("校验失败", result.firstMessage)
    }
}
