package com.novelapp.aiagent.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.novelapp.aiagent.ai.AIRepository
import com.novelapp.aiagent.base.BaseViewModel
import com.novelapp.aiagent.data.repository.ChapterRepository
import com.novelapp.aiagent.data.repository.NovelRepository
import com.novelapp.aiagent.model.Chapter
import com.novelapp.aiagent.model.Novel
import com.novelapp.aiagent.model.AIGenerateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 编辑器ViewModel
 *
 * 职责：
 * - 管理章节内容编辑
 * - 处理AI生成请求
 * - 自动保存机制
 * - 撤销/重做功能
 *
 * @see AGENTS.md 5.3节
 */
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val novelRepository: NovelRepository,
    private val chapterRepository: ChapterRepository,
    private val aiRepository: AIRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    companion object {
        private const val TAG = "EditorViewModel"
        private const val AUTO_SAVE_INTERVAL = 30_000L // 30秒
        private const val MAX_UNDO_HISTORY = 50
    }

    // 路由参数
    private val novelId: String = savedStateHandle["novelId"] ?: ""
    private val chapterId: String? = savedStateHandle["chapterId"]

    // 当前小说
    private val _novel = MutableStateFlow<Novel?>(null)
    val novel: StateFlow<Novel?> = _novel.asStateFlow()

    // 当前章节
    private val _currentChapter = MutableStateFlow<Chapter?>(null)
    val currentChapter: StateFlow<Chapter?> = _currentChapter.asStateFlow()

    // 章节列表
    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters.asStateFlow()

    // 编辑内容
    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    // AI生成状态
    private val _aiState = MutableStateFlow<AIGenerateState>(AIGenerateState.Idle)
    val aiState: StateFlow<AIGenerateState> = _aiState.asStateFlow()

    // 保存状态
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Saved)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    // 撤销栈
    private val undoStack = ArrayDeque<String>(MAX_UNDO_HISTORY)

    // 重做栈
    private val redoStack = ArrayDeque<String>(MAX_UNDO_HISTORY)

    // 自动保存Job
    private var autoSaveJob: Job? = null

    // 上次保存的内容
    private var lastSavedContent = ""

    init {
        loadData()
        startAutoSave()
    }

    /**
     * 加载数据
     */
    private fun loadData() {
        viewModelScope.launch {
            // 加载小说信息
            novelRepository.getNovelById(novelId).fold(
                onSuccess = { _novel.value = it },
                onFailure = { Timber.e(it, "Failed to load novel") }
            )

            // 加载章节列表
            chapterRepository.getChaptersByNovelId(novelId).collect { chapterList ->
                _chapters.value = chapterList

                // 如果没有指定章节，加载最近编辑的章节
                if (chapterId == null && chapterList.isNotEmpty()) {
                    loadChapter(chapterList.last().id)
                } else if (chapterId != null) {
                    loadChapter(chapterId)
                }
            }
        }
    }

    /**
     * 加载章节
     *
     * @param targetChapterId 章节ID
     */
    fun loadChapter(targetChapterId: String) {
        viewModelScope.launch {
            val result = chapterRepository.getChapterById(targetChapterId)

            result.fold(
                onSuccess = { chapter ->
                    _currentChapter.value = chapter
                    _content.value = chapter.content
                    lastSavedContent = chapter.content
                    clearUndoRedoStack()
                    Timber.d("Chapter loaded: ${chapter.id}")
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load chapter")
                    showError("加载章节失败")
                }
            )
        }
    }

    /**
     * 更新内容
     *
     * @param newContent 新内容
     */
    fun updateContent(newContent: String) {
        // 保存到撤销栈
        _content.value.let { old ->
            undoStack.addFirst(old)
            if (undoStack.size > MAX_UNDO_HISTORY) {
                undoStack.removeLast()
            }
        }

        // 清空重做栈
        redoStack.clear()

        _content.value = newContent
        _saveState.value = SaveState.Unsaved
    }

    /**
     * 保存内容
     */
    fun saveContent() {
        val chapter = _currentChapter.value ?: return
        val currentContent = _content.value

        if (currentContent == lastSavedContent) {
            return // 无需保存
        }

        viewModelScope.launch {
            _saveState.value = SaveState.Saving

            val result = chapterRepository.updateChapterContent(
                chapterId = chapter.id,
                content = currentContent,
                novelId = novelId
            )

            result.fold(
                onSuccess = {
                    lastSavedContent = currentContent
                    _saveState.value = SaveState.Saved
                    Timber.d("Content saved")
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to save content")
                    _saveState.value = SaveState.Error("保存失败")
                }
            )
        }
    }

    /**
     * 开始AI生成
     *
     * @param instruction 用户指令（可选）
     */
    fun startAIGeneration(instruction: String? = null) {
        val chapter = _currentChapter.value ?: return

        viewModelScope.launch {
            _aiState.value = AIGenerateState.Connecting

            val result = aiRepository.generateContent(
                novelId = novelId,
                chapterId = chapter.id,
                instruction = instruction
            )

            if (result.isSuccess) {
                val response = result.data!!
                val generatedText = response.data?.text ?: ""
                _aiState.value = AIGenerateState.Success(response.data!!)

                // 追加生成的内容
                updateContent(_content.value + "\n" + generatedText)
            } else {
                Timber.e("AI generation failed: ${result.error}")
                _aiState.value = AIGenerateState.Error(result.error ?: "生成失败")
            }
        }
    }

    /**
     * 取消AI生成
     */
    fun cancelAIGeneration() {
        // TODO: 实现取消逻辑
        _aiState.value = AIGenerateState.Cancelled
    }

    /**
     * 撤销
     */
    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false

        val current = _content.value
        redoStack.addFirst(current)

        val previous = undoStack.removeFirst()
        _content.value = previous
        _saveState.value = SaveState.Unsaved

        return true
    }

    /**
     * 重做
     */
    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false

        val current = _content.value
        undoStack.addFirst(current)

        val next = redoStack.removeFirst()
        _content.value = next
        _saveState.value = SaveState.Unsaved

        return true
    }

    /**
     * 创建新章节
     */
    fun createNewChapter() {
        viewModelScope.launch {
            val result = chapterRepository.createChapter(novelId, "")

            result.fold(
                onSuccess = { chapter ->
                    Timber.i("New chapter created: ${chapter.id}")
                    loadChapter(chapter.id)
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to create chapter")
                    showError("创建章节失败")
                }
            )
        }
    }

    /**
     * 切换到上一章
     */
    fun goToPreviousChapter() {
        val current = _currentChapter.value ?: return
        viewModelScope.launch {
            val result = chapterRepository.getPreviousChapter(novelId, current.chapterNumber)
            result.getOrNull()?.let { loadChapter(it.id) }
        }
    }

    /**
     * 切换到下一章
     */
    fun goToNextChapter() {
        val current = _currentChapter.value ?: return
        viewModelScope.launch {
            val result = chapterRepository.getNextChapter(novelId, current.chapterNumber)
            result.getOrNull()?.let { loadChapter(it.id) }
        }
    }

    /**
     * 启动自动保存
     */
    private fun startAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(AUTO_SAVE_INTERVAL)
                if (_saveState.value == SaveState.Unsaved) {
                    saveContent()
                }
            }
        }
    }

    /**
     * 停止自动保存（仅用于测试）
     */
    @androidx.annotation.VisibleForTesting
    fun stopAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = null
    }

    /**
     * 清空撤销/重做栈
     */
    private fun clearUndoRedoStack() {
        undoStack.clear()
        redoStack.clear()
    }

    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()

        // 退出时保存
        if (_saveState.value == SaveState.Unsaved) {
            // 同步保存（ViewModel已销毁，使用协程作用域外的保存）
            Timber.d("ViewModel cleared with unsaved content")
        }
    }
}

/**
 * 保存状态
 */
sealed class SaveState {
    object Saved : SaveState()
    object Unsaved : SaveState()
    object Saving : SaveState()
    data class Error(val message: String) : SaveState()

    val isSaved: Boolean get() = this is Saved
    val isSaving: Boolean get() = this is Saving
}
