package com.novelapp.aiagent.ui.home

import android.os.Bundle
import com.novelapp.aiagent.base.BaseActivity
import com.novelapp.aiagent.databinding.ActivityHomeBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主页Activity - 小说列表
 */
@AndroidEntryPoint
class HomeActivity : BaseActivity<ActivityHomeBinding>() {

    override fun createBinding(): ActivityHomeBinding {
        return ActivityHomeBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // TODO: 实现小说列表功能
    }
}
