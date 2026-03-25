package com.novelapp.aiagent.ui.home

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.novelapp.aiagent.R
import com.novelapp.aiagent.base.BaseActivity
import com.novelapp.aiagent.databinding.ActivityHomeBinding
import com.novelapp.aiagent.model.Novel
import com.novelapp.aiagent.ui.create.CreateNovelDialog
import com.novelapp.aiagent.ui.delete.DeleteConfirmDialog
import com.novelapp.aiagent.ui.editor.EditorActivity
import com.novelapp.aiagent.viewmodel.HomeUiState
import com.novelapp.aiagent.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.activity.viewModels

/**
 * 主页Activity - 小说列表
 *
 * 命名规范遵循 AGENTS.md 2.1节
 * 页面路由遵循 AGENTS.md 2.2节
 *
 * @see AGENTS.md 2.1节 组件命名规范
 * @see AGENTS.md 2.2节 页面路由规则
 * @see AGENTS.md 5.1节 新建小说流程
 * @see AGENTS.md 5.2节 删除小说流程
 */
@AndroidEntryPoint
class HomeActivity : BaseActivity<ActivityHomeBinding>() {

    companion object {
        private const val TAG = "HomeActivity"

        fun createIntent(context: Context): Intent {
            return Intent(context, HomeActivity::class.java)
        }
    }

    // ViewModel
    private val viewModel: HomeViewModel by viewModels()

    // Adapter
    private lateinit var novelAdapter: NovelListAdapter

    // Dialogs
    private var createDialog: CreateNovelDialog? = null
    private var deleteDialog: DeleteConfirmDialog? = null

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

    override fun createBinding(): ActivityHomeBinding {
        return ActivityHomeBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 设置Toolbar
        setSupportActionBar(binding.toolbar)

        // 初始化RecyclerView
        novelAdapter = NovelListAdapter(
            onItemClick = { novel -> onNovelItemClick(novel) },
            onDeleteClick = { novel -> onDeleteNovelClick(novel) }
        )
        binding.rvNovelList.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = novelAdapter
            setHasFixedSize(true)
        }

        // 重试按钮
        binding.btnRetry.setOnClickListener {
            viewModel.refresh()
        }
    }

    override fun initListener() {
        // 新建按钮
        binding.fabCreate.setOnClickListener {
            showCreateDialog()
        }

        // 语音按钮
        binding.fabVoice.setOnClickListener {
            requestVoicePermission()
        }
    }

    override fun initData() {
        // 观察数据变化
        observeData()
    }

    /**
     * 观察数据变化
     */
    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察UI状态
                launch {
                    viewModel.uiState.collect { state ->
                        updateUiState(state)
                    }
                }

                // 观察小说列表
                launch {
                    viewModel.novels.collect { novels ->
                        novelAdapter.submitList(novels)
                    }
                }

                // 观察删除弹窗状态
                launch {
                    viewModel.showDeleteDialog.collect { show ->
                        if (show) {
                            viewModel.selectedNovel.value?.let { novel ->
                                showDeleteDialog(novel)
                            }
                        } else {
                            dismissDeleteDialog()
                        }
                    }
                }
            }
        }
    }

    /**
     * 更新UI状态
     */
    private fun updateUiState(state: HomeUiState) {
        Timber.d("UI State: $state")

        // 加载状态
        binding.pbLoading.isVisible = state.isLoading

        // 空状态
        binding.llEmpty.isVisible = state.isEmpty

        // 错误状态
        binding.llError.isVisible = state.isError
        if (state is HomeUiState.Error) {
            binding.tvError.text = state.message
        }

        // 列表可见性
        binding.rvNovelList.isVisible = state.isSuccess

        // 列表为空时显示空状态
        if (state is HomeUiState.Success && state.novels.isEmpty()) {
            binding.llEmpty.isVisible = true
            binding.rvNovelList.isVisible = false
        }
    }

    /**
     * 小说项点击事件
     */
    private fun onNovelItemClick(novel: Novel) {
        Timber.i("Novel clicked: ${novel.id}")
        navigateToEditor(novel.id)
    }

    /**
     * 删除按钮点击事件
     */
    private fun onDeleteNovelClick(novel: Novel) {
        Timber.i("Delete novel clicked: ${novel.id}")
        viewModel.showDeleteConfirmation(novel)
    }

    /**
     * 显示新建小说弹窗
     */
    private fun showCreateDialog() {
        createDialog?.dismiss()
        createDialog = CreateNovelDialog.newInstance().apply {
            setOnCreateListener { title, genre ->
                createNovel(title, genre)
            }
        }
        createDialog?.show(supportFragmentManager, CreateNovelDialog.TAG)
    }

    /**
     * 显示删除确认弹窗
     */
    private fun showDeleteDialog(novel: Novel) {
        deleteDialog?.dismiss()
        deleteDialog = DeleteConfirmDialog.newInstance(novel).apply {
            setOnConfirmListener { novelId ->
                viewModel.confirmDelete()
            }
        }
        deleteDialog?.show(supportFragmentManager, DeleteConfirmDialog.TAG)
    }

    /**
     * 隐藏删除确认弹窗
     */
    private fun dismissDeleteDialog() {
        deleteDialog?.dismiss()
        deleteDialog = null
    }

    /**
     * 创建新小说
     */
    private fun createNovel(title: String, genre: String) {
        Timber.i("Creating novel: $title, genre: $genre")
        viewModel.createNovel(title, genre)

        // 创建成功后导航到编辑器
        // 注意：实际导航应该在ViewModel成功回调后执行
        // 这里简化处理，通过观察ViewModel状态来触发导航
    }

    /**
     * 导航到编辑器
     */
    private fun navigateToEditor(novelId: String) {
        val intent = EditorActivity.createIntent(this, novelId)
        startActivity(intent)
    }

    /**
     * 请求语音权限
     */
    private fun requestVoicePermission() {
        voicePermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    /**
     * 开始语音输入
     */
    private fun startVoiceInput() {
        // TODO: 实现语音输入功能
        // 参考 AGENTS.md 3.2节 语音指令映射规则
        showSnackbar(getString(R.string.voice_listening))
    }

    /**
     * 显示Snackbar
     */
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        createDialog?.dismiss()
        deleteDialog?.dismiss()
    }
}
