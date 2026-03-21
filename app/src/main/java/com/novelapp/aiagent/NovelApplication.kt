package com.novelapp.aiagent

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * AI小说安卓App Application类
 *
 * 职责：
 * - 初始化Hilt依赖注入
 * - 全局配置初始化
 * - 全局异常处理
 */
@HiltAndroidApp
class NovelApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化全局异常处理器
        setupExceptionHandler()

        // 其他初始化操作...
    }

    /**
     * 设置全局异常处理器
     * 捕获未处理的异常，防止App崩溃
     */
    private fun setupExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // 记录异常日志
            // Log.e("NovelApp", "Uncaught exception", throwable)

            // 可选：上报到监控平台

            // 调用默认处理器
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
