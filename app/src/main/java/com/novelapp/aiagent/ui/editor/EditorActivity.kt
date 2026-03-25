package com.novelapp.aiagent.ui.editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.novelapp.aiagent.base.BaseActivity
import com.novelapp.aiagent.databinding.ActivityEditorBinding
import dagger.hilt.android.AndroidEntryPoint

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

        fun createIntent(context: Context, novelId: String): Intent {
            return Intent(context, EditorActivity::class.java).apply {
                putExtra(EXTRA_NOVEL_ID, novelId)
            }
        }
    }

    // 当前小说ID
    private val novelId: String? by lazy { intent.getStringExtra(EXTRA_NOVEL_ID) }

    override fun createBinding(): ActivityEditorBinding {
        return ActivityEditorBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 获取小说ID
        novelId?.let { id ->
            // TODO: 加载小说数据
            // viewModel.loadNovel(id)
        }
    }
}
