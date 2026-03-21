package com.novelapp.aiagent.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel基类
 *
 * 职责：
 * - 统一异常处理
 * - 统一UI状态管理
 * - 协程生命周期管理
 */
abstract class BaseViewModel : ViewModel() {

    // 全局异常处理器
    protected val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "ViewModel coroutine error")
        handleException(throwable)
    }

    // UI状态：加载中
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // UI状态：错误信息
    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    // UI状态：提示信息
    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    /**
     * 处理异常
     * 子类可重写此方法实现自定义异常处理
     */
    protected open fun handleException(throwable: Throwable) {
        val message = when (throwable) {
            is NetworkException -> "网络连接失败，请检查网络"
            is TimeoutException -> "请求超时，请重试"
            is ValidationException -> throwable.message ?: "数据验证失败"
            else -> "发生未知错误：${throwable.message}"
        }
        showError(message)
    }

    /**
     * 显示加载中
     */
    protected fun showLoading() {
        _isLoading.value = true
    }

    /**
     * 隐藏加载中
     */
    protected fun hideLoading() {
        _isLoading.value = false
    }

    /**
     * 显示错误信息
     */
    protected fun showError(message: String) {
        viewModelScope.launch {
            _errorMessage.emit(message)
        }
    }

    /**
     * 显示Toast提示
     */
    protected fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.emit(message)
        }
    }

    /**
     * 在IO线程执行协程
     */
    protected fun launchIO(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            block.invoke(this)
        }
    }

    /**
     * 在主线程执行协程
     */
    protected fun launchMain(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(Dispatchers.Main + exceptionHandler) {
            block.invoke(this)
        }
    }

    /**
     * 安全收集Flow
     */
    protected fun <T> Flow<T>.safeCollect(
        onError: (Throwable) -> Unit = { handleException(it) },
        onCollect: (T) -> Unit
    ): Job {
        return this
            .catch { onError(it) }
            .onEach { onCollect(it) }
            .launchIn(viewModelScope)
    }

    /**
     * 执行网络请求（带加载状态）
     */
    protected inline fun <T> launchRequest(
        crossinline request: suspend () -> Result<T>,
        crossinline onSuccess: (T) -> Unit = {},
        crossinline onError: (Throwable) -> Unit = { handleException(it) }
    ) {
        launchIO {
            showLoading()
            try {
                val result = request()
                hideLoading()
                result.fold(
                    onSuccess = { onSuccess(it) },
                    onFailure = { onError(it) }
                )
            } catch (e: Exception) {
                hideLoading()
                onError(e)
            }
        }
    }
}

// 自定义异常类型
class NetworkException(message: String = "网络异常") : Exception(message)
class TimeoutException(message: String = "请求超时") : Exception(message)
class ValidationException(message: String) : Exception(message)
