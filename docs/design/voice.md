# ffmpeg语音交互设计

> **文档用途**：定义语音模块的技术方案和交互规范，作为语音模块开发的唯一依据。
>
> **维护规则**：本文档为固化文档，变更需人工审批并记录版本。

---

## 一、语音处理流程

### 1.1 整体架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                         语音处理架构                                 │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   麦克风      │───>│   ffmpeg     │───>│   云端ASR    │
│   录音模块    │    │   预处理      │    │   语音识别   │
└──────────────┘    └──────────────┘    └──────────────┘
                           │                    │
                           │                    ▼
                           │           ┌──────────────┐
                           │           │   指令解析   │
                           │           │   Command    │
                           │           │   Parser     │
                           │           └──────────────┘
                           │                    │
                           ▼                    ▼
                   ┌──────────────┐    ┌──────────────┐
                   │   本地缓存   │    │   指令执行   │
                   │   离线指令   │    │   Action     │
                   └──────────────┘    └──────────────┘
```

### 1.2 处理流程

```
1. 用户触发语音输入（点击按钮/长按）
          │
          ▼
2. 检查麦克风权限
          │
    ┌─────┴─────┐
    │           │
   已授权      未授权
    │           │
    │           ▼
    │    请求权限 → 拒绝 → 降级手动操作
    │           │
    │          授权
    │           │
    ▼           ▼
3. 开始录音（显示波形动画）
          │
          ▼
4. 录音结束 → ffmpeg预处理
   - 降噪
   - 格式转换（PCM）
   - 压缩
          │
          ▼
5. 发送到云端ASR
          │
    ┌─────┴─────┐
    │           │
   成功        失败
    │           │
    │           ▼
    │    使用离线指令缓存匹配
    │           │
    │     ┌─────┴─────┐
    │     │           │
    │   匹配成功    匹配失败
    │     │           │
    │     │           ▼
    │     │    提示"请说出有效指令"
    │     │           │
    ▼     ▼           │
6. 指令解析 ◄──────────┘
          │
          ▼
7. 执行对应Action
          │
          ▼
8. 语音播报反馈
```

---

## 二、ffmpeg集成方案

### 2.1 依赖配置

```kotlin
// app/build.gradle.kts
dependencies {
    // ffmpeg-kit
    implementation("com.arthenica:ffmpeg-kit-full:6.0-2")
}
```

### 2.2 核心封装

```kotlin
// voice/FFmpegWrapper.kt
package com.novelapp.aiagent.voice

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ffmpeg命令封装
 * 所有语音处理必须通过此类调用
 */
class FFmpegWrapper private constructor() {

    companion object {
        @Volatile
        private var instance: FFmpegWrapper? = null

        fun getInstance(): FFmpegWrapper {
            return instance ?: synchronized(this) {
                instance ?: FFmpegWrapper().also { instance = it }
            }
        }
    }

