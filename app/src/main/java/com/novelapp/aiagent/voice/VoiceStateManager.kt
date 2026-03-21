package com.novelapp.aiagent.voice

import com.novelapp.aiagent.model.VoiceCommandType
import com.novelapp.aiagent.model.VoiceRecognitionResult
import com.novelapp.aiagent.model.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语音状态管理器
 *
 * 职责：
 * - 统一管理语音模块的所有状态
 * - 提供状态流转
 * - 生成反馈消息
 *
 * @see docs/design/voice.md
 * @see AGENTS.md 3.4节
 */
@Singleton
class VoiceStateManager @Inject constructor() {

    companion object {
        private const val TAG = "VoiceStateManager"
    }

    // 当前语音状态
    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    // 反馈消息
    private val _feedback = MutableStateFlow<VoiceFeedback?>(null)
    val feedback: StateFlow<VoiceFeedback?> = _feedback.asStateFlow()

    // 最后识别的文本
    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    /**
     * 语音反馈
     */
    data class VoiceFeedback(
        val message: String,
        val type: FeedbackType
    ) {
        /**
         * 是否需要语音播报
         */
        fun shouldSpeak(): Boolean {
            return type == FeedbackType.SUCCESS || type == FeedbackType.ERROR
        }
    }

    /**
     * 反馈类型
     */
    enum class FeedbackType {
        SUCCESS,    // 成功（绿色）
        ERROR,      // 错误（红色）
        INFO,       // 信息（蓝色）
        HINT        // 提示（灰色）
    }

    /**
     * 更新状态
     *
     * @param newState 新状态
     */
    fun updateState(newState: VoiceState) {
        val oldState = _state.value
        Timber.d("State transition: $oldState -> $newState")
        _state.value = newState

        // 根据状态自动生成反馈
        when (newState) {
            is VoiceState.Idle -> {
                // 不显示反馈
            }
            is VoiceState.Listening -> {
                showFeedback("正在聆听…", FeedbackType.INFO)
            }
            is VoiceState.Processing -> {
                showFeedback("正在处理…", FeedbackType.INFO)
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
        }
    }

    /**
     * 显示反馈消息
     *
     * @param message 消息内容
     * @param type 反馈类型
     */
    fun showFeedback(message: String, type: FeedbackType) {
        _feedback.value = VoiceFeedback(message, type)
        Timber.d("Feedback: [$type] $message")
    }

    /**
     * 清除反馈
     */
    fun clearFeedback() {
        _feedback.value = null
    }

    /**
     * 更新识别文本
     *
     * @param text 识别的文本
     */
    fun updateRecognizedText(text: String) {
        _lastRecognizedText.value = text
    }

    /**
     * 重置所有状态
     */
    fun reset() {
        _state.value = VoiceState.Idle
        _feedback.value = null
        _lastRecognizedText.value = ""
        Timber.d("Voice state reset")
    }

    /**
     * 处理识别结果
     *
     * @param result 识别结果
     */
    fun handleRecognitionResult(result: VoiceRecognitionResult) {
        when {
            result.isSuccess -> {
                updateRecognizedText(result.text)
                if (result.isFinal) {
                    updateState(VoiceState.Success(
                        com.novelapp.aiagent.model.VoiceCommand.parse(result.text)
                            ?: com.novelapp.aiagent.model.VoiceCommand.CREATE_NOVEL,
                        result.text
                    ))
                }
            }
            result.isError -> {
                updateState(VoiceState.Error(
                    result.errorMessage ?: "识别失败"
                ))
            }
        }
    }

    /**
     * 获取当前状态提示文本
     *
     * @return 提示文本
     */
    fun getStateHint(): String {
        return when (val current = _state.value) {
            is VoiceState.Idle -> "点击麦克风开始语音输入"
            is VoiceState.Listening -> "请说出您的指令…"
            is VoiceState.Processing -> "正在识别…"
            is VoiceState.Success -> "识别成功：${current.text}"
            is VoiceState.Error -> "识别失败：${current.message}"
            is VoiceState.PermissionDenied -> "需要麦克风权限"
        }
    }

    /**
     * 是否可以开始录音
     *
     * @return 是否可以开始
     */
    fun canStartRecording(): Boolean {
        return _state.value is VoiceState.Idle || _state.value is VoiceState.Error
    }

    /**
     * 是否正在处理中
     *
     * @return 是否处理中
     */
    fun isProcessing(): Boolean {
        return _state.value is VoiceState.Listening || _state.value is VoiceState.Processing
    }
}
