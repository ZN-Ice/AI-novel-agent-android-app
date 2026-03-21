package com.novelapp.aiagent.harness

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 安全护栏
 *
 * 职责：
 * - API密钥检测
 * - 敏感信息扫描
 * - 权限合规检查
 * - 代码安全审计
 */
@Singleton
class SafetyGuard @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 扫描代码中的敏感信息
     * @param code 代码内容
     * @return 扫描结果
     */
    fun scanForSensitiveInfo(code: String): SafetyScanResult {
        val warnings = mutableListOf<SafetyWarning>()

        // 检测硬编码API密钥
        SENSITIVE_PATTERNS.forEach { (type, pattern) ->
            val matches = pattern.findAll(code)
            matches.forEach { match ->
                warnings.add(
                    SafetyWarning(
                        type = type,
                        message = "检测到可能硬编码的${type}",
                        line = code.substring(0, match.range.first).count { it == '\n' } + 1,
                        suggestion = "请将敏感信息移至 BuildConfig 或 EncryptedSharedPreferences"
                    )
                )
            }
        }

        // 检测不安全的代码模式
        INSECURE_PATTERNS.forEach { (type, pattern) ->
            val matches = pattern.findAll(code)
            matches.forEach { match ->
                warnings.add(
                    SafetyWarning(
                        type = type,
                        message = "检测到不安全的代码模式：$type",
                        line = code.substring(0, match.range.first).count { it == '\n' } + 1,
                        suggestion = getSecuritySuggestion(type)
                    )
                )
            }
        }

        return SafetyScanResult(
            isSafe = warnings.isEmpty(),
            warnings = warnings
        )
    }

    /**
     * 检查权限合规性
     * @param permissions 权限列表
     * @return 合规结果
     */
    fun checkPermissionCompliance(permissions: List<String>): PermissionComplianceResult {
        val violations = mutableListOf<PermissionViolation>()

        permissions.forEach { permission ->
            // 检查是否在白名单中
            if (permission !in ALLOWED_PERMISSIONS) {
                violations.add(
                    PermissionViolation(
                        permission = permission,
                        reason = "权限不在白名单中",
                        suggestion = "请确认是否需要此权限，如需要请更新白名单"
                    )
                )
            }

            // 检查是否有更安全的替代方案
            if (permission in DEPRECATED_PERMISSIONS) {
                violations.add(
                    PermissionViolation(
                        permission = permission,
                        reason = "权限已被废弃或有替代方案",
                        suggestion = DEPRECATED_PERMISSION_ALTERNATIVES[permission] ?: ""
                    )
                )
            }
        }

        return PermissionComplianceResult(
            isCompliant = violations.isEmpty(),
            violations = violations
        )
    }

    /**
     * 验证API密钥是否安全存储
     * @return 是否安全
     */
    fun isApiKeySecurelyStored(): Boolean {
        // 检查是否有硬编码的API密钥
        // 实际实现需要检查代码文件
        return true
    }

    /**
     * 获取安全建议
     */
    private fun getSecuritySuggestion(type: String): String {
        return when (type) {
            "SQL_INJECTION" -> "请使用参数化查询或Room的@Query注解"
            "HARDCODED_PASSWORD" -> "请使用EncryptedSharedPreferences存储密码"
            "INSECURE_RANDOM" -> "请使用SecureRandom替代Random"
            "HTTP_URL" -> "请使用HTTPS替代HTTP"
            "DEBUGGABLE" -> "发布版本请关闭调试模式"
            else -> "请参考安全编码规范"
        }
    }

    companion object {
        // 敏感信息检测模式
        val SENSITIVE_PATTERNS = mapOf(
            "API_KEY" to Regex("(api[_-]?key|apikey)\\s*[=:]\\s*[\"'][^\"']{16,}[\"']", RegexOption.IGNORE_CASE),
            "PASSWORD" to Regex("(password|passwd|pwd)\\s*[=:]\\s*[\"'][^\"']+[\"']", RegexOption.IGNORE_CASE),
            "SECRET" to Regex("(secret|token)\\s*[=:]\\s*[\"'][^\"']{16,}[\"']", RegexOption.IGNORE_CASE),
            "PRIVATE_KEY" to Regex("-----BEGIN (RSA |EC )?PRIVATE KEY-----")
        )

        // 不安全代码模式
        val INSECURE_PATTERNS = mapOf(
            "SQL_INJECTION" to Regex("rawQuery\\s*\\(\\s*[\"'].*\\+.*[\"']"),
            "HARDCODED_PASSWORD" to Regex("password\\s*=\\s*[\"'][^\"']+[\"']"),
            "INSECURE_RANDOM" to Regex("Random\\s*\\(\\s*\\)"),
            "HTTP_URL" to Regex("http://[^\\s\"']+"),
            "DEBUGGABLE" to Regex("android:debuggable\\s*=\\s*[\"']true[\"']")
        )

        // 允许的权限白名单
        val ALLOWED_PERMISSIONS = listOf(
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE"
        )

        // 已废弃的权限
        val DEPRECATED_PERMISSIONS = listOf(
            "android.permission.READ_PHONE_STATE",
            "android.permission.GET_ACCOUNTS"
        )

        // 废弃权限的替代方案
        val DEPRECATED_PERMISSION_ALTERNATIVES = mapOf(
            "android.permission.READ_PHONE_STATE" to "使用实例ID或Firebase Installation ID",
            "android.permission.GET_ACCOUNTS" to "使用AccountPicker或OAuth2"
        )
    }
}

/**
 * 安全扫描结果
 */
data class SafetyScanResult(
    val isSafe: Boolean,
    val warnings: List<SafetyWarning>
)

/**
 * 安全警告
 */
data class SafetyWarning(
    val type: String,
    val message: String,
    val line: Int,
    val suggestion: String
)

/**
 * 权限合规结果
 */
data class PermissionComplianceResult(
    val isCompliant: Boolean,
    val violations: List<PermissionViolation>
)

/**
 * 权限违规
 */
data class PermissionViolation(
    val permission: String,
    val reason: String,
    val suggestion: String
)