    /**
     * 音频转码为PCM格式
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     */
    suspend fun convertToPcm(
        inputPath: String,
        outputPath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val command = "-i \"$inputPath\" -f s16le -acodec pcm_s16le -ar 16000 -ac 1 \"$outputPath\""

            FFmpegKit.executeAsync(command, { session ->
                val returnCode = session.returnCode
                when {
                    returnCode.isValueSuccess -> {
                        continuation.resume(Result.success(outputPath))
                    }
                    else -> {
                        val error = session.output
                        continuation.resume(Result.failure(Exception("转码失败: $error")))
                    }
                }
            })
        }
    }

    /**
     * 音频降噪处理
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     */
    suspend fun denoise(
        inputPath: String,
        outputPath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            // 使用高通和低通滤波器降噪
            val command = "-i \"$inputPath\" -af \"highpass=f=200,lowpass=f=3000\" -ar 16000 \"$outputPath\""

            FFmpegKit.executeAsync(command, { session ->
                val returnCode = session.returnCode
                when {
                    returnCode.isValueSuccess -> {
                        continuation.resume(Result.success(outputPath))
                    }
                    else -> {
                        val error = session.output
                        continuation.resume(Result.failure(Exception("降噪失败: $error")))
                    }
                }
            })
        }
    }

    /**
     * 音频压缩
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @param bitrate 比特率，默认64k
     */
    suspend fun compress(
        inputPath: String,
        outputPath: String,
        bitrate: String = "64k"
    ): Result<String> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val command = "-i \"$inputPath\" -b:a $bitrate \"$outputPath\""

            FFmpegKit.executeAsync(command, { session ->
                val returnCode = session.returnCode
                when {
                    returnCode.isValueSuccess -> {
                        continuation.resume(Result.success(outputPath))
                    }
                    else -> {
                        val error = session.output
                        continuation.resume(Result.failure(Exception("压缩失败: $error")))
                    }
                }
            })
        }
    }

    /**
     * 获取音频信息
     */
    suspend fun getAudioInfo(inputPath: String): Result<AudioInfo> = withContext(Dispatchers.IO) {
        try {
            val mediaInformation = FFprobeKit.getMediaInformation(inputPath).mediaInformation
            val duration = mediaInformation.duration?.toDoubleOrNull() ?: 0.0
            val sampleRate = mediaInformation.streams?.firstOrNull()?.sampleRate?.toIntOrNull() ?: 0
            val channels = mediaInformation.streams?.firstOrNull()?.channels?.toIntOrNull() ?: 0

            Result.success(AudioInfo(duration, sampleRate, channels))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 取消所有执行中的任务
     */
    fun cancelAll() {
        FFmpegKit.cancel()
    }
}

data class AudioInfo(
    val duration: Double,      // 时长（秒）
    val sampleRate: Int,       // 采样率
    val channels: Int          // 声道数
)
```

### 2.3 录音管理

```kotlin
// voice/VoiceRecorder.kt
package com.novelapp.aiagent.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * 录音管理器
 */
class VoiceRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState

    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude

    sealed class RecordingState {
        object Idle : RecordingState()
        object Recording : RecordingState()
        data class Error(val message: String) : RecordingState()
    }

    /**
     * 开始录音
     */
    fun startRecording(outputPath: String): Boolean {
        return try {
            outputFile = File(outputPath)
            outputFile?.parentFile?.mkdirs()

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputPath)

                prepare()
                start()
            }

            _recordingState.value = RecordingState.Recording
            startAmplitudeMonitoring()
            true
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error(e.message ?: "录音启动失败")
            false
        }
    }

    /**
     * 停止录音
     */
    fun stopRecording(): File? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _recordingState.value = RecordingState.Idle
            outputFile
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error(e.message ?: "录音停止失败")
            null
        }
    }

    /**
     * 取消录音
     */
    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            outputFile?.delete()
            outputFile = null
            _recordingState.value = RecordingState.Idle
        } catch (e: Exception) {
            // 忽略取消时的错误
        }
    }

    private fun startAmplitudeMonitoring() {
        // 在协程中监控音量振幅
        // 实现略...
    }
}
```

---

## 三、指令白名单

### 3.1 指令映射表

```kotlin
// voice/VoiceCommandParser.kt
package com.novelapp.aiagent.voice

/**
 * 语音指令枚举
 * 所有支持的语音指令必须在白名单中定义
 */
