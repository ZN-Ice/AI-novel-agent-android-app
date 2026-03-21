package com.novelapp.aiagent.base

/**
 * UI状态封装
 *
 * 用于统一管理页面的加载、成功、失败状态
 */
sealed class UiState<out T> {
    /**
     * 初始状态
     */
    object Idle : UiState<Nothing>()

    /**
     * 加载中
     */
    object Loading : UiState<Nothing>()

    /**
     * 成功
     * @param data 成功数据
     */
    data class Success<T>(val data: T) : UiState<T>()

    /**
     * 失败
     * @param error 错误信息
     */
    data class Error(val error: Throwable) : UiState<Nothing>()

    /**
     * 空数据
     */
    object Empty : UiState<Nothing>()

    /**
     * 是否正在加载
     */
    val isLoading: Boolean
        get() = this is Loading

    /**
     * 是否成功
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * 是否失败
     */
    val isError: Boolean
        get() = this is Error

    /**
     * 获取数据（如果成功）
     */
    fun getDataOrNull(): T? {
        return (this as? Success)?.data
    }

    /**
     * 获取错误信息（如果失败）
     */
    fun getErrorOrNull(): Throwable? {
        return (this as? Error)?.error
    }

    companion object {
        /**
         * 创建成功状态
         */
        fun <T> success(data: T): UiState<T> = Success(data)

        /**
         * 创建失败状态
         */
        fun error(error: Throwable): UiState<Nothing> = Error(error)

        /**
         * 创建失败状态（带消息）
         */
        fun error(message: String): UiState<Nothing> = Error(Exception(message))

        /**
         * 创建加载状态
         */
        fun loading(): UiState<Nothing> = Loading
    }
}

/**
 * 分页状态封装
 */
sealed class PageState<out T> {
    /**
     * 刷新中
     */
    data class Refreshing<T>(val data: T? = null) : PageState<T>()

    /**
     * 加载更多中
     */
    data class LoadingMore<T>(val data: T) : PageState<T>()

    /**
     * 成功
     */
    data class Success<T>(
        val data: T,
        val hasMore: Boolean = false
    ) : PageState<T>()

    /**
     * 失败
     */
    data class Error<T>(
        val error: Throwable,
        val data: T? = null
    ) : PageState<T>()

    /**
     * 空数据
     */
    class Empty<T> : PageState<T>()

    val isLoading: Boolean
        get() = this is Refreshing || this is LoadingMore

    val isError: Boolean
        get() = this is Error
}
