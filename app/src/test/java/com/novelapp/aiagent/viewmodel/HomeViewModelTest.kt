package com.novelapp.aiagent.viewmodel

import org.junit.Assert.*
import org.junit.Test

/**
 * HomeViewModel单元测试
 *
 * 注意：实际测试需要使用MockK和测试协程
 */
class HomeViewModelTest {

    // ==================== HomeUiState测试 ====================

    @Test
    fun `HomeUiState Loading isLoading is true`() {
        val state = HomeUiState.Loading

        assertTrue(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isEmpty)
        assertFalse(state.isError)
    }

    @Test
    fun `HomeUiState Success isSuccess is true`() {
        val state = HomeUiState.Success(emptyList())

        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
        assertFalse(state.isEmpty)
        assertFalse(state.isError)
    }

    @Test
    fun `HomeUiState Empty isEmpty is true`() {
        val state = HomeUiState.Empty

        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertTrue(state.isEmpty)
        assertFalse(state.isError)
    }

    @Test
    fun `HomeUiState Error isError is true`() {
        val state = HomeUiState.Error("错误消息")

        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isEmpty)
        assertTrue(state.isError)
    }

    // ==================== NovelStats测试 ====================

    @Test
    fun `NovelStats calculates correctly`() {
        val stats = NovelStats(
            totalCount = 10,
            ongoingCount = 6,
            completedCount = 4,
            totalWords = 50000
        )

        assertEquals(10, stats.totalCount)
        assertEquals(6, stats.ongoingCount)
        assertEquals(4, stats.completedCount)
        assertEquals(50000L, stats.totalWords)
    }

    @Test
    fun `NovelStats getFormattedTotalWords for large numbers`() {
        val stats = NovelStats(
            totalCount = 1,
            ongoingCount = 1,
            completedCount = 0,
            totalWords = 15000
        )

        val formatted = stats.getFormattedTotalWords()

        assertEquals("1.5万", formatted)
    }

    @Test
    fun `NovelStats getFormattedTotalWords for small numbers`() {
        val stats = NovelStats(
            totalCount = 1,
            ongoingCount = 1,
            completedCount = 0,
            totalWords = 5000
        )

        val formatted = stats.getFormattedTotalWords()

        assertEquals("5000", formatted)
    }

    @Test
    fun `NovelStats getFormattedTotalWords for exactly 10000`() {
        val stats = NovelStats(
            totalCount = 1,
            ongoingCount = 1,
            completedCount = 0,
            totalWords = 10000
        )

        val formatted = stats.getFormattedTotalWords()

        assertEquals("1.0万", formatted)
    }
}