enum class VoiceCommand(
    val keywords: List<String>,
    val action: String,
    val description: String
) {
    // ==================== 新建相关 ====================
    CREATE_NOVEL(
        keywords = listOf("新建小说", "创建小说", "开始写作", "新建作品"),
        action = "ACTION_CREATE_NOVEL",
        description = "打开新建小说弹窗"
    ),

    // ==================== 删除相关 ====================
    DELETE_NOVEL(
        keywords = listOf("删除小说", "移除小说", "删掉这个"),
        action = "ACTION_DELETE_NOVEL",
        description = "触发删除确认弹窗"
    ),
    CONFIRM_DELETE(
        keywords = listOf("确认删除", "是的删除", "确定删除", "是的删掉"),
        action = "ACTION_CONFIRM_DELETE",
        description = "确认删除操作"
    ),
    CANCEL_DELETE(
        keywords = listOf("取消删除", "不删除", "取消", "不要删"),
        action = "ACTION_CANCEL_DELETE",
        description = "取消删除操作"
    ),

    // ==================== 创作相关 ====================
    ENTER_EDITOR(
        keywords = listOf("进入创作", "开始创作", "打开编辑", "进入编辑"),
        action = "ACTION_ENTER_EDITOR",
        description = "进入创作空间"
    ),
    START_WRITING(
        keywords = listOf("写一段", "继续写", "生成内容", "帮我写", "接着写"),
        action = "ACTION_START_WRITING",
        description = "开始AI生成"
    ),
    PAUSE_WRITING(
        keywords = listOf("暂停", "停一下", "等一下"),
        action = "ACTION_PAUSE",
        description = "暂停当前操作"
    ),
    UNDO(
        keywords = listOf("撤销", "回退", "撤回"),
        action = "ACTION_UNDO",
        description = "撤销上一步操作"
    ),
    REDO(
        keywords = listOf("重做", "恢复"),
        action = "ACTION_REDO",
        description = "重做上一步操作"
    ),
    SAVE(
        keywords = listOf("保存", "存一下", "保存草稿"),
        action = "ACTION_SAVE",
        description = "保存当前内容"
    ),

    // ==================== 导航相关 ====================
    GO_HOME(
        keywords = listOf("返回首页", "回到主页", "返回主页", "去首页"),
        action = "ACTION_GO_HOME",
        description = "返回首页"
    ),
    NEXT_CHAPTER(
        keywords = listOf("下一章", "跳转下一章"),
        action = "ACTION_NEXT_CHAPTER",
        description = "跳转到下一章"
    ),
    PREV_CHAPTER(
        keywords = listOf("上一章", "跳转上一章"),
        action = "ACTION_PREV_CHAPTER",
        description = "跳转到上一章"
    ),

    // ==================== 设置相关 ====================
    SETTINGS(
        keywords = listOf("打开设置", "设置"),
        action = "ACTION_SETTINGS",
        description = "打开设置页面"
    ),
    HELP(
        keywords = listOf("帮助", "怎么用", "使用说明"),
        action = "ACTION_HELP",
        description = "显示帮助信息"
    );

    companion object {
        /**
         * 解析语音文本为指令
         * @param text 识别的文本
         * @return 匹配的指令，未匹配返回null
         */
        fun parse(text: String): VoiceCommand? {
            val normalizedText = text.trim().lowercase()
            return entries.find { command ->
                command.keywords.any { keyword ->
                    normalizedText.contains(keyword.lowercase())
                }
            }
        }

        /**
         * 获取所有指令的提示文本
         */
        fun getAllHints(): List<String> {
            return entries.map { "${it.keywords.first()}：${it.description}" }
        }
    }
}
```

### 3.2 指令执行器

```kotlin
// voice/VoiceCommandExecutor.kt
package com.novelapp.aiagent.voice

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 语音指令执行器
 * 负责将语音指令转换为UI Action
 */
class VoiceCommandExecutor {

    private val _commandFlow = MutableSharedFlow<VoiceCommand>(extraBufferCapacity = 1)
    val commandFlow: SharedFlow<VoiceCommand> = _commandFlow.asSharedFlow()

    /**
     * 执行语音指令
     */
    fun execute(text: String): VoiceCommandResult {
        val command = VoiceCommand.parse(text)

        return if (command != null) {
            _commandFlow.tryEmit(command)
            VoiceCommandResult.Success(command)
        } else {
            VoiceCommandResult.NotFound(text)
        }
    }

    /**
     * 检查是否是有效指令（不执行）
     */
    fun isValidCommand(text: String): Boolean {
        return VoiceCommand.parse(text) != null
    }
}

sealed class VoiceCommandResult {
    data class Success(val command: VoiceCommand) : VoiceCommandResult()
    data class NotFound(val text: String) : VoiceCommandResult()
    data class Error(val message: String) : VoiceCommandResult()
}
```

---

## 四、权限申请流程

### 4.1 权限声明

```xml
<!-- AndroidManifest.xml -->
<!-- 语音录制权限 -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- 可选：存储权限（API < 29） -->
<uses-permission
    android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

### 4.2 权限管理器

