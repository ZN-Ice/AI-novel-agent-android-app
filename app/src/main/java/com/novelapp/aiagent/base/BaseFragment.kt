package com.novelapp.aiagent.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * Fragment基类
 *
 * 职责：
 * - 统一ViewBinding管理
 * - 统一Toast显示
 * - 生命周期管理
 * - 懒加载支持
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    // ViewBinding实例
    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException("ViewBinding is only valid between onCreateView and onDestroyView")

    // 是否已初始化
    private var isInitialized = false

    // 视图是否已创建
    private var isViewCreated = false

    /**
     * 创建ViewBinding实例
     * 子类必须实现此方法
     */
    protected abstract fun createBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = createBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isViewCreated = true

        // 初始化视图
        initView()
        // 设置监听器
        initListener()

        // 尝试懒加载
        tryLazyLoad()
    }

    override fun onResume() {
        super.onResume()
        // 尝试懒加载
        tryLazyLoad()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        isViewCreated = false
        isInitialized = false
    }

    /**
     * 初始化视图
     * 子类实现具体的视图初始化逻辑
     */
    protected open fun initView() {}

    /**
     * 初始化监听器
     * 子类实现具体的监听器设置逻辑
     */
    protected open fun initListener() {}

    /**
     * 懒加载数据
     * 只在视图可见时调用一次
     * 子类实现具体的数据加载逻辑
     */
    protected open fun lazyLoadData() {}

    /**
     * 尝试执行懒加载
     */
    private fun tryLazyLoad() {
        if (!isInitialized && isViewCreated && isVisible && isResumed) {
            isInitialized = true
            lazyLoadData()
        }
    }

    /**
     * 获取宿主Activity（类型安全）
     */
    @Suppress("UNCHECKED_CAST")
    protected fun <T : BaseActivity<*>> getHostActivity(): T? {
        return activity as? T
    }

    /**
     * 显示加载中
     */
    protected fun showLoading() {
        getHostActivity<BaseActivity<*>>()?.let {
            // 显示加载对话框
        }
    }

    /**
     * 隐藏加载中
     */
    protected fun hideLoading() {
        getHostActivity<BaseActivity<*>>()?.let {
            // 隐藏加载对话框
        }
    }
}
