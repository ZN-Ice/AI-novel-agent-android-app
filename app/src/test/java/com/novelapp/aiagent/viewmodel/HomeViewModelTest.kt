package com.novelapp.aiagent.viewmodel

import app.cash.turbine.test
import com.novelapp.aiagent.data.repository.NovelRepository
import com.novelapp.aiagent.model.Novel
import com.novelapp.aiagent.model.NovelStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * HomeViewModel 单元测试
 *
 * 测试覆盖：
 * - UI状态管理（Loading/Success/Empty/Error）
 * - 小说列表加载
 * - 创建小说流程
 * - 删除小说流程
 * - 搜索功能
 * - 统计信息计算
 *
 * @see AGENTS.md 10.2节 单元测试规范
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    // 测试调度器
    private val testDispatcher = StandardTestDispatcher()

    // Mock依赖
    private val novelRepository: NovelRepository = mockk()

    // 被测对象
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== 初始化测试 ====================

    @Test
    fun `when init then load novels`() = runTest {
        // Given
        val novels = listOf(createTestNovel())
        coEvery { novelRepository.getAllNovels() } returns flowOf(novels)

        // When
        viewModel = HomeViewModel(novelRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { novelRepository.getAllNovels() }
    }

    @Test
    fun `when load novels success then state is Success`() = runTest {
        // Given
        val novels = listOf(createTestNovel())
        coEvery { novelRepository.getAllNovels() } returns flowOf(novels)

        // When
        viewModel = HomeViewModel(novelRepository)

        // Then - 验证最终状态是 Success
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue("Expected Success but got $state", state is HomeUiState.Success)
        assertEquals(novels, (state as HomeUiState.Success).novels)
    }

    @Test
    fun `when load novels empty then state is Empty`() = runTest {
        // Given
        coEvery { novelRepository.getAllNovels() } returns flowOf(emptyList())

        // When
        viewModel = HomeViewModel(novelRepository)

        // Then - 验证最终状态是 Empty
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue("Expected Empty but got $state", state is HomeUiState.Empty)
    }

    // ==================== 创建小说测试 ====================

    @Test
    fun `when create novel success then show toast`() = runTest {
        // Given
        val novel = createTestNovel()
        coEvery { novelRepository.getAllNovels() } returns flowOf(listOf(novel))
        coEvery { novelRepository.createNovel(any(), any()) } returns Result.success(novel)

        viewModel = HomeViewModel(novelRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.createNovel("测试小说", "玄幻")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { novelRepository.createNovel("测试小说", "玄幻") }
    }

    @Test
    fun `when create novel fails then show error`() = runTest {
        // Given
        coEvery { novelRepository.getAllNovels() } returns flowOf(emptyList())
        coEvery { novelRepository.createNovel(any(), any()) } returns Result.failure(Exception("创建失败"))

        viewModel = HomeViewModel(novelRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.createNovel("测试小说", "玄幻")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { novelRepository.createNovel("测试小说", "玄幻") }
    }

    // ==================== 删除小说测试 ====================

    @Test
    fun `when show delete confirmation then update state`() = runTest {
        // Given
        val novel = createTestNovel()
        coEvery { novelRepository.getAllNovels() } returns flowOf(listOf(novel))

        viewModel = HomeViewModel(novelRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.showDeleteConfirmation(novel)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(novel, viewModel.selectedNovel.value)
        assertTrue(viewModel.showDeleteDialog.value)
    }

    @Test
    fun `when hide delete confirmation then clear state`() = runTest {
        // Given
        val novel = createTestNovel()
        coEvery { novelRepository.getAllNovels() } returns flowOf(listOf(novel))

        viewModel = HomeViewModel(novelRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showDeleteConfirmation(novel)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.hideDeleteConfirmation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertNull(viewModel.selectedNovel.value)
        assertFalse(viewModel.showDeleteDialog.value)
    }

    @Test
    fun `when confirm delete then call repository`() = runTest {
        // Given
        val novel = createTestNovel()
        coEvery { novelRepository.getAllNovels() } returns flowOf(listOf(novel))
        coEvery { novelRepository.deleteNovel(any()) } returns Result.success(Unit)

        viewModel = HomeViewModel(novelRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showDeleteConfirmation(novel)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.confirmDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { novelRepository.deleteNovel(novel.id) }
    }

    // ==================== 搜索测试 ====================

    @Test
    fun `when search with keyword then call repository search`() = runTest {
        // Given
        val keyword = "测试"
        val results = listOf(createTestNovel(title = "测试小说"))
        coEvery { novelRepository.getAllNovels() } returns flowOf(emptyList())
        coEvery { novelRepository.searchNovels(keyword) } returns flowOf(results)

        viewModel = HomeViewModel(novelRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.searchNovels(keyword)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { novelRepository.searchNovels(keyword) }
    }

    @Test
    fun `when search with blank keyword then load all novels`() = runTest {
        // Given
        val novels = listOf(createTestNovel())
        coEvery { novelRepository.getAllNovels() } returns flowOf(novels)

        viewModel = HomeViewModel(novelRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.searchNovels("   ")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(atLeast = 2) { novelRepository.getAllNovels() }
    }

    // ==================== 统计信息测试 ====================

    @Test
    fun `when get stats then calculate correctly`() = runTest {
        // Given
        val novels = listOf(
            createTestNovel(id = "1", status = NovelStatus.ONGOING, wordCount = 10000),
            createTestNovel(id = "2", status = NovelStatus.ONGOING, wordCount = 20000),
            createTestNovel(id = "3", status = NovelStatus.COMPLETED, wordCount = 30000)
        )
        coEvery { novelRepository.getAllNovels() } returns flowOf(novels)

        viewModel = HomeViewModel(novelRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        val stats = viewModel.getNovelStats()

        // Then
        assertEquals(3, stats.totalCount)
        assertEquals(2, stats.ongoingCount)
        assertEquals(1, stats.completedCount)
        assertEquals(60000L, stats.totalWords)
    }

    // ==================== 刷新测试 ====================

    @Test
    fun `when refresh then reload novels`() = runTest {
        // Given
        coEvery { novelRepository.getAllNovels() } returns flowOf(emptyList())

        viewModel = HomeViewModel(novelRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(atLeast = 2) { novelRepository.getAllNovels() }
    }

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

    @Test
    fun `HomeUiState Error contains message`() {
        val message = "加载失败"
        val state = HomeUiState.Error(message)

        assertEquals(message, state.message)
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

    // ==================== 辅助方法 ====================

    private fun createTestNovel(
        id: String = "test-id",
        title: String = "测试小说",
        genre: String = "玄幻",
        status: NovelStatus = NovelStatus.ONGOING,
        wordCount: Long = 10000
    ): Novel {
        return Novel(
            id = id,
            title = title,
            genre = genre,
            chapterCount = 10,
            wordCount = wordCount,
            status = status,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