```kotlin
// voice/VoicePermissionManager.kt
package com.novelapp.aiagent.voice

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 语音权限管理器
 */
class VoicePermissionManager(private val context: Context) {

    companion object {
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO
        )

        const val REQUEST_CODE = 1001
    }

    /**
     * 检查是否有录音权限
     */
    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 请求录音权限
     */
    fun requestRecordPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            REQUIRED_PERMISSIONS,
            REQUEST_CODE
        )
    }

    /**
     * 检查是否需要显示权限说明
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.RECORD_AUDIO
        )
    }

    /**
     * 处理权限请求结果
     */
    fun handlePermissionResult(
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onGranted()
        } else if (shouldShowRationale(context as Activity)) {
            onDenied()
        } else {
            onPermanentlyDenied()
        }
    }
}
```

### 4.3 权限引导流程

```kotlin
// 在Activity/Fragment中使用
class VoicePermissionHelper(
    private val activity: Activity,
    private val permissionManager: VoicePermissionManager
) {
    /**
     * 检查并请求权限
     */
    fun checkAndRequestPermission(
        onGranted: () -> Unit,
        onShowRationale: () -> Unit,
        onPermanentlyDenied: () -> Unit
    ) {
        when {
            permissionManager.hasRecordPermission() -> {
                onGranted()
            }
            permissionManager.shouldShowRationale(activity) -> {
                // 显示说明弹窗
                onShowRationale()
            }
            else -> {
                // 首次请求权限
                permissionManager.requestRecordPermission(activity)
            }
        }
    }

    /**
     * 处理权限结果
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit
    ) {
        if (requestCode == VoicePermissionManager.REQUEST_CODE) {
            permissionManager.handlePermissionResult(
                grantResults,
                onGranted,
                onDenied,
                onPermanentlyDenied
            )
        }
    }
}
```

---

## 五、状态管理

### 5.1 状态定义

```kotlin
// voice/VoiceState.kt
package com.novelapp.aiagent.voice

/**
 * 语音状态
 */
sealed class VoiceState {
    /** 空闲状态 */
    object Idle : VoiceState()

    /** 等待用户说话 */
    object Listening : VoiceState()

    /** 正在处理 */
    object Processing : VoiceState()

    /** 识别成功 */
    data class Success(val command: VoiceCommand, val text: String) : VoiceState()

    /** 识别失败 */
    data class Error(val message: String) : VoiceState()

    /** 权限被拒绝 */
    object PermissionDenied : VoiceState()
}

/**
 * 语音反馈消息
 */
data class VoiceFeedback(
    val message: String,
    val type: FeedbackType
)

enum class FeedbackType {
    SUCCESS,
    ERROR,
    INFO,
    HINT
}
```

### 5.2 状态管理器

```kotlin
// voice/VoiceStateManager.kt
package com.novelapp.aiagent.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 语音状态管理器
 * 统一管理语音模块的所有状态
 */
class VoiceStateManager {

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private val _feedback = MutableStateFlow<VoiceFeedback?>(null)
    val feedback: StateFlow<VoiceFeedback?> = _feedback.asStateFlow()

    /**
     * 更新状态
     */
    fun updateState(newState: VoiceState) {
        _state.value = newState

        // 根据状态自动生成反馈
        when (newState) {
            is VoiceState.Listening -> {
                showFeedback("正在聆听...", FeedbackType.INFO)
            }
            is VoiceState.Processing -> {
                showFeedback("正在处理...", FeedbackType.INFO)
            }
            is VoiceState.Success -> {
                showFeedback("识别成功：${newState.text}", FeedbackType.SUCCESS)
            }
            is VoiceState.Error -> {
                showFeedback(newState.message, FeedbackType.ERROR)
            }
            is VoiceState.PermissionDenied -> {
                showFeedback("请授予麦克风权限", FeedbackType.ERROR)
            }
            is VoiceState.Idle -> {
                // 不显示反馈
            }
        }
    }

    /**
     * 显示反馈消息
     */
    fun showFeedback(message: String, type: FeedbackType) {
        _feedback.value = VoiceFeedback(message, type)
    }

    /**
     * 清除反馈
     */
    fun clearFeedback() {
        _feedback.value = null
    }

    /**
     * 重置状态
     */
    fun reset() {
        _state.value = VoiceState.Idle
        _feedback.value = null
    }
}
```

---

## 六、离线指令缓存

### 6.1 缓存策略

