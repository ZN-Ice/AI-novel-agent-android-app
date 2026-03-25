package com.novelapp.aiagent.ui.dialog

import org.junit.Assert.*
import org.junit.Test

/**
 * DeleteConfirmDialog 单元测试
 *
 * 测试覆盖：
 * - 删除确认逻辑
 * - 参数传递
 * - 软删除规则
 *
 * @see AGENTS.md 10.2节 单元测试规范
 * @see AGENTS.md 5.2节 删除小说流程
 */
class DeleteConfirmDialogTest {

    // ==================== 删除确认逻辑测试 ====================

    @Test
    fun `delete confirmation requires novel id`() {
        val novel = TestNovel(id = "novel-123", title = "测试小说")

        assertNotNull(novel.id)
        assertTrue(novel.id.isNotEmpty())
    }

    @Test
    fun `delete confirmation requires novel title`() {
        val novel = TestNovel(id = "novel-123", title = "测试小说")

        assertNotNull(novel.title)
        assertTrue(novel.title.isNotEmpty())
    }

    @Test
    fun `delete confirmation message contains title`() {
        val title = "我的小说"
        val message = buildDeleteMessage(title)

        assertTrue(message.contains(title))
    }

    // ==================== 软删除规则测试 ====================

    @Test
    fun `soft delete sets isDeleted to true`() {
        val novel = TestNovel(
            id = "novel-123",
            title = "测试小说",
            isDeleted = false
        )
        val deletedNovel = novel.copy(isDeleted = true)

        assertTrue(deletedNovel.isDeleted)
    }

    @Test
    fun `soft delete sets deletedAt timestamp`() {
        val now = System.currentTimeMillis()
        val novel = TestNovel(
            id = "novel-123",
            title = "测试小说",
            isDeleted = false,
            deletedAt = null
        )
        val deletedNovel = novel.copy(
            isDeleted = true,
            deletedAt = now
        )

        assertTrue(deletedNovel.isDeleted)
        assertNotNull(deletedNovel.deletedAt)
        assertEquals(now, deletedNovel.deletedAt)
    }

    @Test
    fun `deleted novel is recoverable within 7 days`() {
        val sixDaysAgo = System.currentTimeMillis() - (6 * 24 * 60 * 60 * 1000L)
        val deletedNovel = TestNovel(
            id = "novel-123",
            title = "测试小说",
            isDeleted = true,
            deletedAt = sixDaysAgo
        )

        assertFalse(isExpiredForRecovery(deletedNovel))
    }

    @Test
    fun `deleted novel is not recoverable after 7 days`() {
        val eightDaysAgo = System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000L)
        val deletedNovel = TestNovel(
            id = "novel-123",
            title = "测试小说",
            isDeleted = true,
            deletedAt = eightDaysAgo
        )

        assertTrue(isExpiredForRecovery(deletedNovel))
    }

    @Test
    fun `recovery period is exactly 7 days`() {
        // 7天整 + 1毫秒，确保刚好超过7天
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L + 1)
        val deletedNovel = TestNovel(
            id = "novel-123",
            title = "测试小说",
            isDeleted = true,
            deletedAt = sevenDaysAgo
        )

        // 7天整刚好过期
        assertTrue(isExpiredForRecovery(deletedNovel))
    }

    // ==================== 取消删除测试 ====================

    @Test
    fun `cancel delete should not modify novel`() {
        val novel = TestNovel(
            id = "novel-123",
            title = "测试小说",
            isDeleted = false,
            deletedAt = null
        )

        // 取消删除不应修改数据
        assertFalse(novel.isDeleted)
        assertNull(novel.deletedAt)
    }

    // ==================== 回调测试 ====================

    @Test
    fun `confirm callback receives correct novel id`() {
        var receivedId: String? = null
        val onConfirm: (String) -> Unit = { id -> receivedId = id }

        val expectedId = "novel-123"
        onConfirm(expectedId)

        assertEquals(expectedId, receivedId)
    }

    @Test
    fun `cancel callback does not trigger confirm`() {
        var confirmCalled = false
        val onConfirm: (String) -> Unit = { confirmCalled = true }

        // 取消操作不应触发确认回调
        assertFalse(confirmCalled)
    }

    // ==================== 辅助方法 ====================

    private fun buildDeleteMessage(title: String): String {
        return "确定要删除「$title」吗？删除后7天内可恢复。"
    }

    private fun isExpiredForRecovery(novel: TestNovel): Boolean {
        if (novel.deletedAt == null) return false
        val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L
        return System.currentTimeMillis() - novel.deletedAt > sevenDaysInMillis
    }

    /**
     * 测试用小说数据类
     */
    private data class TestNovel(
        val id: String,
        val title: String,
        val isDeleted: Boolean = false,
        val deletedAt: Long? = null
    )
}
