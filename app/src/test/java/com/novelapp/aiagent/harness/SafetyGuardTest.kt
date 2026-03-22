package com.novelapp.aiagent.harness

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * SafetyGuard单元测试
 */
class SafetyGuardTest {

    private lateinit var safetyGuard: SafetyGuard
    private val mockContext: Context = mockk(relaxed = true)

    @Before
    fun setup() {
        safetyGuard = SafetyGuard(mockContext)
    }

    // ==================== 敏感信息扫描测试 ====================

    @Test
    fun `scanForSensitiveInfo with clean code returns safe`() {
        val code = """
            fun calculateSum(a: Int, b: Int): Int {
                return a + b
            }
        """.trimIndent()

        val result = safetyGuard.scanForSensitiveInfo(code)

        assertTrue(result.isSafe)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `scanForSensitiveInfo with hardcoded api key returns warning`() {
        val code = """
            val apiKey = "sk-1234567890abcdefghijklmnop"
        """.trimIndent()

        val result = safetyGuard.scanForSensitiveInfo(code)

        assertFalse(result.isSafe)
        assertTrue(result.warnings.any { it.type == "API_KEY" })
    }

    @Test
    fun `scanForSensitiveInfo with hardcoded password returns warning`() {
        val code = """
            val password = "my_secret_password"
        """.trimIndent()

        val result = safetyGuard.scanForSensitiveInfo(code)

        assertFalse(result.isSafe)
        assertTrue(result.warnings.any { it.type == "PASSWORD" })
    }

    @Test
    fun `scanForSensitiveInfo with private key returns warning`() {
        val code = """
            val key = "-----BEGIN PRIVATE KEY-----
            MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC=
            -----END PRIVATE KEY-----"
        """.trimIndent()

        val result = safetyGuard.scanForSensitiveInfo(code)

        assertFalse(result.isSafe)
        assertTrue(result.warnings.any { it.type == "PRIVATE_KEY" })
    }

    // ==================== 不安全代码模式测试 ====================

    @Test
    fun `scanForSensitiveInfo with http url returns warning`() {
        val code = """
            val url = "http://api.example.com/data"
        """.trimIndent()

        val result = safetyGuard.scanForSensitiveInfo(code)

        assertFalse(result.isSafe)
        assertTrue(result.warnings.any { it.type == "HTTP_URL" })
    }

    @Test
    fun `scanForSensitiveInfo with sql injection pattern returns warning`() {
        // SQL注入模式需要匹配 rawQuery("..." + "...") 中的字符串拼接
        val code = """
            db.rawQuery("SELECT * FROM users WHERE id = " + userId + " AND status = 'active'")
        """.trimIndent()

        val result = safetyGuard.scanForSensitiveInfo(code)

        assertFalse(result.isSafe)
        assertTrue(result.warnings.any { it.type == "SQL_INJECTION" })
    }

    @Test
    fun `scanForSensitiveInfo with insecure random returns warning`() {
        val code = """
            val random = Random()
            val value = random.nextInt()
        """.trimIndent()

        val result = safetyGuard.scanForSensitiveInfo(code)

        assertFalse(result.isSafe)
        assertTrue(result.warnings.any { it.type == "INSECURE_RANDOM" })
    }

    // ==================== 权限合规检查测试 ====================

    @Test
    fun `checkPermissionCompliance with allowed permissions returns compliant`() {
        val permissions = listOf(
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET"
        )

        val result = safetyGuard.checkPermissionCompliance(permissions)

        assertTrue(result.isCompliant)
        assertTrue(result.violations.isEmpty())
    }

    @Test
    fun `checkPermissionCompliance with disallowed permission returns violation`() {
        val permissions = listOf(
            "android.permission.READ_SMS",
            "android.permission.INTERNET"
        )

        val result = safetyGuard.checkPermissionCompliance(permissions)

        assertFalse(result.isCompliant)
        assertTrue(result.violations.any { it.permission == "android.permission.READ_SMS" })
    }

    @Test
    fun `checkPermissionCompliance with deprecated permission returns violation`() {
        val permissions = listOf(
            "android.permission.READ_PHONE_STATE"
        )

        val result = safetyGuard.checkPermissionCompliance(permissions)

        assertFalse(result.isCompliant)
        // READ_PHONE_STATE produces two violations: one for whitelist, one for deprecated
        // Find the deprecated violation which has the suggestion
        val violation = result.violations.find { it.reason.contains("废弃") }
        assertNotNull(violation)
        assertTrue(violation!!.suggestion.contains("实例ID"))
    }

    // ==================== 安全建议测试 ====================

    @Test
    fun `SafetyWarning has correct properties`() {
        val warning = SafetyWarning(
            type = "SQL_INJECTION",
            message = "检测到SQL注入风险",
            line = 10,
            suggestion = "请使用参数化查询"
        )

        assertEquals("SQL_INJECTION", warning.type)
        assertEquals("检测到SQL注入风险", warning.message)
        assertEquals(10, warning.line)
        assertEquals("请使用参数化查询", warning.suggestion)
    }

    @Test
    fun `PermissionViolation has correct properties`() {
        val violation = PermissionViolation(
            permission = "android.permission.READ_SMS",
            reason = "权限不在白名单中",
            suggestion = "请确认是否需要此权限"
        )

        assertEquals("android.permission.READ_SMS", violation.permission)
        assertEquals("权限不在白名单中", violation.reason)
        assertEquals("请确认是否需要此权限", violation.suggestion)
    }
}
