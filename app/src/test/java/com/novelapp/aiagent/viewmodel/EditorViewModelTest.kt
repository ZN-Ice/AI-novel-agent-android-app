package com.novelapp.aiagent.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.novelapp.aiagent.ai.AIRepository
import com.novelapp.aiagent.data.repository.ChapterRepository
import com.novelapp.aiagent.data.repository.NovelRepository
import com.novelapp.aiagent.model.AIContent
import com.novelapp.aiagent.model.AIGenerateState
import com.novelapp.aiagent.model.AIResult
import com.novelapp.aiagent.model.AIResponse
import com.novelapp.aiagent.model.Chapter
import com.novelapp.aiagent.model.FinishReason
import com.novelapp.aiagent.model.Novel
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
 * EditorViewModel 单元测试
 *
 * 测试覆盖：
 * - 加载小说和章节数据
 * - 内容编辑和更新
 * - 保存机制
 * - AI生成状态
 * - 撤销/重做功能
 * - 章节切换
 * - 新建章节
 *
 * 注意：ViewModel 的 startAutoSave() 包含 while(true) { delay(30s) } 无限循环。
 * 必须在每个测试中调用 stopAutoSave() 取消自动保存协程，
 * 否则 runTest cleanup 的 advanceUntilIdle() 会永久挂起。
 *
 * @see AGENTS.md 10.2节 单元测试规范
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val novelRepository: NovelRepository = mockk()
    private val chapterRepository: ChapterRepository = mockk()
    private val aiRepository: AIRepository = mockk()

    private lateinit var viewModel: EditorViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 安全执行当前排队的协程（不推进虚拟时间）。
     * 不能使用 advanceUntilIdle()，因为 ViewModel 的
     * startAutoSave() 包含 while(true) { delay() } 无限循环。
     */
    private fun runPendingTasks() {
        repeat(3) {
            testDispatcher.scheduler.runCurrent()
        }
    }

    // ==================== 初始化测试 ====================

    @Test
    fun `when init with novelId then load novel data`() = runTest {
        // Given
        val novel = createTestNovel()
        val chapter = createTestChapter()
        coEvery { novelRepository.getNovelById("novel-1") } returns Result.success(novel)
        coEvery { chapterRepository.getChaptersByNovelId("novel-1") } returns flowOf(listOf(chapter))
        coEvery { chapterRepository.getChapterById(chapter.id) } returns Result.success(chapter)

        val savedState = SavedStateHandle(mapOf("novelId" to "novel-1"))

        // When
        viewModel = EditorViewModel(novelRepository, chapterRepository, aiRepository, savedState)
        runPendingTasks()
        viewModel.stopAutoSave()

        // Then
        assertEquals(novel, viewModel.novel.value)
    }

    @Test
    fun `when init then load chapters and select latest`() = runTest {
        // Given
        val chapter1 = createTestChapter(id = "ch-1", chapterNumber = 1)
        val chapter2 = createTestChapter(id = "ch-2", chapterNumber = 2)
        coEvery { novelRepository.getNovelById(any()) } returns Result.success(createTestNovel())
        coEvery { chapterRepository.getChaptersByNovelId(any()) } returns flowOf(listOf(chapter1, chapter2))
        coEvery { chapterRepository.getChapterById("ch-2") } returns Result.success(chapter2)

        val savedState = SavedStateHandle(mapOf("novelId" to "novel-1"))

        // When
        viewModel = EditorViewModel(novelRepository, chapterRepository, aiRepository, savedState)
        runPendingTasks()
        viewModel.stopAutoSave()

        // Then
        assertEquals(2, viewModel.chapters.value.size)
        assertEquals(chapter2, viewModel.currentChapter.value)
    }

    // ==================== 内容编辑测试 ====================

    @Test
    fun `when update content then content changes and state is Unsaved`() = runTest {
        // Given
        setupViewModel()

        // When
        viewModel.updateContent("新的内容")

        // Then
        assertEquals("新的内容", viewModel.content.value)
        assertTrue(viewModel.saveState.value is SaveState.Unsaved)
    }

    @Test
    fun `when update content multiple times then content tracks latest`() = runTest {
        // Given
        setupViewModel()

        // When
        viewModel.updateContent("第一段")
        viewModel.updateContent("第二段")
        viewModel.updateContent("第三段")

        // Then
        assertEquals("第三段", viewModel.content.value)
    }

    // ==================== 保存测试 ====================

    @Test
    fun `when save content then call repository and state is Saved`() = runTest {
        // Given
        val chapter = createTestChapter(content = "原始内容")
        coEvery { chapterRepository.updateChapterContent(any(), any(), any()) } returns Result.success(Unit)
        setupViewModel(chapter = chapter)

        // When
        viewModel.updateContent("修改后的内容")
        viewModel.saveContent()
        runPendingTasks()

        // Then
        coVerify { chapterRepository.updateChapterContent(chapter.id, "修改后的内容", "novel-1") }
        assertTrue(viewModel.saveState.value is SaveState.Saved)
    }

    @Test
    fun `when save with no changes then skip save`() = runTest {
        // Given
        coEvery { chapterRepository.updateChapterContent(any(), any(), any()) } returns Result.success(Unit)
        setupViewModel()

        // When - save without changes
        viewModel.saveContent()
        runPendingTasks()

        // Then - repository should not be called
        coVerify(exactly = 0) { chapterRepository.updateChapterContent(any(), any(), any()) }
    }

    @Test
    fun `when save fails then state is Error`() = runTest {
        // Given
        val chapter = createTestChapter(content = "原始内容")
        coEvery {
            chapterRepository.updateChapterContent(any(), any(), any())
        } returns Result.failure(Exception("保存失败"))
        setupViewModel(chapter = chapter)

        // When
        viewModel.updateContent("新内容")
        viewModel.saveContent()
        runPendingTasks()

        // Then
        assertTrue(viewModel.saveState.value is SaveState.Error)
    }

    // ==================== 撤销/重做测试 ====================

    @Test
    fun `when undo then restore previous content`() = runTest {
        // Given
        setupViewModel()

        viewModel.updateContent("版本1")
        viewModel.updateContent("版本2")
        viewModel.updateContent("版本3")

        // When
        val result = viewModel.undo()

        // Then
        assertTrue(result)
        assertEquals("版本2", viewModel.content.value)
    }

    @Test
    fun `when undo multiple times then restore correct version`() = runTest {
        // Given
        setupViewModel()

        viewModel.updateContent("版本1")
        viewModel.updateContent("版本2")
        viewModel.updateContent("版本3")

        // When
        viewModel.undo()
        viewModel.undo()

        // Then
        assertEquals("版本1", viewModel.content.value)
    }

    @Test
    fun `when undo with empty stack then return false`() = runTest {
        // Given
        setupViewModel()

        // When
        val result = viewModel.undo()

        // Then
        assertFalse(result)
    }

    @Test
    fun `when redo after undo then restore next version`() = runTest {
        // Given
        setupViewModel()

        viewModel.updateContent("版本1")
        viewModel.updateContent("版本2")
        viewModel.undo()

        // When
        val result = viewModel.redo()

        // Then
        assertTrue(result)
        assertEquals("版本2", viewModel.content.value)
    }

    @Test
    fun `when redo with empty stack then return false`() = runTest {
        // Given
        setupViewModel()

        // When
        val result = viewModel.redo()

        // Then
        assertFalse(result)
    }

    @Test
    fun `when update content after undo then clear redo stack`() = runTest {
        // Given
        setupViewModel()

        viewModel.updateContent("版本1")
        viewModel.updateContent("版本2")
        viewModel.undo()

        // When - new edit after undo
        viewModel.updateContent("版本2-修改")

        // Then - redo should be empty
        assertFalse(viewModel.redo())
    }

    // ==================== AI生成测试 ====================

    @Test
    fun `when start AI generation then state changes to Success`() = runTest {
        // Given
        val chapter = createTestChapter()
        val aiContent = AIContent(
            requestId = "req-1",
            text = "AI生成的内容",
            tokens = 100,
            finishReason = FinishReason.STOP
        )
        val aiResponse = AIResponse(code = 0, message = "success", data = aiContent)
        coEvery { aiRepository.generateContent(any(), any(), any()) } returns AIResult.success(aiResponse)
        setupViewModel(chapter = chapter)

        // When
        viewModel.startAIGeneration("写一段打斗")
        runPendingTasks()

        // Then
        coVerify { aiRepository.generateContent("novel-1", chapter.id, "写一段打斗") }
        assertTrue(viewModel.aiState.value is AIGenerateState.Success)
    }

    @Test
    fun `when AI generation fails then state is Error`() = runTest {
        // Given
        coEvery { aiRepository.generateContent(any(), any(), any()) } returns AIResult.failure<AIResponse>("生成失败")
        setupViewModel()

        // When
        viewModel.startAIGeneration()
        runPendingTasks()

        // Then
        val state = viewModel.aiState.value
        assertTrue("Expected Error but got $state", state is AIGenerateState.Error)
    }

    @Test
    fun `when cancel AI generation then state is Cancelled`() = runTest {
        // Given
        setupViewModel()

        // When
        viewModel.cancelAIGeneration()

        // Then
        assertTrue(viewModel.aiState.value is AIGenerateState.Cancelled)
    }

    @Test
    fun `when AI generates text then append to content`() = runTest {
        // Given
        val chapter = createTestChapter(content = "原有内容")
        val aiContent = AIContent(
            requestId = "req-1",
            text = "AI新内容",
            tokens = 50,
            finishReason = FinishReason.STOP
        )
        val aiResponse = AIResponse(code = 0, message = "success", data = aiContent)
        coEvery { aiRepository.generateContent(any(), any(), any()) } returns AIResult.success(aiResponse)
        setupViewModel(chapter = chapter)

        // When
        viewModel.startAIGeneration()
        runPendingTasks()

        // Then
        assertTrue(viewModel.content.value.contains("原有内容"))
        assertTrue(viewModel.content.value.contains("AI新内容"))
    }

    // ==================== 章节切换测试 ====================

    @Test
    fun `when go to previous chapter then load previous`() = runTest {
        // Given
        val currentChapter = createTestChapter(id = "ch-2", chapterNumber = 2)
        val prevChapter = createTestChapter(id = "ch-1", chapterNumber = 1)
        coEvery { chapterRepository.getPreviousChapter("novel-1", 2) } returns Result.success(prevChapter)
        coEvery { chapterRepository.getChapterById("ch-1") } returns Result.success(prevChapter)
        setupViewModel(chapter = currentChapter)

        // When
        viewModel.goToPreviousChapter()
        runPendingTasks()

        // Then
        coVerify { chapterRepository.getPreviousChapter("novel-1", 2) }
    }

    @Test
    fun `when go to next chapter then load next`() = runTest {
        // Given
        val currentChapter = createTestChapter(id = "ch-1", chapterNumber = 1)
        val nextChapter = createTestChapter(id = "ch-2", chapterNumber = 2)
        coEvery { chapterRepository.getNextChapter("novel-1", 1) } returns Result.success(nextChapter)
        coEvery { chapterRepository.getChapterById("ch-2") } returns Result.success(nextChapter)
        setupViewModel(chapter = currentChapter)

        // When
        viewModel.goToNextChapter()
        runPendingTasks()

        // Then
        coVerify { chapterRepository.getNextChapter("novel-1", 1) }
    }

    @Test
    fun `when create new chapter then load new chapter`() = runTest {
        // Given
        val newChapter = createTestChapter(id = "ch-new", chapterNumber = 3)
        coEvery { chapterRepository.createChapter(any(), any()) } returns Result.success(newChapter)
        coEvery { chapterRepository.getChapterById("ch-new") } returns Result.success(newChapter)
        setupViewModel()

        // When
        viewModel.createNewChapter()
        runPendingTasks()

        // Then
        coVerify { chapterRepository.createChapter("novel-1", "") }
    }

    @Test
    fun `when create chapter fails then show error`() = runTest {
        // Given
        coEvery { chapterRepository.createChapter(any(), any()) } returns Result.failure(Exception("创建失败"))
        setupViewModel()

        // When
        viewModel.createNewChapter()
        runPendingTasks()

        // Then - error should be handled (no crash)
        coVerify { chapterRepository.createChapter("novel-1", "") }
    }

    // ==================== SaveState属性测试 ====================

    @Test
    fun `SaveState Saved isSaved is true`() {
        val state = SaveState.Saved
        assertTrue(state.isSaved)
        assertFalse(state.isSaving)
    }

    @Test
    fun `SaveState Saving isSaving is true`() {
        val state = SaveState.Saving
        assertFalse(state.isSaved)
        assertTrue(state.isSaving)
    }

    @Test
    fun `SaveState Unsaved properties correct`() {
        val state = SaveState.Unsaved
        assertFalse(state.isSaved)
        assertFalse(state.isSaving)
    }

    @Test
    fun `SaveState Error contains message`() {
        val state = SaveState.Error("保存失败")
        assertFalse(state.isSaved)
        assertFalse(state.isSaving)
        assertEquals("保存失败", state.message)
    }

    // ==================== AIGenerateState属性测试 ====================

    @Test
    fun `AIGenerateState Connecting isGenerating is true`() {
        val state = AIGenerateState.Connecting
        assertTrue(state.isGenerating)
        assertFalse(state.isSuccess)
        assertFalse(state.isError)
    }

    @Test
    fun `AIGenerateState Idle is not generating`() {
        val state = AIGenerateState.Idle
        assertFalse(state.isGenerating)
    }

    // ==================== 辅助方法 ====================

    private fun setupViewModel(
        novel: Novel = createTestNovel(),
        chapter: Chapter = createTestChapter()
    ) {
        coEvery { novelRepository.getNovelById("novel-1") } returns Result.success(novel)
        coEvery { chapterRepository.getChaptersByNovelId("novel-1") } returns flowOf(listOf(chapter))
        coEvery { chapterRepository.getChapterById(chapter.id) } returns Result.success(chapter)

        val savedState = SavedStateHandle(mapOf("novelId" to "novel-1"))
        viewModel = EditorViewModel(novelRepository, chapterRepository, aiRepository, savedState)
        runPendingTasks()
        viewModel.stopAutoSave()
    }

    private fun createTestNovel(
        id: String = "novel-1",
        title: String = "测试小说"
    ): Novel {
        return Novel(
            id = id,
            title = title,
            genre = "玄幻",
            chapterCount = 2,
            wordCount = 5000,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun createTestChapter(
        id: String = "ch-1",
        chapterNumber: Int = 1,
        content: String = "测试内容"
    ): Chapter {
        return Chapter(
            id = id,
            novelId = "novel-1",
            title = "第${chapterNumber}章",
            content = content,
            wordCount = content.length,
            chapterNumber = chapterNumber,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
