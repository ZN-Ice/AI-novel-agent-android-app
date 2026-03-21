package com.novelapp.aiagent.model

/**
 * 语音指令实体
 *
 * @property id 指令ID
 * @property commandType 指令类型
 * @property recognizedText 识别的文本
 * @property confidence 置信度
 * @property timestamp 时间戳
 * @property executed 是否已执行
 * @property executionResult 执行结果
 */
data class VoiceCommandEntity(
    val id: String,
    val commandType: VoiceCommandType,
    val recognizedText: String,
    val confidence: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val executed: Boolean = false,
    val executionResult: String? = null
)

/**
 * 语音指令类型
 */
enum class VoiceCommandType(val action: String, val description: String) {
    // 新建相关
    CREATE_NOVEL("ACTION_CREATE_NOVEL", "新建小说"),
    CREATE_CHAPTER("ACTION_CREATE_CHAPTER", "新建章节"),

    // 删除相关
    DELETE_NOVEL("ACTION_DELETE_NOVEL", "删除小说"),
    DELETE_CHAPTER("ACTION_DELETE_CHAPTER", "删除章节"),
    CONFIRM_DELETE("ACTION_CONFIRM_DELETE", "确认删除"),
    CANCEL_DELETE("ACTION_CANCEL_DELETE", "取消删除"),

    // 创作相关
    ENTER_EDITOR("ACTION_ENTER_EDITOR", "进入创作"),
    START_WRITING("ACTION_START_WRITING", "开始写作"),
    PAUSE("ACTION_PAUSE", "暂停"),
    CONTINUE("ACTION_CONTINUE", "继续"),
    UNDO("ACTION_UNDO", "撤销"),
    REDO("ACTION_REDO", "重做"),
    SAVE("ACTION_SAVE", "保存"),

    // 导航相关
    GO_HOME("ACTION_GO_HOME", "返回首页"),
    NEXT_CHAPTER("ACTION_NEXT_CHAPTER", "下一章"),
    PREV_CHAPTER("ACTION_PREV_CHAPTER", "上一章"),

    // 设置相关
    SETTINGS("ACTION_SETTINGS", "设置"),
    HELP("ACTION_HELP", "帮助");

    companion object {
        fun fromAction(action: String): VoiceCommandType? {
            return entries.find { it.action == action }
        }
    }
}

/**
 * 语音识别结果
 */
data class VoiceRecognitionResult(
    val text: String,
    val confidence: Float,
    val isFinal: Boolean = false,
    val errorCode: Int? = null,
    val errorMessage: String? = null
) {
    val isSuccess: Boolean
        get() = errorCode == null && text.isNotBlank()

    companion object {
        fun error(code: Int, message: String): VoiceRecognitionResult {
            return VoiceRecognitionResult(
                text = "",
                confidence = 0f,
                isFinal = true,
                errorCode = code,
                errorMessage = message
            )
        }

        fun success(text: String, confidence: Float, isFinal: Boolean = true): VoiceRecognitionResult {
            return VoiceRecognitionResult(
                text = text,
                confidence = confidence,
                isFinal = isFinal
            )
        }
    }
}

/**
 * 语音状态
 */
sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    object Processing : VoiceState()
    data class Success(val command: VoiceCommandType, val text: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
    object PermissionDenied : VoiceState()

    val isListening: Boolean
        get() = this is Listening

    val isProcessing: Boolean
        get() = this is Processing

    val isSuccess: Boolean
        get() = this is Success

    val isError: Boolean
        get() = this is Error
}
