package com.novelapp.aiagent.ui.editor

import com.novelapp.aiagent.base.BaseActivity
import com.novelapp.aiagent.databinding.ActivityEditorBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * 创作空间Activity - 编辑器
 */
@AndroidEntryPoint
class EditorActivity : BaseActivity<ActivityEditorBinding>() {

    override fun createBinding(): ActivityEditorBinding {
        return ActivityEditorBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // TODO: 实现编辑器功能
    }
}
