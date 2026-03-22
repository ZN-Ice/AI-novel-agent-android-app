package com.novelapp.aiagent.voice

import com.novelapp.aiagent.model.VoiceCommandType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语音指令执行器
 *
 * 职责：
 * - 执行语音指令
 * - 分发指令到对应模块
 * - 记录执行结果
 *
 * @see docs/design/voice.md
 */
@Singleton
class VoiceCommandExecutor @Inject constructor() {

    companion object {
        private const val TAG = "VoiceCommandExecutor"
    }

    // 指令流（供UI层订阅）
    private val _commandFlow = MutableSharedFlow<CommandEvent>(extraBufferCapacity = 1)
    val commandFlow: SharedFlow<CommandEvent> = _commandFlow.asSharedFlow()

    // 执行结果流
    private val _resultFlow = MutableSharedFlow<CommandResult>(extraBufferCapacity = 1)
    val resultFlow: SharedFlow<CommandResult> = _resultFlow.asSharedFlow()

    /**
     * 指令事件
     */
    data class CommandEvent(
        val command: VoiceCommandType,
        val text: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * 执行结果
     */
    sealed class CommandResult {
        data class Success(val command: VoiceCommandType) : CommandResult()
        data class Failed(val command: VoiceCommandType, val reason: String) : CommandResult()
    }

    /**
     * 执行语音指令
     *
     * @param text 识别的文本
     * @return 执行结果
     */
    fun execute(text: String): ExecuteResult {
        val parseResult = VoiceCommandParser.parse(text)

        if (!parseResult.isSuccess) {
            Timber.d("Command not recognized: $text")
            return ExecuteResult.NotFound(text)
        }

        val command = parseResult.command!!
        Timber.i("Executing command: $command, text: $text")

        // 发送指令事件
        val event = CommandEvent(command, text)
        _commandFlow.tryEmit(event)

        return ExecuteResult.Success(command, text)
    }

    /**
     * 执行已解析的指令
     *
     * @param command 指令类型
     * @return 执行结果
     */
    fun executeCommand(command: VoiceCommandType): ExecuteResult {
        Timber.i("Executing command directly: $command")

        val event = CommandEvent(command, command.name)
        _commandFlow.tryEmit(event)

        return ExecuteResult.Success(command, command.name)
    }

    /**
     * 报告执行成功
     *
     * @param command 执行的指令
     */
    fun reportSuccess(command: VoiceCommandType) {
        _resultFlow.tryEmit(CommandResult.Success(command))
        Timber.d("Command executed successfully: $command")
    }

    /**
     * 报告执行失败
     *
     * @param command 执行的指令
     * @param reason 失败原因
     */
    fun reportFailure(command: VoiceCommandType, reason: String) {
        _resultFlow.tryEmit(CommandResult.Failed(command, reason))
        Timber.w("Command execution failed: $command, reason: $reason")
    }

    /**
     * 检查文本是否是有效指令（不执行）
     *
     * @param text 识别的文本
     * @return 是否是有效指令
     */
    fun isValidCommand(text: String): Boolean {
        return VoiceCommandParser.isValidCommand(text)
    }

    /**
     * 获取当前场景的指令提示
     *
     * @param scene 场景
     * @return 指令提示列表
     */
    fun getHintsForScene(scene: VoiceScene): List<CommandHint> {
        return VoiceCommandParser.getHintsForScene(scene)
    }

    /**
     * 获取所有指令提示
     *
     * @return 所有指令提示
     */
    fun getAllHints(): List<CommandHint> {
        return VoiceCommandParser.getAllHints()
    }
}

/**
 * 执行结果
 */
sealed class ExecuteResult {
    data class Success(val command: VoiceCommandType, val text: String) : ExecuteResult()
    data class NotFound(val text: String) : ExecuteResult()
    data class Error(val message: String) : ExecuteResult()

    val isSuccess: Boolean
        get() = this is Success

    val isNotFound: Boolean
        get() = this is NotFound
}
