package com.novelapp.aiagent.viewmodel

import androidx.lifecycle.viewModelScope
import com.novelapp.aiagent.base.BaseViewModel
import com.novelapp.aiagent.base.UiState
import com.novelapp.aiagent.data.repository.NovelRepository
import com.novelapp.aiagent.model.Novel
import com.novelapp.aiagent.model.NovelStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 首页ViewModel
 *
 * 职责：
 * - 管理小说列表状态
 * - 处理新建/删除小说操作
 * - 监听数据变化
 *
 * @see AGENTS.md 5.1节
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val novelRepository: NovelRepository
) : BaseViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    // UI状态
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 小说列表
    private val _novels = MutableStateFlow<List<Novel>>(emptyList())
    val novels: StateFlow<List<Novel>> = _novels.asStateFlow()

    // 搜索关键词
    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    // 选中的小说（用于删除确认）
    private val _selectedNovel = MutableStateFlow<Novel?>(null)
    val selectedNovel: StateFlow<Novel?> = _selectedNovel.asStateFlow()

    // 删除确认弹窗状态
    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    init {
        loadNovels()
    }

    /**
     * 加载小说列表
     */
    fun loadNovels() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            novelRepository.getAllNovels()
                .catch { e ->
                    Timber.e(e, "Failed to load novels")
                    _uiState.value = HomeUiState.Error(e.message ?: "加载失败")
                    handleException(e)
                }
                .collect { novelList ->
                    _novels.value = novelList
                    _uiState.value = if (novelList.isEmpty()) {
                        HomeUiState.Empty
                    } else {
                        HomeUiState.Success(novelList)
                    }
                }
        }
    }

    /**
     * 搜索小说
     *
     * @param keyword 搜索关键词
     */
    fun searchNovels(keyword: String) {
        _searchKeyword.value = keyword

        if (keyword.isBlank()) {
            loadNovels()
            return
        }

        viewModelScope.launch {
            novelRepository.searchNovels(keyword)
                .catch { e ->
                    Timber.e(e, "Search failed")
                    _uiState.value = HomeUiState.Error("搜索失败")
                }
                .collect { results ->
                    _novels.value = results
                    _uiState.value = HomeUiState.Success(results)
                }
        }
    }

    /**
     * 创建新小说
     *
     * @param title 小说标题
     * @param genre 小说类型
     */
    fun createNovel(title: String, genre: String) {
        viewModelScope.launch {
            showLoading()

            val result = novelRepository.createNovel(title, genre)

            hideLoading()

            result.fold(
                onSuccess = { novel ->
                    Timber.i("Novel created: ${novel.id}")
                    showToast("小说创建成功")
                    // 导航到编辑器将由UI层处理
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to create novel")
                    showError(error.message ?: "创建失败")
                }
            )
        }
    }

    /**
     * 显示删除确认弹窗
     *
     * @param novel 要删除的小说
     */
    fun showDeleteConfirmation(novel: Novel) {
        _selectedNovel.value = novel
        _showDeleteDialog.value = true
    }

    /**
     * 隐藏删除确认弹窗
     */
    fun hideDeleteConfirmation() {
        _selectedNovel.value = null
        _showDeleteDialog.value = false
    }

    /**
     * 确认删除小说
     */
    fun confirmDelete() {
        val novel = _selectedNovel.value ?: return

        viewModelScope.launch {
            val result = novelRepository.deleteNovel(novel.id)

            result.fold(
                onSuccess = {
                    Timber.i("Novel deleted: ${novel.id}")
                    showToast("已删除，7天内可恢复")
                    hideDeleteConfirmation()
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to delete novel")
                    showError("删除失败")
                }
            )
        }
    }

    /**
     * 获取小说统计信息
     */
    fun getNovelStats(): NovelStats {
        val novelList = _novels.value
        return NovelStats(
            totalCount = novelList.size,
            ongoingCount = novelList.count { it.status == NovelStatus.ONGOING },
            completedCount = novelList.count { it.status == NovelStatus.COMPLETED },
            totalWords = novelList.sumOf { it.wordCount }
        )
    }

    /**
     * 刷新列表
     */
    fun refresh() {
        _searchKeyword.value = ""
        loadNovels()
    }
}

/**
 * 首页UI状态
 */
sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val novels: List<Novel>) : HomeUiState()
    object Empty : HomeUiState()
    data class Error(val message: String) : HomeUiState()

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isEmpty: Boolean get() = this is Empty
    val isError: Boolean get() = this is Error
}

/**
 * 小说统计
 */
data class NovelStats(
    val totalCount: Int,
    val ongoingCount: Int,
    val completedCount: Int,
    val totalWords: Long
) {
    fun getFormattedTotalWords(): String {
        return when {
            totalWords >= 10000 -> String.format("%.1f万", totalWords / 10000.0)
            else -> totalWords.toString()
        }
    }
}