```kotlin
// voice/OfflineCommandCache.kt
package com.novelapp.aiagent.voice

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 离线指令缓存
 * 用于无网络时的基础指令匹配
 */
class OfflineCommandCache(private val context: Context) {

    private val cacheFile = File(context.cacheDir, "offline_commands.json")

    /**
     * 预加载常用指令到本地
     */
    fun preload() {
        val commands = OfflineCommandData(
            commands = listOf(
                CachedCommand("新建小说", "ACTION_CREATE_NOVEL"),
                CachedCommand("删除小说", "ACTION_DELETE_NOVEL"),
                CachedCommand("确认删除", "ACTION_CONFIRM_DELETE"),
                CachedCommand("取消", "ACTION_CANCEL_DELETE"),
                CachedCommand("进入创作", "ACTION_ENTER_EDITOR"),
                CachedCommand("开始创作", "ACTION_START_WRITING"),
                CachedCommand("暂停", "ACTION_PAUSE"),
                CachedCommand("撤销", "ACTION_UNDO"),
                CachedCommand("返回首页", "ACTION_GO_HOME")
            )
        )

        cacheFile.writeText(Json.encodeToString(commands))
    }

    /**
     * 从缓存匹配指令
     */
    fun match(text: String): CachedCommand? {
        if (!cacheFile.exists()) {
            preload()
        }

        return try {
            val data = Json.decodeFromString<OfflineCommandData>(cacheFile.readText())
            data.commands.find { text.contains(it.keyword, ignoreCase = true) }
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
data class OfflineCommandData(
    val commands: List<CachedCommand>
)

@Serializable
data class CachedCommand(
    val keyword: String,
    val action: String
)
```

---

## 七、异常处理方案

### 7.1 异常类型定义

```kotlin
// voice/VoiceException.kt
package com.novelapp.aiagent.voice

/**
 * 语音模块异常
 */
sealed class VoiceException(message: String) : Exception(message) {
    /** 权限被拒绝 */
    class PermissionDenied : VoiceException("麦克风权限被拒绝")

    /** 录音失败 */
    class RecordingFailed(reason: String) : VoiceException("录音失败: $reason")

    /** 识别超时 */
    class RecognitionTimeout : VoiceException("语音识别超时")

    /** 网络异常 */
    class NetworkError : VoiceException("网络连接失败")

    /** 指令不识别 */
    class CommandNotRecognized(text: String) : VoiceException("无法识别指令: $text")

    /** ffmpeg处理失败 */
    class FFmpegError(reason: String) : VoiceException("音频处理失败: $reason")
}
```

### 7.2 异常处理策略

```kotlin
// voice/VoiceErrorHandler.kt
package com.novelapp.aiagent.voice

/**
 * 语音异常处理器
 */
class VoiceErrorHandler(
    private val stateManager: VoiceStateManager
) {
    /**
     * 处理异常
     */
    fun handle(exception: VoiceException) {
        when (exception) {
            is VoiceException.PermissionDenied -> {
                stateManager.updateState(VoiceState.PermissionDenied)
                // 引导用户到设置页面
            }
            is VoiceException.RecordingFailed -> {
                stateManager.updateState(VoiceState.Error("录音失败，请检查麦克风"))
            }
            is VoiceException.RecognitionTimeout -> {
                stateManager.updateState(VoiceState.Error("识别超时，请重试"))
            }
            is VoiceException.NetworkError -> {
                // 尝试使用离线缓存
                stateManager.showFeedback("网络异常，使用离线模式", FeedbackType.INFO)
            }
            is VoiceException.CommandNotRecognized -> {
                stateManager.updateState(VoiceState.Error("未识别的指令，请重试"))
            }
            is VoiceException.FFmpegError -> {
                stateManager.updateState(VoiceState.Error("音频处理失败"))
            }
        }
    }
}
```

---

## 八、版本记录

| 版本 | 日期 | 变更内容 | 审批人 |
|------|------|----------|--------|
| v1.0.0 | 2026-03-21 | 初始化语音交互设计 | - |

---

**维护者**：AI小说安卓App研发团队
**相关文档**：[AGENTS.md](../AGENTS.md) | [ui.md](./ui.md) | [api.md](./api.md)
