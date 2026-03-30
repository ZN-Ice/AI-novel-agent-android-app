package com.novelapp.aiagent.utils

/**
 * 小说名称验证错误类型
 */
enum class ValidationError(val message: String) {
    EMPTY("请输入小说名称"),
    TOO_LONG("小说名称不能超过50个字符"),
    DUPLICATE("已存在同名小说")
}

/**
 * 验证结果
 */
data class ValidationResult(
    val isValid: Boolean,
    val error: ValidationError?,
    val trimmedName: String
) {
    val errorMessage: String
        get() = error?.message ?: ""
}

/**
 * 小说名称验证器
 *
 * 验证优先级：空 > 过长 > 重名
 */
class NovelNameValidator {

    private var existingNames: List<String> = emptyList()

    fun setExistingNames(names: List<String>) {
        existingNames = names
    }

    fun validate(name: String): ValidationResult {
        val trimmed = name.trim()

        // 1. 空名称检查
        if (trimmed.isEmpty()) {
            return ValidationResult(
                isValid = false,
                error = ValidationError.EMPTY,
                trimmedName = trimmed
            )
        }

        // 2. 长度检查
        if (trimmed.length > MAX_NAME_LENGTH) {
            return ValidationResult(
                isValid = false,
                error = ValidationError.TOO_LONG,
                trimmedName = trimmed
            )
        }

        // 3. 重名检查
        if (existingNames.any { it.trim() == trimmed }) {
            return ValidationResult(
                isValid = false,
                error = ValidationError.DUPLICATE,
                trimmedName = trimmed
            )
        }

        return ValidationResult(
            isValid = true,
            error = null,
            trimmedName = trimmed
        )
    }

    companion object {
        private const val MAX_NAME_LENGTH = 50
    }
}
