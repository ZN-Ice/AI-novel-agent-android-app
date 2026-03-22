package com.novelapp.aiagent.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

/**
 * Activity基类
 *
 * 职责：
 * - 统一ViewBinding管理
 * - 统一权限请求处理
 * - 统一Toast显示
 * - 生命周期日志
 *
 * 注意：子类需要使用Hilt依赖注入时，必须在子类上添加@AndroidEntryPoint注解
 * Hilt不支持在带有泛型参数的基类上使用@AndroidEntryPoint
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    // ViewBinding实例，由子类创建
    protected lateinit var binding: VB

    /**
     * 创建ViewBinding实例
     * 子类必须实现此方法
     */
    protected abstract fun createBinding(): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = createBinding()
        setContentView(binding.root)

        // 初始化视图
        initView()
        // 初始化数据
        initData()
        // 设置监听器
        initListener()
    }

    /**
     * 初始化视图
     * 子类实现具体的视图初始化逻辑
     */
    protected open fun initView() {}

    /**
     * 初始化数据
     * 子类实现具体的数据加载逻辑
     */
    protected open fun initData() {}

    /**
     * 设置监听器
     * 子类实现具体的监听器设置逻辑
     */
    protected open fun initListener() {}

    /**
     * 检查是否有指定权限
     */
    protected fun hasPermission(permission: String): Boolean {
        return checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查是否有多个权限
     */
    protected fun hasPermissions(permissions: Array<String>): Boolean {
        return permissions.all { hasPermission(it) }
    }

    /**
     * 请求权限
     * @param permissions 权限数组
     * @param requestCode 请求码
     */
    protected fun requestPermissionsCompat(permissions: Array<String>, requestCode: Int) {
        requestPermissions(permissions, requestCode)
    }

    /**
     * 权限请求结果回调
     * 子类可重写此方法处理权限结果
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onPermissionResult(requestCode, permissions, grantResults)
    }

    /**
     * 权限请求结果处理
     * 子类实现具体的权限结果处理逻辑
     */
    protected open fun onPermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {}
}
