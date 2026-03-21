package com.novelapp.aiagent.voice

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * VoiceCommandParser单元测试
 */
class VoiceCommandParserTest {

    // ==================== 新建相关测试 ====================

    @Test
    fun `parse CREATE_NOVEL with exact keyword`() {
        val result = VoiceCommandParser.parse("新建小说")

        assertTrue(result.isSuccess)
        assertEquals(VoiceCommandType.CREATE_NOVEL, result.command)
        assertEquals("新建小说", result.matchedKeyword)
    }

    @Test
    fun `parse CREATE_NOVEL with variant keywords`() {
        val variants = listOf("创建小说", "开始写作", "新建作品", "写一部小说")

        variants.forEach { text ->
            val result = VoiceCommandParser.parse(text)
            assertTrue("Failed for: $text", result.isSuccess)
            assertEquals(VoiceCommandType.CREATE_NOVEL, result.command)
        }
    }

    @Test
    fun `parse CREATE_NOVEL in longer sentence`() {
        val result = VoiceCommandParser.parse("我想新建小说可以吗")

        assertTrue(result.isSuccess)
        assertEquals(VoiceCommandType.CREATE_NOVEL, result.command)
    }

    // ==================== 删除相关测试 ====================

    @Test
    fun `parse DELETE_NOVEL with exact keyword`() {
        val result = VoiceCommandParser.parse("删除小说")

        assertTrue(result.isSuccess)
        assertEquals(VoiceCommandType.DELETE_NOVEL, result.command)
    }

    @Test
    fun `parse CONFIRM_DELETE with variants`() {
        val variants = listOf("确认删除", "是的删除", "确定删除", "是的删掉", "确认")

        variants.forEach { text ->
            val result = VoiceCommandParser.parse(text)
            assertTrue("Failed for: $text", result.isSuccess)
            assertEquals(VoiceCommandType.CONFIRM_DELETE, result.command)
        }
    }

    @Test
    fun `parse CANCEL_DELETE with variants`() {
        val variants = listOf("取消删除", "不删除", "取消", "不要删", "算了")

        variants.forEach { text ->
            val result = VoiceCommandParser.parse(text)
            assertTrue("Failed for: $text", result.isSuccess)
            assertEquals(VoiceCommandType.CANCEL_DELETE, result.command)
        }
    }

    // ==================== 创作相关测试 ====================

    @Test
    fun `parse START_WRITING with variants`() {
        val variants = listOf("写一段", "继续写", "生成内容", "帮我写", "接着写", "AI续写")

        variants.forEach { text ->
            val result = VoiceCommandParser.parse(text)
            assertTrue("Failed for: $text", result.isSuccess)
            assertEquals(VoiceCommandType.START_WRITING, result.command)
        }
    }

    @Test
    fun `parse UNDO with exact keyword`() {
        val result = VoiceCommandParser.parse("撤销")

        assertTrue(result.isSuccess)
        assertEquals(VoiceCommandType.UNDO, result.command)
    }

    @Test
    fun `parse SAVE with variants`() {
        val variants = listOf("保存", "存一下", "保存草稿", "存稿")

        variants.forEach { text ->
            val result = VoiceCommandParser.parse(text)
            assertTrue("Failed for: $text", result.isSuccess)
            assertEquals(VoiceCommandType.SAVE, result.command)
        }
    }

    // ==================== 导航相关测试 ====================

    @Test
    fun `parse GO_HOME with variants`() {
        val variants = listOf("返回首页", "回到主页", "返回主页", "去首页", "主页")

        variants.forEach { text ->
            val result = VoiceCommandParser.parse(text)
            assertTrue("Failed for: $text", result.isSuccess)
            assertEquals(VoiceCommandType.GO_HOME, result.command)
        }
    }

    @Test
    fun `parse NEXT_CHAPTER with exact keyword`() {
        val result = VoiceCommandParser.parse("下一章")

        assertTrue(result.isSuccess)
        assertEquals(VoiceCommandType.NEXT_CHAPTER, result.command)
    }

    // ==================== 无效输入测试 ====================

    @Test
    fun `parse with empty text returns null`() {
        val result = VoiceCommandParser.parse("")

        assertFalse(result.isSuccess)
        assertNull(result.command)
    }

    @Test
    fun `parse with blank text returns null`() {
        val result = VoiceCommandParser.parse("   ")

        assertFalse(result.isSuccess)
        assertNull(result.command)
    }

    @Test
    fun `parse with unrecognized text returns null`() {
        val result = VoiceCommandParser.parse("今天天气不错")

        assertFalse(result.isSuccess)
        assertNull(result.command)
    }

    // ==================== 置信度测试 ====================

    @Test
    fun `exact match has confidence 1`() {
        val result = VoiceCommandParser.parse("新建小说")

        assertEquals(1.0f, result.confidence, 0.01f)
    }

    @Test
    fun `prefix match has high confidence`() {
        val result = VoiceCommandParser.parse("新建小说请")

        assertTrue(result.confidence >= 0.9f)
    }

    // ==================== 工具方法测试 ====================

    @Test
    fun `isValidCommand returns true for valid commands`() {
        assertTrue(VoiceCommandParser.isValidCommand("新建小说"))
        assertTrue(VoiceCommandParser.isValidCommand("删除小说"))
        assertTrue(VoiceCommandParser.isValidCommand("保存"))
    }

    @Test
    fun `isValidCommand returns false for invalid commands`() {
        assertFalse(VoiceCommandParser.isValidCommand(""))
        assertFalse(VoiceCommandParser.isValidCommand("随便说说"))
    }

    @Test
    fun `getAllHints returns all commands`() {
        val hints = VoiceCommandParser.getAllHints()

        assertTrue(hints.isNotEmpty())
        assertTrue(hints.any { it.command == VoiceCommandType.CREATE_NOVEL })
        assertTrue(hints.any { it.command == VoiceCommandType.START_WRITING })
    }

    @Test
    fun `getHintsForScene HOME returns correct commands`() {
        val hints = VoiceCommandParser.getHintsForScene(VoiceScene.HOME)

        assertTrue(hints.any { it.command == VoiceCommandType.CREATE_NOVEL })
        assertTrue(hints.any { it.command == VoiceCommandType.ENTER_EDITOR })
        assertFalse(hints.any { it.command == VoiceCommandType.START_WRITING })
    }

    @Test
    fun `getHintsForScene EDITOR returns correct commands`() {
        val hints = VoiceCommandParser.getHintsForScene(VoiceScene.EDITOR)

        assertTrue(hints.any { it.command == VoiceCommandType.START_WRITING })
        assertTrue(hints.any { it.command == VoiceCommandType.SAVE })
        assertTrue(hints.any { it.command == VoiceCommandType.UNDO })
        assertFalse(hints.any { it.command == VoiceCommandType.CREATE_NOVEL })
    }

    @Test
    fun `getHintsForScene DELETE_DIALOG returns confirm and cancel`() {
        val hints = VoiceCommandParser.getHintsForScene(VoiceScene.DELETE_DIALOG)

        assertTrue(hints.any { it.command == VoiceCommandType.CONFIRM_DELETE })
        assertTrue(hints.any { it.command == VoiceCommandType.CANCEL_DELETE })
        assertEquals(2, hints.size)
    }
}
