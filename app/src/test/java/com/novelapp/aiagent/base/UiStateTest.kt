package com.novelapp.aiagent.base

import org.junit.Assert.*
import org.junit.Test

/**
 * UiState单元测试
 */
class UiStateTest {

    // ==================== 状态类型测试 ====================

    @Test
    fun `Idle state has correct properties`() {
        val state = UiState.Idle

        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isError)
        assertNull(state.getDataOrNull())
        assertNull(state.getErrorOrNull())
    }

    @Test
    fun `Loading state has correct properties`() {
        val state: UiState<Nothing> = UiState.Loading

        assertTrue(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isError)
    }

    @Test
    fun `Success state has correct properties`() {
        val data = listOf("item1", "item2")
        val state = UiState.Success(data)

        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
        assertFalse(state.isError)
        assertEquals(data, state.getDataOrNull())
        assertNull(state.getErrorOrNull())
    }

    @Test
    fun `Error state has correct properties`() {
        val error = Exception("测试错误")
        val state: UiState<Nothing> = UiState.Error(error)

        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertTrue(state.isError)
        assertNull(state.getDataOrNull())
        assertEquals(error, state.getErrorOrNull())
    }

    @Test
    fun `Empty state has correct properties`() {
        val state: UiState<Nothing> = UiState.Empty

        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isError)
    }

    // ==================== 工厂方法测试 ====================

    @Test
    fun `success factory creates Success state`() {
        val data = "test data"
        val state = UiState.success(data)

        assertTrue(state.isSuccess)
        assertEquals(data, state.getDataOrNull())
    }

    @Test
    fun `error factory with Throwable creates Error state`() {
        val error = RuntimeException("test error")
        val state = UiState.error(error)

        assertTrue(state.isError)
        assertEquals(error, state.getErrorOrNull())
    }

    @Test
    fun `error factory with message creates Error state`() {
        val state = UiState.error("error message")

        assertTrue(state.isError)
        assertEquals("error message", state.getErrorOrNull()?.message)
    }

    @Test
    fun `loading factory creates Loading state`() {
        val state = UiState.loading()

        assertTrue(state.isLoading)
    }

    // ==================== PageState测试 ====================

    @Test
    fun `PageState Refreshing isLoading is true`() {
        val state: PageState<List<String>> = PageState.Refreshing()

        assertTrue(state.isLoading)
    }

    @Test
    fun `PageState LoadingMore isLoading is true`() {
        val state: PageState<List<String>> = PageState.LoadingMore(emptyList())

        assertTrue(state.isLoading)
    }

    @Test
    fun `PageState Success has correct properties`() {
        val data = listOf("item1", "item2")
        val state = PageState.Success(data, hasMore = true)

        assertFalse(state.isLoading)
        assertFalse(state.isError)
        assertEquals(data, state.data)
        assertTrue(state.hasMore)
    }

    @Test
    fun `PageState Error has correct properties`() {
        val error = Exception("加载失败")
        val state: PageState<List<String>> = PageState.Error(error)

        assertTrue(state.isError)
        assertFalse(state.isLoading)
    }

    @Test
    fun `PageState Empty isLoading is false`() {
        val state: PageState<List<String>> = PageState.Empty()

        assertFalse(state.isLoading)
        assertFalse(state.isError)
    }
}
