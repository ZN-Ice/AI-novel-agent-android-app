package com.novelapp.aiagent.harness

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 代码校验器
 *
 * 职责：
 * - 命名规范校验
 * - 代码格式校验
 * - 安全规范校验
 */
@Singleton
class CodeValidator @Inject constructor() {

    /**
     * 校验小说名称
     * @param name 小说名称
     * @return 校验结果
     */
    fun validateNovelName(name: String): ValidationResult {
        val errors = mutableListOf<String>()

        // 非空检查
        if (name.isBlank()) {
            errors.add("小说名称不能为空")
        }

        // 长度检查
        if (name.length > MAX_NOVEL_NAME_LENGTH) {
            errors.add("小说名称不能超过$MAX_NOVEL_NAME_LENGTH 个字符")
        }

        // 特殊字符检查
        if (INVALID_CHARS.any { name.contains(it) }) {
            errors.add("小说名称不能包含特殊字符")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }

    /**
     * 校验章节标题
     * @param title 章节标题
     * @return 校验结果
     */
    fun validateChapterTitle(title: String): ValidationResult {
        val errors = mutableListOf<String>()

        // 非空检查
        if (title.isBlank()) {
            errors.add("章节标题不能为空")
        }

        // 长度检查
        if (title.length > MAX_CHAPTER_TITLE_LENGTH) {
            errors.add("章节标题不能超过$MAX_CHAPTER_TITLE_LENGTH 个字符")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }

    /**
     * 校验章节内容
     * @param content 章节内容
     * @return 校验结果
     */
    fun validateChapterContent(content: String): ValidationResult {
        val errors = mutableListOf<String>()

        // 长度检查
        if (content.length > MAX_CHAPTER_CONTENT_LENGTH) {
            errors.add("章节内容不能超过${MAX_CHAPTER_CONTENT_LENGTH / 10000}万字")
        }

        // 敏感词检查
        val sensitiveWords = findSensitiveWords(content)
        if (sensitiveWords.isNotEmpty()) {
            errors.add("内容包含敏感词：${sensitiveWords.joinToString(", ")}")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }

    /**
     * 校验API密钥格式
     * @param apiKey API密钥
     * @return 校验结果
     */
    fun validateApiKey(apiKey: String): ValidationResult {
        val errors = mutableListOf<String>()

        // 格式检查
        if (apiKey.isBlank()) {
            errors.add("API密钥不能为空")
        } else if (!apiKey.matches(Regex(API_KEY_PATTERN))) {
            errors.add("API密钥格式不正确")
        }

        // 安全检查：是否是常见弱密钥
        if (apiKey in WEAK_API_KEYS) {
            errors.add("请使用有效的API密钥")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }

    /**
     * 查找敏感词
     */
    private fun findSensitiveWords(text: String): List<String> {
        // TODO: 实现敏感词过滤逻辑
        // 这里只是一个示例实现
        return emptyList()
    }

    companion object {
        // 常量定义
        const val MAX_NOVEL_NAME_LENGTH = 50
        const val MAX_CHAPTER_TITLE_LENGTH = 100
        const val MAX_CHAPTER_CONTENT_LENGTH = 100000 // 10万字

        // 无效字符
        val INVALID_CHARS = listOf("<", ">", ":", "\"", "|", "?", "*")

        // API密钥格式（示例）
        const val API_KEY_PATTERN = "^[a-zA-Z0-9_-]{32,}$"

        // 弱密钥列表
        val WEAK_API_KEYS = listOf(
            "test",
            "123456",
            "password",
            "api_key"
        )
    }
}

/**
 * 校验结果
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val messages: List<String>) : ValidationResult() {
        val firstMessage: String
            get() = messages.firstOrNull() ?: "校验失败"
    }

    val isSuccess: Boolean
        get() = this is Success

    val isError: Boolean
        get() = this is Error
}
