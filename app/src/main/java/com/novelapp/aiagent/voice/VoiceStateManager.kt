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
                    // Parse command from text - default to CREATE_NOVEL if not recognized
                    val command = parseCommandFromText(result.text)
                    updateState(VoiceState.Success(command, result.text))
                }
            }
            result.errorCode != null -> {
                updateState(VoiceState.Error(
                    result.errorMessage ?: "识别失败"
                ))
            }
        }
    }

    /**
     * Parse voice command from recognized text
     */
    private fun parseCommandFromText(text: String): VoiceCommandType {
        // Simple keyword matching - can be enhanced with NLP
        return when {
            text.contains("新建小说") || text.contains("创建小说") -> VoiceCommandType.CREATE_NOVEL
            text.contains("新建章节") || text.contains("创建章节") -> VoiceCommandType.CREATE_CHAPTER
            text.contains("删除小说") -> VoiceCommandType.DELETE_NOVEL
            text.contains("删除章节") -> VoiceCommandType.DELETE_CHAPTER
            text.contains("确认删除") -> VoiceCommandType.CONFIRM_DELETE
            text.contains("取消删除") -> VoiceCommandType.CANCEL_DELETE
            text.contains("进入创作") || text.contains("开始编辑") -> VoiceCommandType.ENTER_EDITOR
            text.contains("开始写作") || text.contains("开始写") -> VoiceCommandType.START_WRITING
            text.contains("暂停") -> VoiceCommandType.PAUSE
            text.contains("继续") -> VoiceCommandType.CONTINUE
            text.contains("撤销") -> VoiceCommandType.UNDO
            text.contains("重做") -> VoiceCommandType.REDO
            text.contains("保存") -> VoiceCommandType.SAVE
            text.contains("返回首页") || text.contains("回到首页") -> VoiceCommandType.GO_HOME
            text.contains("下一章") -> VoiceCommandType.NEXT_CHAPTER
            text.contains("上一章") || text.contains("前一章") -> VoiceCommandType.PREV_CHAPTER
            text.contains("设置") -> VoiceCommandType.SETTINGS
            text.contains("帮助") -> VoiceCommandType.HELP
            else -> VoiceCommandType.CREATE_NOVEL // Default
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
