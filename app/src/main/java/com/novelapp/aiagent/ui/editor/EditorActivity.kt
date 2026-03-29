package com.novelapp.aiagent.ui.editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.novelapp.aiagent.R
import com.novelapp.aiagent.base.BaseActivity
import com.novelapp.aiagent.databinding.ActivityEditorBinding
import com.novelapp.aiagent.model.AIGenerateState
import com.novelapp.aiagent.viewmodel.EditorViewModel
import com.novelapp.aiagent.viewmodel.SaveState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.activity.viewModels

/**
 * 创作空间Activity - 编辑器
 *
 * 命名规范遵循 AGENTS.md 2.1节
 * 页面路由遵循 AGENTS.md 2.2节
 *
 * @see AGENTS.md 5.3节 创作空间流程
 */
@AndroidEntryPoint
class EditorActivity : BaseActivity<ActivityEditorBinding>() {

    companion object {
        private const val TAG = "EditorActivity"
        private const val EXTRA_NOVEL_ID = "novel_id"
        private const val EXTRA_CHAPTER_ID = "chapter_id"

        fun createIntent(context: Context, novelId: String, chapterId: String? = null): Intent {
            return Intent(context, EditorActivity::class.java).apply {
                putExtra(EXTRA_NOVEL_ID, novelId)
                chapterId?.let { putExtra(EXTRA_CHAPTER_ID, it) }
            }
        }
    }

    // ViewModel
    private val viewModel: EditorViewModel by viewModels()

    // 内容变更监听标记（防止循环更新）
    private var isContentUpdatingFromVm = false

