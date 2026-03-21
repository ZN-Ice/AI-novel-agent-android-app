package com.novelapp.aiagent.voice

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语音权限管理器
 *
 * 职责：
 * - 检查/请求录音权限
 * - 处理权限拒绝情况
 * - 引导用户到设置页面
 *
 * @see docs/design/voice.md
 * @see AGENTS.md 3.3节
 */
@Singleton
class VoicePermissionManager @Inject constructor() {

    companion object {
        private const val TAG = "VoicePermissionManager"

        // 必要权限
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO
        )

        // 请求码
        const val REQUEST_CODE = 1001
    }

    // 权限状态
    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.Unknown)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    /**
     * 权限状态
     */
    sealed class PermissionState {
        object Unknown : PermissionState()
        object Granted : PermissionState()
        object Denied : PermissionState()
        object PermanentlyDenied : PermissionState()

        val isGranted: Boolean
            get() = this is Granted
    }

    /**
     * 检查是否有录音权限
     *
     * @param context 上下文
     * @return 是否有权限
     */
    fun hasRecordPermission(context: Context): Boolean {
        val result = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        _permissionState.value = if (result) PermissionState.Granted else PermissionState.Denied
        return result
    }

    /**
     * 检查是否需要显示权限说明
     *
     * @param activity Activity
     * @return 是否需要说明
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.RECORD_AUDIO
        )
    }

    /**
     * 请求录音权限
     *
     * @param activity Activity
     */
    fun requestRecordPermission(activity: Activity) {
        Timber.i("Requesting record permission")
        ActivityCompat.requestPermissions(
            activity,
            REQUIRED_PERMISSIONS,
            REQUEST_CODE
        )
    }

    /**
     * 处理权限请求结果
     *
     * @param requestCode 请求码
     * @param permissions 权限数组
     * @param grantResults 结果数组
     * @return 处理结果
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        activity: Activity? = null
    ): PermissionResult {
        if (requestCode != REQUEST_CODE) {
            return PermissionResult.Ignored
        }

        if (grantResults.isEmpty()) {
            _permissionState.value = PermissionState.Denied
            return PermissionResult.Denied
        }

        val granted = grantResults[0] == PackageManager.PERMISSION_GRANTED

        return if (granted) {
            _permissionState.value = PermissionState.Granted
            Timber.i("Record permission granted")
            PermissionResult.Granted
        } else {
            // 检查是否是永久拒绝
            val permanentlyDenied = activity?.let {
                !shouldShowRationale(it)
            } ?: false

            if (permanentlyDenied) {
                _permissionState.value = PermissionState.PermanentlyDenied
                Timber.w("Record permission permanently denied")
                PermissionResult.PermanentlyDenied
            } else {
                _permissionState.value = PermissionState.Denied
                Timber.w("Record permission denied")
                PermissionResult.Denied
            }
        }
    }

    /**
     * 检查并请求权限（带回调）
     *
     * @param activity Activity
     * @param onGranted 已授权回调
     * @param onShowRationale 需要说明回调
     * @param onDenied 拒绝回调
     * @param onPermanentlyDenied 永久拒绝回调
     */
    fun checkAndRequestPermission(
        activity: Activity,
        onGranted: () -> Unit,
        onShowRationale: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit
    ) {
        when {
            hasRecordPermission(activity) -> {
                onGranted()
            }
            shouldShowRationale(activity) -> {
                onShowRationale()
            }
            else -> {
                requestRecordPermission(activity)
            }
        }
    }

    /**
     * 获取权限说明文本
     *
     * @return 说明文本
     */
    fun getRationaleMessage(): String {
        return "AI小说创作需要麦克风权限来识别您的语音指令，您可以：\n" +
                "• 使用语音创建小说\n" +
                "• 使用语音控制创作\n" +
                "• 使用语音进行AI续写"
    }

    /**
     * 获取永久拒绝时的引导文本
     *
     * @return 引导文本
     */
    fun getPermanentlyDeniedMessage(): String {
        return "麦克风权限被拒绝，语音功能无法使用。\n" +
                "请在系统设置中手动开启权限，或使用手动操作。"
    }
}

/**
 * 权限请求结果
 */
sealed class PermissionResult {
    object Granted : PermissionResult()
    object Denied : PermissionResult()
    object PermanentlyDenied : PermissionResult()
    object Ignored : PermissionResult()

    val isGranted: Boolean
        get() = this is Granted
}
