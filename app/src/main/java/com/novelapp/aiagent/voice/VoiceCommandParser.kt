package com.novelapp.aiagent.voice

import com.novelapp.aiagent.model.VoiceCommandType
import timber.log.Timber

/**
 * 语音指令解析器
 *
 * 职责：
 * - 解析语音识别文本为指令
 * - 支持模糊匹配
 * - 提供指令提示信息
 *
 * @see docs/design/voice.md
 * @see AGENTS.md 3.2节
 */
object VoiceCommandParser {

    private const val TAG = "VoiceCommandParser"

    /**
     * 指令关键词映射
     * 格式：指令类型 -> 关键词列表
     */
    private val COMMAND_KEYWORDS = mapOf(
        // 新建相关
        VoiceCommandType.CREATE_NOVEL to listOf(
            "新建小说", "创建小说", "开始写作", "新建作品", "写一部小说"
        ),
        VoiceCommandType.CREATE_CHAPTER to listOf(
            "新建章节", "创建章节", "添加章节", "新章节"
        ),

        // 删除相关
        VoiceCommandType.DELETE_NOVEL to listOf(
            "删除小说", "移除小说", "删掉这个小说"
        ),
        VoiceCommandType.DELETE_CHAPTER to listOf(
            "删除章节", "移除章节", "删掉这个章节"
        ),
        VoiceCommandType.CONFIRM_DELETE to listOf(
            "确认删除", "是的删除", "确定删除", "是的删掉", "确认"
        ),
        VoiceCommandType.CANCEL_DELETE to listOf(
            "取消删除", "不删除", "取消", "不要删", "算了"
        ),

        // 创作相关
        VoiceCommandType.ENTER_EDITOR to listOf(
            "进入创作", "开始创作", "打开编辑", "进入编辑", "打开创作空间"
        ),
        VoiceCommandType.START_WRITING to listOf(
            "写一段", "继续写", "生成内容", "帮我写", "接着写", "AI续写"
        ),
        VoiceCommandType.PAUSE to listOf(
            "暂停", "停一下", "等一下", "停止"
        ),
        VoiceCommandType.CONTINUE to listOf(
            "继续", "继续写", "接着写"
        ),
        VoiceCommandType.UNDO to listOf(
            "撤销", "回退", "撤回", "撤销上一步"
        ),
        VoiceCommandType.REDO to listOf(
            "重做", "恢复", "恢复上一步"
        ),
        VoiceCommandType.SAVE to listOf(
            "保存", "存一下", "保存草稿", "存稿"
        ),

        // 导航相关
        VoiceCommandType.GO_HOME to listOf(
            "返回首页", "回到主页", "返回主页", "去首页", "主页"
        ),
        VoiceCommandType.NEXT_CHAPTER to listOf(
            "下一章", "跳转下一章", "翻到下一章"
        ),
        VoiceCommandType.PREV_CHAPTER to listOf(
            "上一章", "跳转上一章", "翻到上一章"
        ),

        // 设置相关
        VoiceCommandType.SETTINGS to listOf(
            "打开设置", "设置", "进入设置"
        ),
        VoiceCommandType.HELP to listOf(
            "帮助", "怎么用", "使用说明", "使用帮助"
        )
    )

    /**
     * 解析结果
     */
    data class ParseResult(
        val command: VoiceCommandType?,
        val confidence: Float,
        val matchedKeyword: String?,
        val originalText: String
    ) {
        val isSuccess: Boolean
            get() = command != null && confidence > 0.5f
    }

    /**
     * 解析语音文本
     *
     * @param text 识别的文本
     * @return 解析结果
     */
    fun parse(text: String): ParseResult {
        if (text.isBlank()) {
            return ParseResult(null, 0f, null, text)
        }

        val normalizedText = text.trim().lowercase()

        // 遍历所有指令，查找匹配
        for ((commandType, keywords) in COMMAND_KEYWORDS) {
            for (keyword in keywords) {
                if (normalizedText.contains(keyword.lowercase())) {
                    val confidence = calculateConfidence(normalizedText, keyword)
                    Timber.d("Matched command: $commandType, keyword: $keyword, confidence: $confidence")

                    return ParseResult(
                        command = commandType,
                        confidence = confidence,
                        matchedKeyword = keyword,
                        originalText = text
                    )
                }
            }
        }

        // 未匹配到任何指令
        Timber.d("No command matched for text: $text")
        return ParseResult(null, 0f, null, text)
    }

    /**
     * 检查文本是否包含有效指令
     *
     * @param text 识别的文本
     * @return 是否包含有效指令
     */
    fun isValidCommand(text: String): Boolean {
        return parse(text).isSuccess
    }

    /**
     * 获取所有指令的提示文本
     *
     * @return 指令提示列表
     */
    fun getAllHints(): List<CommandHint> {
        return COMMAND_KEYWORDS.map { (type, keywords) ->
            CommandHint(
                command = type,
                primaryKeyword = keywords.first(),
                description = type.description,
                allKeywords = keywords
            )
        }
    }

    /**
     * 获取特定场景的指令提示
     *
     * @param scene 场景类型
     * @return 该场景的指令提示
     */
    fun getHintsForScene(scene: VoiceScene): List<CommandHint> {
        val commandTypes = when (scene) {
            VoiceScene.HOME -> listOf(
                VoiceCommandType.CREATE_NOVEL,
                VoiceCommandType.ENTER_EDITOR,
                VoiceCommandType.DELETE_NOVEL,
                VoiceCommandType.HELP
            )
            VoiceScene.EDITOR -> listOf(
                VoiceCommandType.START_WRITING,
                VoiceCommandType.SAVE,
                VoiceCommandType.UNDO,
                VoiceCommandType.REDO,
                VoiceCommandType.CREATE_CHAPTER,
                VoiceCommandType.NEXT_CHAPTER,
                VoiceCommandType.PREV_CHAPTER,
                VoiceCommandType.GO_HOME
            )
            VoiceScene.DELETE_DIALOG -> listOf(
                VoiceCommandType.CONFIRM_DELETE,
                VoiceCommandType.CANCEL_DELETE
            )
            VoiceScene.CREATE_DIALOG -> listOf(
                VoiceCommandType.CONFIRM_DELETE, // 复用"确认"
                VoiceCommandType.CANCEL_DELETE   // 复用"取消"
            )
        }

        return getAllHints().filter { it.command in commandTypes }
    }

    /**
     * 计算匹配置信度
     *
     * @param text 原始文本
     * @param keyword 匹配的关键词
     * @return 置信度（0-1）
     */
    private fun calculateConfidence(text: String, keyword: String): Float {
        // 如果完全匹配，置信度为1
        if (text == keyword.lowercase()) {
            return 1.0f
        }

        // 如果关键词在文本开头，置信度较高
        if (text.startsWith(keyword.lowercase())) {
            return 0.95f
        }

        // 如果关键词在文本末尾，置信度中等
        if (text.endsWith(keyword.lowercase())) {
            return 0.85f
        }

        // 默认置信度
        return 0.8f
    }
}

/**
 * 指令提示
 */
data class CommandHint(
    val command: VoiceCommandType,
    val primaryKeyword: String,
    val description: String,
    val allKeywords: List<String>
) {
    /**
     * 获取显示文本
     */
    fun getDisplayText(): String {
        return "\"$primaryKeyword\" - $description"
    }
}

/**
 * 语音场景
 */
enum class VoiceScene {
    HOME,           // 首页
    EDITOR,         // 编辑器
    DELETE_DIALOG,  // 删除确认弹窗
    CREATE_DIALOG   // 新建弹窗
}