    // 语音权限请求
    private val voicePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceInput()
        } else {
            showSnackbar(getString(R.string.voice_permission_denied))
        }
    }

    override fun createBinding(): ActivityEditorBinding {
        return ActivityEditorBinding.inflate(layoutInflater)
    }

    override fun initView() {
        setupToolbar()
        setupContentEditor()
    }

    override fun initListener() {
        setupNavigationListeners()
        setupBottomBarListeners()
        setupBackPressHandler()
    }

    override fun initData() {
        observeViewModel()
    }

    // ==================== Toolbar ====================

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    // ==================== 内容编辑器 ====================

    private fun setupContentEditor() {
        binding.etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (!isContentUpdatingFromVm) {
                    viewModel.updateContent(s?.toString() ?: "")
                }
                updateWordCount(s?.toString() ?: "")
            }
        })
    }

    // ==================== 章节导航 ====================

    private fun setupNavigationListeners() {
        binding.btnPrevChapter.setOnClickListener {
            viewModel.goToPreviousChapter()
        }

        binding.btnNextChapter.setOnClickListener {
            viewModel.goToNextChapter()
        }
    }

    // ==================== 底部操作栏 ====================

    private fun setupBottomBarListeners() {
        // AI续写按钮
        binding.btnAiGenerate.setOnClickListener {
            if (viewModel.aiState.value is AIGenerateState.Connecting ||
                viewModel.aiState.value is AIGenerateState.Generating
            ) {
                viewModel.cancelAIGeneration()
            } else {
                viewModel.startAIGeneration()
            }
        }

        // 语音输入按钮
        binding.btnVoice.setOnClickListener {
            requestVoicePermission()
        }

        // 撤销按钮
        binding.btnUndo.setOnClickListener {
            if (!viewModel.undo()) {
                showSnackbar("没有可撤销的操作")
            }
        }

        // 重做按钮
        binding.btnRedo.setOnClickListener {
            if (!viewModel.redo()) {
                showSnackbar("没有可重做的操作")
            }
        }

        // 新建章节按钮
        binding.btnNewChapter.setOnClickListener {
            viewModel.createNewChapter()
        }

        // 取消AI生成
        binding.btnCancelAi.setOnClickListener {
            viewModel.cancelAIGeneration()
        }
    }

    // ==================== 返回键处理 ====================

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.saveContent()
                finish()
            }
        })

        binding.toolbar.setNavigationOnClickListener {
            viewModel.saveContent()
            finish()
        }
    }

    // ==================== 观察ViewModel ====================

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察小说标题
                launch {
                    viewModel.novel.collect { novel ->
                        novel?.let {
                            binding.toolbar.title = it.title
                        }
                    }
                }

                // 观察当前章节
                launch {
                    viewModel.currentChapter.collect { chapter ->
                        chapter?.let {
                            updateChapterInfo(it.chapterNumber, viewModel.chapters.value.size)
                        }
                    }
                }

                // 观察章节列表
                launch {
                    viewModel.chapters.collect { chapters ->
                        val currentChapter = viewModel.currentChapter.value
                        currentChapter?.let {
                            updateChapterInfo(it.chapterNumber, chapters.size)
                        }
                    }
                }

                // 观察内容变化（从ViewModel到UI的单向同步）
                launch {
                    viewModel.content.collect { content ->
                        if (binding.etContent.text?.toString() != content) {
                            isContentUpdatingFromVm = true
                            binding.etContent.setText(content)
                            binding.etContent.setSelection(content.length)
                            isContentUpdatingFromVm = false
                        }
                    }
                }

                // 观察保存状态
                launch {
                    viewModel.saveState.collect { state ->
                        updateSaveStatus(state)
                    }
                }

                // 观察AI生成状态
                launch {
                    viewModel.aiState.collect { state ->
                        updateAIState(state)
                    }
                }
            }
        }
    }

    // ==================== UI更新方法 ====================

    /**
     * 更新章节导航信息
     */
    private fun updateChapterInfo(currentChapter: Int, totalChapters: Int) {
        binding.tvChapterTitle.text = getString(
            R.string.editor_chapter_format,
            currentChapter,
            totalChapters
        )

        // 更新导航按钮状态
        binding.btnPrevChapter.isEnabled = currentChapter > 1
        binding.btnNextChapter.isEnabled = currentChapter < totalChapters
        binding.btnPrevChapter.alpha = if (currentChapter > 1) 1.0f else 0.3f
        binding.btnNextChapter.alpha = if (currentChapter < totalChapters) 1.0f else 0.3f
    }

    /**
     * 更新保存状态指示器
     */
    private fun updateSaveStatus(state: SaveState) {
        when (state) {
            is SaveState.Saved -> {
                binding.tvSaveStatus.text = getString(R.string.editor_saved)
                binding.tvSaveStatus.setTextColor(getColor(R.color.text_hint))
            }
            is SaveState.Unsaved -> {
                binding.tvSaveStatus.text = getString(R.string.editor_unsaved)
                binding.tvSaveStatus.setTextColor(getColor(R.color.warning))
            }
            is SaveState.Saving -> {
                binding.tvSaveStatus.text = getString(R.string.editor_saving)
                binding.tvSaveStatus.setTextColor(getColor(R.color.primary))
            }
            is SaveState.Error -> {
                binding.tvSaveStatus.text = getString(R.string.editor_save_error)
                binding.tvSaveStatus.setTextColor(getColor(R.color.error))
            }
        }
    }

    /**
     * 更新AI生成状态
     */
    private fun updateAIState(state: AIGenerateState) {
        when (state) {
            is AIGenerateState.Idle -> {
                binding.llAiStatus.isVisible = false
                binding.btnAiGenerate.text = getString(R.string.editor_ai_generate)
                binding.btnAiGenerate.isEnabled = true
            }
            is AIGenerateState.Connecting -> {
                binding.llAiStatus.isVisible = true
                binding.tvAiStatus.text = getString(R.string.ai_generating)
                binding.btnAiGenerate.isEnabled = false
            }
            is AIGenerateState.Generating -> {
                binding.llAiStatus.isVisible = true
                binding.tvAiStatus.text = getString(R.string.ai_generating)
            }
            is AIGenerateState.Success -> {
                binding.llAiStatus.isVisible = false
                binding.btnAiGenerate.text = getString(R.string.editor_ai_generate)
                binding.btnAiGenerate.isEnabled = true
                showSnackbar("AI创作完成")
            }
            is AIGenerateState.Error -> {
                binding.llAiStatus.isVisible = false
                binding.btnAiGenerate.text = getString(R.string.editor_ai_generate)
                binding.btnAiGenerate.isEnabled = true
                showSnackbar(getString(R.string.ai_error, state.message))
            }
            is AIGenerateState.Cancelled -> {
                binding.llAiStatus.isVisible = false
                binding.btnAiGenerate.text = getString(R.string.editor_ai_generate)
                binding.btnAiGenerate.isEnabled = true
            }
        }
    }

    /**
     * 更新字数统计
     */
    private fun updateWordCount(content: String) {
        val count = calculateWordCount(content)
        binding.tvWordCount.text = getString(R.string.editor_word_count, count)
    }

    // ==================== 语音输入 ====================

    private fun requestVoicePermission() {
        voicePermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    private fun startVoiceInput() {
        // TODO: 实现语音输入功能
        // 参考 AGENTS.md 3.2节 语音指令映射规则
        showSnackbar(getString(R.string.voice_listening))
    }

    // ==================== 辅助方法 ====================

    /**
     * 计算字数（中文字符+英文单词）
     */
    private fun calculateWordCount(content: String): Int {
        if (content.isBlank()) return 0
        val chineseChars = content.count { it.code in 0x4E00..0x9FFF }
        val englishWords = content.split(Regex("\\s+"))
            .count { it.isNotEmpty() && it.all { c -> c.code !in 0x4E00..0x9FFF } }
        return chineseChars + englishWords
    }

    /**
     * 显示Snackbar
     */
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
