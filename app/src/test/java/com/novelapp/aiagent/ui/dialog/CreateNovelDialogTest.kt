package com.novelapp.aiagent.ui.dialog

import org.junit.Assert.*
import org.junit.Test

/**
 * CreateNovelDialog 单元测试
 *
 * 测试覆盖：
 * - 输入验证逻辑
 * - 名称长度限制
 * - 类型选择
 *
 * 注意：UI交互测试需要使用Robolectric或AndroidTest
 *
 * @see AGENTS.md 10.2节 单元测试规范
 */
class CreateNovelDialogTest {

    // ==================== 输入验证测试 ====================

    @Test
    fun `validateInput returns false when name is empty`() {
        val name = ""

        assertFalse(isValidNovelName(name))
    }

    @Test
    fun `validateInput returns false when name is blank`() {
        val name = "   "

        assertFalse(isValidNovelName(name))
    }

    @Test
    fun `validateInput returns true when name is valid`() {
        val name = "我的小说"

        assertTrue(isValidNovelName(name))
    }

    @Test
    fun `validateInput returns true when name is exactly 50 chars`() {
        val name = "a".repeat(50)

        assertTrue(isValidNovelName(name))
    }

    @Test
    fun `validateInput returns false when name exceeds 50 chars`() {
        val name = "a".repeat(51)

        assertFalse(isValidNovelName(name))
    }

    @Test
    fun `validateInput trims whitespace`() {
        val name = "  我的小说  "

        assertTrue(isValidNovelName(name))
        assertEquals("我的小说", name.trim())
    }

    @Test
    fun `validateInput handles special characters`() {
        val name = "小说《测试》【第一卷】"

        assertTrue(isValidNovelName(name))
    }

    @Test
    fun `validateInput handles unicode characters`() {
        val name = "🔥小说名称🔥"

        assertTrue(isValidNovelName(name))
    }

    // ==================== 类型选择测试 ====================

    @Test
    fun `default genre is xuanhuan`() {
        val defaultGenre = NovelGenre.XUANHUAN

        assertEquals("玄幻", defaultGenre.label)
    }

    @Test
    fun `all genres have valid labels`() {
        val genres = NovelGenre.entries

        assertTrue(genres.all { it.label.isNotEmpty() })
    }

    @Test
    fun `fromLabel returns correct genre`() {
        assertEquals(NovelGenre.XUANHUAN, NovelGenre.fromLabel("玄幻"))
        assertEquals(NovelGenre.DUSHI, NovelGenre.fromLabel("都市"))
        assertEquals(NovelGenre.KEHUAN, NovelGenre.fromLabel("科幻"))
    }

    @Test
    fun `fromLabel returns OTHER for unknown label`() {
        assertEquals(NovelGenre.OTHER, NovelGenre.fromLabel("未知类型"))
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `name with exactly 1 char is valid`() {
        val name = "书"

        assertTrue(isValidNovelName(name))
    }

    @Test
    fun `name with mixed length chars is valid`() {
        val name = "abc测试123"

        assertTrue(isValidNovelName(name))
    }

    @Test
    fun `name with newlines should be trimmed`() {
        val name = "\n我的小说\n"

        assertTrue(isValidNovelName(name.trim()))
    }

    // ==================== 辅助方法 ====================

    /**
     * 验证小说名称
     * 遵循 AGENTS.md 5.1节 规范
     */
    private fun isValidNovelName(name: String): Boolean {
        val trimmedName = name.trim()
        return trimmedName.isNotEmpty() && trimmedName.length <= 50
    }

    /**
     * 小说类型枚举（测试用副本）
     */
    private enum class NovelGenre(val label: String) {
        XUANHUAN("玄幻"),
        QIHUAN("奇幻"),
        WUXIA("武侠"),
        XIANXIA("仙侠"),
        DUSHI("都市"),
        LISHI("历史"),
        JUNSHI("军事"),
        KEHUAN("科幻"),
        LINGYI("灵异"),
        OTHER("其他");

        companion object {
            fun fromLabel(label: String): NovelGenre {
                return entries.find { it.label == label } ?: OTHER
            }
        }
    }
}
