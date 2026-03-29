package com.novelapp.aiagent.utils

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * NovelNameValidator 单元测试
 *
 * 测试覆盖：
 * - 空名称验证
 * - 长度限制验证
 * - 重名检查
 * - 特殊字符处理
 * - 边界条件
 *
 * @see AGENTS.md 10.2节 单元测试规范
 */
class NovelNameValidatorTest {

    private lateinit var validator: NovelNameValidator

    @Before
    fun setup() {
        validator = NovelNameValidator()
    }

    // ==================== 空名称验证 ====================

    @Test
    fun `when name is empty then validation fails with empty error`() {
        val result = validator.validate("")

        assertFalse(result.isValid)
        assertEquals(ValidationError.EMPTY, result.error)
    }

    @Test
    fun `when name is blank spaces only then validation fails with empty error`() {
        val result = validator.validate("   ")

        assertFalse(result.isValid)
        assertEquals(ValidationError.EMPTY, result.error)
    }

    @Test
    fun `when name is blank with tabs and newlines then validation fails`() {
        val result = validator.validate("\t\n ")

        assertFalse(result.isValid)
        assertEquals(ValidationError.EMPTY, result.error)
    }

    // ==================== 长度限制验证 ====================

    @Test
    fun `when name exceeds 50 chars then validation fails with too long error`() {
        val name = "a".repeat(51)

        val result = validator.validate(name)

        assertFalse(result.isValid)
        assertEquals(ValidationError.TOO_LONG, result.error)
    }

    @Test
    fun `when name is exactly 50 chars then validation passes`() {
        val name = "a".repeat(50)

        val result = validator.validate(name)

        assertTrue(result.isValid)
        assertNull(result.error)
    }

    @Test
    fun `when name is exactly 51 chars then validation fails`() {
        val name = "a".repeat(51)

        val result = validator.validate(name)

        assertFalse(result.isValid)
    }

    // ==================== 重名检查 ====================

    @Test
    fun `when name is duplicate then validation fails with duplicate error`() {
        val existingNames = listOf("我的小说", "测试小说", "玄幻世界")
        validator.setExistingNames(existingNames)

        val result = validator.validate("我的小说")

        assertFalse(result.isValid)
        assertEquals(ValidationError.DUPLICATE, result.error)
    }

    @Test
    fun `when name is not duplicate then validation passes`() {
        val existingNames = listOf("我的小说", "测试小说")
        validator.setExistingNames(existingNames)

        val result = validator.validate("新小说")

        assertTrue(result.isValid)
    }

    @Test
    fun `when duplicate check is case sensitive then different case passes`() {
        val existingNames = listOf("Test Novel")
        validator.setExistingNames(existingNames)

        val result = validator.validate("test novel")

        assertTrue(result.isValid)
    }

    @Test
    fun `when name matches after trim then validation fails as duplicate`() {
        val existingNames = listOf("我的小说")
        validator.setExistingNames(existingNames)

        val result = validator.validate("  我的小说  ")

        assertFalse(result.isValid)
        assertEquals(ValidationError.DUPLICATE, result.error)
    }

    @Test
    fun `when existing names is empty then no duplicate check needed`() {
        validator.setExistingNames(emptyList())

        val result = validator.validate("新小说")

        assertTrue(result.isValid)
    }

    // ==================== 正常名称验证 ====================

    @Test
    fun `when name is valid chinese then validation passes`() {
        val result = validator.validate("我的玄幻小说")

        assertTrue(result.isValid)
        assertNull(result.error)
    }

    @Test
    fun `when name has special characters then validation passes`() {
        val result = validator.validate("小说《测试》【第一卷】")

        assertTrue(result.isValid)
    }

    @Test
    fun `when name has unicode emoji then validation passes`() {
        val result = validator.validate("🔥小说名称🔥")

        assertTrue(result.isValid)
    }

    @Test
    fun `when name is single character then validation passes`() {
        val result = validator.validate("书")

        assertTrue(result.isValid)
    }

    @Test
    fun `when name has mixed languages then validation passes`() {
        val result = validator.validate("abc测试123")

        assertTrue(result.isValid)
    }

    @Test
    fun `when name has spaces in middle then validation passes`() {
        val result = validator.validate("我的 玄幻 小说")

        assertTrue(result.isValid)
    }

    // ==================== 边界条件 ====================

    @Test
    fun `when name is 50 unicode chars then validation passes`() {
        val name = "啊".repeat(50)

        val result = validator.validate(name)

        assertTrue(result.isValid)
    }

    @Test
    fun `when name is 51 unicode chars then validation fails`() {
        val name = "啊".repeat(51)

        val result = validator.validate(name)

        assertFalse(result.isValid)
        assertEquals(ValidationError.TOO_LONG, result.error)
    }

    // ==================== trim处理 ====================

    @Test
    fun `when name has leading and trailing spaces then trimmed name is returned`() {
        val result = validator.validate("  我的小说  ")

        assertTrue(result.isValid)
        assertEquals("我的小说", result.trimmedName)
    }

    @Test
    fun `when name has newlines then trimmed name excludes them`() {
        val result = validator.validate("\n我的小说\n")

        assertTrue(result.isValid)
        assertEquals("我的小说", result.trimmedName)
    }

    // ==================== ValidationResult ====================

    @Test
    fun `validation success result has correct properties`() {
        val result = validator.validate("有效名称")

        assertTrue(result.isValid)
        assertNull(result.error)
        assertEquals("有效名称", result.trimmedName)
    }

    @Test
    fun `validation failure result has error message`() {
        val result = validator.validate("")

        assertFalse(result.isValid)
        assertNotNull(result.error)
        assertFalse(result.errorMessage.isEmpty())
    }

    // ==================== 错误消息 ====================

    @Test
    fun `EMPTY error has meaningful message`() {
        val result = validator.validate("")

        assertEquals("请输入小说名称", result.errorMessage)
    }

    @Test
    fun `TOO_LONG error has meaningful message`() {
        val result = validator.validate("a".repeat(51))

        assertEquals("小说名称不能超过50个字符", result.errorMessage)
    }

    @Test
    fun `DUPLICATE error has meaningful message`() {
        validator.setExistingNames(listOf("已存在小说"))

        val result = validator.validate("已存在小说")

        assertEquals("已存在同名小说", result.errorMessage)
    }

    // ==================== 优先级：空 > 过长 > 重名 ====================

    @Test
    fun `when name is empty and also duplicate then empty error takes priority`() {
        validator.setExistingNames(listOf(""))

        val result = validator.validate("")

        assertEquals(ValidationError.EMPTY, result.error)
    }

    @Test
    fun `when name is too long and also duplicate then too long error takes priority`() {
        val longName = "a".repeat(51)
        validator.setExistingNames(listOf(longName))

        val result = validator.validate(longName)

        assertEquals(ValidationError.TOO_LONG, result.error)
    }
}
